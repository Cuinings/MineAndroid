package com.cn.board.wallpaper

import android.graphics.BitmapFactory
import android.media.Image
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * OpenGL ES 壁纸渲染器。
 *
 * 实现 [WallpaperRenderer]：不用 GLSurfaceView——壁纸 Surface 由系统创建、属于 TYPE_WALLPAPER 层，
 * 必须由我们手动用 EGL14 在上面建立 GL 上下文并驱动渲染循环。
 *
 * 渲染内容（片元着色器）：
 * 1. **无媒体时**：程序化动画渐变（plasma）
 * 2. **图片时**：cover 适配 + Ken Burns 缩放 + 色相呼吸
 * 3. **视频时**：MediaCodec 字节缓冲区解码 → Y/UV 两个 GL_TEXTURE_2D 纹理 → 片元着色器 YUV→RGB。
 *    不依赖 SurfaceTexture / GL_TEXTURE_EXTERNAL_OES / GL_OES_EGL_image_external 扩展，
 *    兼容性最好（Rockchip Mali 等嵌入式 GPU 最可靠路径）。
 * 4. **桌面视差**：onOffsetsChanged 的 xOffset 驱动横向位移。
 *
 * 视频管线：解码线程每解出一帧，把 Y/UV 平面拷贝到共享 ByteBuffer 后置位 [newFrameReady]；
 * GL 渲染线程在 [drawFrame] 中检测到后上传到 GL 纹理。各线程只做自己的事，零锁竞争。
 *
 * 全部渲染跑在独立线程；EGL 资源在可见时建立、隐藏时销毁，Surface 变化时重建 window-surface，
 * 视频解码随可见性启停。所有销毁可重入、线程安全。
 */
class GLWallpaperRenderer(
    initialConfig: WallpaperConfig = WallpaperConfig.DEFAULT,
) : WallpaperRenderer {

    companion object {
        private const val TAG = "GLWallpaperRenderer"

        private val QUAD = floatArrayOf(
            -1f, -1f,  1f, -1f,  -1f, 1f,  1f, 1f,
        )

        private const val VERTEX_SHADER = """
            attribute vec2 aPos;
            varying vec2 vUv;
            void main() {
                vUv = aPos * 0.5 + 0.5;
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
        """

        // 三态：uMode=0 程序化   uMode=1 图片   uMode=2 视频(YUV→RGB)
        // 视频走 BT.601 limited-range YUV→RGB，不依赖 samplerExternalOES。
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vUv;
            uniform float uTime;
            uniform vec2  uRes;
            uniform vec2  uImg;
            uniform float uOffset;
            uniform int   uMode;
            uniform sampler2D uTex;    // 图片 / 视频 Y 平面
            uniform sampler2D uUvTex;  // 视频 UV 平面 (NV12: U,V 交错)

            vec2 coverUv(vec2 uv, vec2 res, vec2 img) {
                float rs = res.x / res.y;
                float is = img.x / img.y;
                vec2 n = uv;
                if (rs > is) {
                    n.y = (uv.y - 0.5) * (is / rs) + 0.5;
                } else {
                    n.x = (uv.x - 0.5) * (rs / is) + 0.5;
                }
                return n;
            }

            // BT.601 limited-range YUV → RGB
            vec3 yuvToRgb(float yVal, float u, float v) {
                float y = 1.164 * (yVal - 0.0625);
                u = u - 0.5;
                v = v - 0.5;
                float r = y + 1.596 * v;
                float g = y - 0.391 * u - 0.813 * v;
                float b = y + 2.018 * u;
                return clamp(vec3(r, g, b), 0.0, 1.0);
            }

            void main() {
                vec2 uv = vUv;
                uv.x += uOffset * 0.06;
                vec3 col;
                if (uMode == 2) {
                    // 视频：Y 从 uTex.r 取（GL_LUMINANCE → vec4(L,L,L,1)），UV 从 uUvTex.ra 取（GL_LUMINANCE_ALPHA）
                    vec2 t = coverUv(uv, uRes, uImg);
                    t.y = 1.0 - t.y;  // GL 纹理原点在左下角，视频帧原点在左上角
                    float yVal = texture2D(uTex, t).r;
                    vec2  uvVal = texture2D(uUvTex, t).ra;
                    col = yuvToRgb(yVal, uvVal.r, uvVal.g);
                } else if (uMode == 1) {
                    float z = 1.0 + 0.04 * sin(uTime * 0.3);
                    vec2 c = vec2(0.5);
                    uv = (uv - c) / z + c;
                    vec2 t = coverUv(uv, uRes, uImg);
                    col = texture2D(uTex, t).rgb;
                    col *= 0.92 + 0.08 * vec3(
                        sin(uTime * 0.5),
                        sin(uTime * 0.5 + 2.094),
                        sin(uTime * 0.5 + 4.188)
                    );
                } else {
                    float t = uTime * 0.4;
                    float v = sin(uv.x * 6.0 + t)
                            + sin(uv.y * 6.0 - t * 1.3)
                            + sin((uv.x + uv.y) * 5.0 + t);
                    col = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + v + t);
                }
                gl_FragColor = vec4(col, 1.0);
            }
        """
    }

    // ---- 配置 & 状态 ----
    @Volatile private var config: WallpaperConfig = initialConfig
    private val running = AtomicBoolean(false)
    private var isVisible = false

    @Volatile private var surfaceW = 0
    @Volatile private var surfaceH = 0

    @Volatile private var lastXOffset = 0.5f
    @Volatile private var currentSurface: Surface? = null

    // EGL
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null
    private var eglConfig: EGLConfig? = null
    private var boundSurface: Surface? = null

    // GL 资源
    private var program = 0
    private var aPosLoc = 0
    private var texId = 0                     // 图片纹理 (sampler2D)
    private var imgW = 1f
    private var imgH = 1f
    private var quadBuffer: java.nio.FloatBuffer? = null

    private var uTimeLoc = 0
    private var uResLoc = 0
    private var uImgLoc = 0
    private var uOffsetLoc = 0
    private var uModeLoc = 0
    private var uTexLoc = 0
    private var uUvTexLoc = 0

    // ---- 视频管线（双缓冲 — 解码写 / GL 读并行，无锁无阻塞）----
    private var yTexId = 0; private var uvTexId = 0
    private var yBufA: ByteBuffer? = null; private var uvBufA: ByteBuffer? = null
    private var yBufB: ByteBuffer? = null; private var uvBufB: ByteBuffer? = null
    private var writeIdx = 0                                    // 解码线程写此索引的 buffer
    @Volatile private var readyIdx = -1                         // GL 线程读取此索引（-1=无帧）
    private var allocatedTexW = 0; private var allocatedTexH = 0 // 已分配的 GL 纹理尺寸（≠0 表示可用 SubImage）
    private val frameConsumed = AtomicBoolean(true)            // true=解码可写下一帧
    @Volatile private var newFrameReady = false                 // 兼容旧 drawFrame 触发
    private var extractor: MediaExtractor? = null
    private var mediaCodec: MediaCodec? = null
    private var decodeThread: Thread? = null
    private val videoRunning = AtomicBoolean(false)
    private var videoW = 0; private var videoH = 0
    private var yStride = 0
    private var videoRotation = 0
    private var isNv21 = false
    private var isI420 = false
    private var loadedMediaPath: String? = null

    // 跨线程信号
    @Volatile private var needRebuild = false
    @Volatile private var pendingTextureLoad = false
    private var startTime = 0L
    private var lastDrawMode = -1

    private var glThread: Thread? = null

    // ==================== WallpaperRenderer 生命周期 ====================

    override fun attach(holder: SurfaceHolder) {
        currentSurface = holder.surface
        val f = holder.surfaceFrame
        surfaceW = f.width()
        surfaceH = f.height()
        loadConfigTextureIfNeeded()
        if (isVisible) startRender()
    }

    override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        currentSurface = holder.surface
        if (width > 0 && height > 0) {
            surfaceW = width
            surfaceH = height
        }
        if (running.get() && currentSurface != boundSurface) {
            needRebuild = true
        }
    }

    override fun onVisibilityChanged(visible: Boolean) {
        isVisible = visible
        if (visible) startRender() else stopRender()
    }

    override fun onOffsetsChanged(
        xOffset: Float, yOffset: Float, xStep: Float, yStep: Float, xPixels: Int, yPixels: Int,
    ) {
        lastXOffset = xOffset
    }

    override fun onConfigChanged(newConfig: WallpaperConfig) {
        config = newConfig
        loadConfigTextureIfNeeded()
    }

    override fun release() {
        stopRender()
        currentSurface = null
    }

    override fun isRunning(): Boolean = running.get()

    // ==================== 渲染线程控制 ====================

    private fun startRender() {
        if (running.get()) return
        if (currentSurface == null) { Log.w(TAG, "startRender: surface not ready"); return }
        running.set(true)
        pendingTextureLoad = true
        startTime = System.nanoTime()
        glThread = Thread(renderRunnable, "WallpaperGLRender").also { it.start() }
    }

    private fun stopRender() {
        running.set(false)
        glThread?.let { t -> try { t.join(2000) } catch (_: Exception) {} }
        glThread = null
    }

    private val renderRunnable = Runnable { runLoop() }

    private fun runLoop() {
        if (currentSurface == null) { Log.w(TAG, "runLoop: no surface"); running.set(false); return }
        if (!initEglContext()) { running.set(false); return }
        if (!ensureEglSurface()) {
            Log.e(TAG, "runLoop: cannot create EGL window-surface")
            destroyEgl(); running.set(false); return
        }
        if (!buildProgram()) { destroyEgl(); running.set(false); return }
        Log.i(TAG, "render loop started (surface=${surfaceW}x${surfaceH})")
        try {
            while (running.get()) {
                if (needRebuild) { rebuildEglSurface(); needRebuild = false }
                if (pendingTextureLoad) { loadTextureOnGlThread(); pendingTextureLoad = false }
                if (eglSurface == null || eglSurface === EGL14.EGL_NO_SURFACE) {
                    if (!ensureEglSurface()) { Thread.sleep(16); continue }
                }
                drawFrame()
                val d = eglDisplay; val s = eglSurface
                if (d != null && s != null && s !== EGL14.EGL_NO_SURFACE) {
                    if (!EGL14.eglSwapBuffers(d, s)) {
                        val err = EGL14.eglGetError()
                        if (err == EGL14.EGL_CONTEXT_LOST || err == EGL14.EGL_BAD_SURFACE) {
                            Log.w(TAG, "eglSwapBuffers lost, rebuild")
                            rebuildEglSurface()
                        }
                    }
                }
                pace()
            }
        } catch (e: InterruptedException) {
            Log.d(TAG, "render loop interrupted")
        } catch (e: Exception) {
            Log.e(TAG, "render loop error", e)
        } finally {
            destroyEgl()
        }
    }

    private fun pace() {
        val target = if (config.fpsCap > 0) config.fpsCap else 60
        try { Thread.sleep(1000L / target) } catch (_: InterruptedException) {}
    }

    // ==================== EGL / GL 初始化 ====================

    private fun initEglContext(): Boolean {
        return try {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay === EGL14.EGL_NO_DISPLAY) { Log.e(TAG, "eglGetDisplay failed"); return false }
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { Log.e(TAG, "eglInit failed"); return false }
            val attribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 0, EGL14.EGL_DEPTH_SIZE, 0, EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1); val num = IntArray(1)
            if (!EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs, 0, 1, num, 0) || num[0] <= 0 || configs[0] == null) {
                Log.e(TAG, "eglChooseConfig failed: 0x${EGL14.eglGetError().toString(16)}"); return false
            }
            eglConfig = configs[0]!!
            val ctxAttr = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, ctxAttr, 0)
            if (eglContext === EGL14.EGL_NO_CONTEXT) { Log.e(TAG, "eglCreateContext failed"); return false }
            Log.d(TAG, "EGL context created"); true
        } catch (e: Exception) { Log.e(TAG, "initEglContext exception", e); false }
    }

    private fun ensureEglSurface(): Boolean {
        val s = currentSurface ?: return false
        if (eglSurface !== EGL14.EGL_NO_SURFACE && eglSurface != null && boundSurface === s) return true
        return buildWindowSurface(s)
    }

    private fun rebuildEglSurface() {
        val d = eglDisplay
        try {
            val s = eglSurface
            if (d != null && s != null && s !== EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(d, s)
            }
        } catch (e: Exception) { Log.w(TAG, "rebuildEglSurface fail", e) }
        eglSurface = null; boundSurface = null
        currentSurface?.let { buildWindowSurface(it) }
    }

    private fun buildWindowSurface(surface: Surface): Boolean {
        val d = eglDisplay ?: return false; val cfg = eglConfig ?: return false; val ctx = eglContext ?: return false
        return try {
            val win = EGL14.eglCreateWindowSurface(d, cfg, surface, intArrayOf(EGL14.EGL_NONE), 0)
            if (win === EGL14.EGL_NO_SURFACE) { Log.e(TAG, "eglCreateWindowSurface failed"); return false }
            if (!EGL14.eglMakeCurrent(d, win, win, ctx)) { Log.e(TAG, "eglMakeCurrent failed"); return false }
            eglSurface = win; boundSurface = surface
            Log.d(TAG, "EGL window-surface bound"); true
        } catch (e: Exception) { Log.e(TAG, "buildWindowSurface exception", e); false }
    }

    private fun destroyEgl() {
        val d = eglDisplay
        try {
            stopVideoDecode()
            if (texId != 0) { GLES20.glDeleteTextures(1, intArrayOf(texId), 0); texId = 0 }
            if (yTexId != 0) { GLES20.glDeleteTextures(1, intArrayOf(yTexId), 0); yTexId = 0 }
            if (uvTexId != 0) { GLES20.glDeleteTextures(1, intArrayOf(uvTexId), 0); uvTexId = 0 }
            if (program != 0) { GLES20.glDeleteProgram(program); program = 0 }
            val s = eglSurface
            if (d != null && s != null && s !== EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(d, s)
            }
            val c = eglContext
            if (d != null && c != null && c !== EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(d, c)
            if (d != null && d !== EGL14.EGL_NO_DISPLAY) EGL14.eglTerminate(d)
        } catch (e: Exception) { Log.w(TAG, "destroyEgl failed", e) }
        eglSurface = null; boundSurface = null; eglContext = null; eglDisplay = null
    }

    // ==================== 着色器程序 ====================

    private fun buildProgram(): Boolean {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER) ?: return false
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER) ?: return false
        val p = GLES20.glCreateProgram().also { if (it == 0) return false }
        GLES20.glAttachShader(p, vs); GLES20.glAttachShader(p, fs); GLES20.glLinkProgram(p)
        val linked = IntArray(1); GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) {
            Log.e(TAG, "program link failed: ${GLES20.glGetProgramInfoLog(p)}")
            GLES20.glDeleteShader(vs); GLES20.glDeleteShader(fs); GLES20.glDeleteProgram(p)
            return false
        }
        GLES20.glDeleteShader(vs); GLES20.glDeleteShader(fs)
        program = p
        aPosLoc  = GLES20.glGetAttribLocation(p, "aPos")
        uTimeLoc  = GLES20.glGetUniformLocation(p, "uTime")
        uResLoc   = GLES20.glGetUniformLocation(p, "uRes")
        uImgLoc   = GLES20.glGetUniformLocation(p, "uImg")
        uOffsetLoc = GLES20.glGetUniformLocation(p, "uOffset")
        uModeLoc  = GLES20.glGetUniformLocation(p, "uMode")
        uTexLoc   = GLES20.glGetUniformLocation(p, "uTex")
        uUvTexLoc = GLES20.glGetUniformLocation(p, "uUvTex")
        quadBuffer = floatBufferOf(QUAD)
        GLES20.glUseProgram(p)
        GLES20.glEnableVertexAttribArray(aPosLoc)
        GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer)
        return true
    }

    private fun compileShader(type: Int, src: String): Int? {
        val sh = GLES20.glCreateShader(type).also { if (it == 0) return null }
        GLES20.glShaderSource(sh, src); GLES20.glCompileShader(sh)
        val ok = IntArray(1); GLES20.glGetShaderiv(sh, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) {
            Log.e(TAG, "shader compile failed: ${GLES20.glGetShaderInfoLog(sh)}")
            GLES20.glDeleteShader(sh); return null
        }
        return sh
    }

    private fun floatBufferOf(arr: FloatArray): java.nio.FloatBuffer {
        return ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().also { it.put(arr).position(0) }
    }

    // ==================== 纹理 / 视频加载 ====================

    private fun loadConfigTextureIfNeeded() {
        if (config.type == WallpaperType.GL) pendingTextureLoad = true
    }

    private fun loadTextureOnGlThread() {
        val path = config.glImagePath
        Log.i(TAG, "loadTexture: path='$path' isVideo=${isVideoPath(path)} exists=${!path.isNullOrEmpty() && File(path).exists()}")

        if (path == loadedMediaPath && (yTexId != 0 || texId != 0)) {
            Log.i(TAG, "loadTexture: path unchanged, skip"); return
        }

        if (texId != 0) { GLES20.glDeleteTextures(1, intArrayOf(texId), 0); texId = 0 }
        stopVideoDecode()

        when {
            isVideoPath(path) -> {
                Log.i(TAG, "loadTexture: → init video ($path)")
                initVideoOnGlThread(path!!)
            }
            !path.isNullOrEmpty() && File(path).exists() -> {
                Log.i(TAG, "loadTexture: → load image ($path)")
                loadImageTexture(path)
            }
            else -> {
                imgW = 1f; imgH = 1f; loadedMediaPath = null
                Log.i(TAG, "loadTexture: → procedural (no GL media)")
            }
        }
    }

    private fun loadImageTexture(path: String) {
        val bmp = BitmapFactory.decodeFile(path)
        if (bmp == null) { Log.w(TAG, "decode GL image failed: $path"); return }
        val t = IntArray(1); GLES20.glGenTextures(1, t, 0)
        texId = t[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        imgW = bmp.width.toFloat(); imgH = bmp.height.toFloat()
        loadedMediaPath = path
        bmp.recycle()
        Log.d(TAG, "GL image texture loaded: $path (${bmp.width}x${bmp.height})")
    }

    /**
     * 渲染线程调用：初始化视频解码——创建两个 GL_TEXTURE_2D 纹理（Y + UV 平面），
     * 分配共享 ByteBuffer，启动 MediaCodec（**无** output Surface，纯字节缓冲区输出），
     * 解码线程产出帧后拷贝到共享缓冲区 → 置位 [newFrameReady] → GL 线程上传纹理。
     */
    private fun initVideoOnGlThread(path: String) {
        try {
            Log.i(TAG, "initVideo: creating Y+UV 2D textures for $path")
            val t = IntArray(2); GLES20.glGenTextures(2, t, 0)
            yTexId = t[0]; uvTexId = t[1]
            for (tid in intArrayOf(yTexId, uvTexId)) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tid)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            }

            Log.i(TAG, "initVideo: extractor setDataSource($path)...")
            val ex = MediaExtractor()
            ex.setDataSource(path)
            var trackIndex = -1
            var trackMime: String? = null
            for (i in 0 until ex.trackCount) {
                val fmt = ex.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    trackIndex = i; trackMime = mime
                    Log.i(TAG, "initVideo: found video track $i mime=$mime")
                    break
                }
            }
            if (trackIndex < 0) { Log.w(TAG, "initVideo: no video track"); stopVideoDecode(); return }
            ex.selectTrack(trackIndex)
            val fmt = ex.getTrackFormat(trackIndex)
            val mime = trackMime!!
            Log.i(TAG, "initVideo: creating byte-buffer decoder for $mime")
            val codec = MediaCodec.createDecoderByType(mime)
            // 关键：不传 output Surface → 字节缓冲区输出
            codec.configure(fmt, null, null, 0)
            codec.start()
            Log.i(TAG, "initVideo: byte-buffer codec started")

            // 分配缓冲区（尺寸在 INFO_OUTPUT_FORMAT_CHANGED 时更新）
            videoW = try { fmt.getInteger(MediaFormat.KEY_WIDTH) } catch (_: Exception) { 0 }
            videoH = try { fmt.getInteger(MediaFormat.KEY_HEIGHT) } catch (_: Exception) { 0 }
            allocYuvBuffers(videoW, videoH)
            extractor = ex
            mediaCodec = codec
            videoRunning.set(true)
            newFrameReady = false
            writeIdx = 0; readyIdx = -1
            allocatedTexW = 0; allocatedTexH = 0
            frameConsumed.set(true)
            loadedMediaPath = path
            isNv21 = false; isI420 = false
            decodeThread = Thread(decodeRunnable, "WallpaperGLVideoDecode").also { it.start() }
            Log.i(TAG, "initVideo: decode thread started, res=${videoW}x${videoH} yTex=$yTexId uvTex=$uvTexId")
        } catch (e: Exception) {
            Log.e(TAG, "initVideo: FAILED for $path — ${e.javaClass.simpleName}: ${e.message}", e)
            stopVideoDecode()
        }
    }

    private fun allocYuvBuffers(w: Int, h: Int) {
        val ySize = w * h; val uvSize = w * h / 2
        if (ySize <= 0 || uvSize <= 0) {
            Log.w(TAG, "allocYuvBuffers skipped: w=$w h=$h (will retry after format changed)")
            return
        }
        yBufA = ByteBuffer.allocateDirect(ySize)
        uvBufA = ByteBuffer.allocateDirect(uvSize)
        yBufB = ByteBuffer.allocateDirect(ySize)
        uvBufB = ByteBuffer.allocateDirect(uvSize)
        Log.d(TAG, "allocYuvBuffers: Y=${ySize} UV=${uvSize} x2")
    }

    // ==================== 解码线程 ====================

    private val decodeRunnable = Runnable { decodeLoop() }

    private fun decodeLoop() {
        val codec = mediaCodec ?: return
        val ex = extractor ?: return
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var frameCount = 0
        var stallCount = 0
        try {
            while (videoRunning.get()) {
                // 喂帧
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(5000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)
                        if (buf != null) {
                            val size = ex.readSampleData(buf, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, size, ex.sampleTime, 0)
                                ex.advance()
                            }
                        }
                    }
                }
                // 取帧
                val outIdx = codec.dequeueOutputBuffer(info, 5000)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val nf = codec.outputFormat
                        val nw = nf.getInteger(MediaFormat.KEY_WIDTH)
                        val nh = nf.getInteger(MediaFormat.KEY_HEIGHT)
                        val ns = try { nf.getInteger(MediaFormat.KEY_STRIDE) } catch (_: Exception) { nw }
                        yStride = if (ns > 0) ns else nw
                        videoRotation = try { nf.getInteger(MediaFormat.KEY_ROTATION) } catch (_: Exception) { 0 }
                        val cf = try { nf.getInteger(MediaFormat.KEY_COLOR_FORMAT) } catch (_: Exception) { -1 }
                        Log.i(TAG, "decode: format changed → ${nw}x${nh} stride=$yStride rotation=$videoRotation colorFmt=$cf")
                        // 检测 NV12 vs NV21 vs I420
                        isNv21 = cf == 2141391876 || cf.toString().contains("NV21", ignoreCase = true)
                        isI420 = cf == 19 || cf == 39 || cf.toString().contains("I420", ignoreCase = true)
                                || cf.toString().contains("YUV420P", ignoreCase = true)
                        Log.i(TAG, "decode: colorFmt=$cf → nv21=$isNv21 i420=$isI420")
                        if (nw != videoW || nh != videoH) {
                            videoW = nw; videoH = nh
                            allocYuvBuffers(videoW, videoH)
                        }
                        stallCount = 0
                    }
                    outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        stallCount++
                        if (stallCount % 120 == 1) {
                            Log.w(TAG, "decode: stalled $stallCount×5s — codec not producing output")
                        }
                    }
                    outIdx >= 0 -> {
                        val isEos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        if (!isEos) {
                            if (isI420) {
                                val img = codec.getOutputImage(outIdx)
                                if (img != null) {
                                    copyYuvImage(img)
                                    img.close()
                                }
                            } else {
                                copyYuvFrame(codec, outIdx)
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        frameCount++; stallCount = 0
                        if (frameCount == 1) Log.i(TAG, "decode: FIRST frame output!")
                        else if (frameCount % 30 == 0) Log.d(TAG, "decode: $frameCount frames")
                        if (isEos) {
                            Log.i(TAG, "decode: EOS → loop (frames=$frameCount)")
                            frameCount = 0
                            inputDone = false
                            try { ex.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC) } catch (_: Exception) {}
                            try { codec.flush() } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "decodeLoop ended", e)
        }
        Log.i(TAG, "decode: thread exit")
    }

    /** NV12/NV21 用：ByteBuffer 输出 → 双缓冲写入 + 原子交换。 */
    private fun copyYuvFrame(codec: MediaCodec, outIdx: Int) {
        val outBuf = codec.getOutputBuffer(outIdx) ?: return
        val w = videoW; val h = videoH
        val stride = if (yStride > 0) yStride else w
        if (w <= 0 || h <= 0) return
        if (!frameConsumed.get()) return

        val yBuf = if (writeIdx == 0) yBufA else yBufB
        val uvBuf = if (writeIdx == 0) uvBufA else uvBufB
        if (yBuf == null || uvBuf == null) {
            Log.w(TAG, "copyYuvFrame: buffers not allocated yet, drop frame"); return
        }
        if (yBuf.capacity() < w * h || uvBuf.capacity() < w * h / 2) {
            Log.w(TAG, "copyYuvFrame: buffer capacity mismatch"); allocYuvBuffers(w, h); return
        }

        yBuf.clear(); uvBuf.clear()
        // Y 平面：stride==width 时一次 bulk copy
        if (stride == w) {
            outBuf.limit(w * h); outBuf.position(0); yBuf.put(outBuf)
        } else {
            for (row in 0 until h) {
                val rs = row * stride; outBuf.limit(rs + w); outBuf.position(rs); yBuf.put(outBuf)
            }
        }

        val uvStart = stride * h; val uvRowBytes = w
        if (isNv21) {
            for (row in 0 until h / 2) {
                val rs = uvStart + row * stride; outBuf.limit(rs + uvRowBytes); outBuf.position(rs)
                for (i in 0 until uvRowBytes / 2) {
                    val v = outBuf.get(); val u = outBuf.get(); uvBuf.put(u); uvBuf.put(v)
                }
            }
        } else {
            // NV12 且 stride==w 时，UV 也是连续内存，一次 bulk copy
            if (stride == w && uvStart + w * h / 2 <= outBuf.capacity()) {
                outBuf.limit(uvStart + w * h / 2); outBuf.position(uvStart); uvBuf.put(outBuf)
            } else {
                for (row in 0 until h / 2) {
                    val rs = uvStart + row * stride; outBuf.limit(rs + uvRowBytes); outBuf.position(rs)
                    uvBuf.put(outBuf)
                }
            }
        }
        yBuf.flip(); uvBuf.flip()

        readyIdx = writeIdx
        writeIdx = 1 - writeIdx
        frameConsumed.set(false)
        newFrameReady = true
    }

    /**
     * I420 专用：Image API → 写入双缓冲的 writeIdx 槽 → 原子交换 readyIdx。
     * 若 GL 线程未消费上一帧（frameConsumed==false），跳过当前帧（解码快于渲染时不堆积）。
     */
    private fun copyYuvImage(image: Image) {
        val w = videoW; val h = videoH
        if (w <= 0 || h <= 0) return
        if (!frameConsumed.get()) return  // GL 还没消费上一帧，跳过

        val planes = image.planes
        if (planes.size < 3) { Log.w(TAG, "copyYuvImage: expected 3 planes, got ${planes.size}"); return }

        val yPlane = planes[0]; val uPlane = planes[1]; val vPlane = planes[2]
        val yRowStride = yPlane.rowStride; val uRowStride = uPlane.rowStride; val vRowStride = vPlane.rowStride
        val yb = yPlane.buffer; val ub = uPlane.buffer; val vb = vPlane.buffer

        val yBuf = if (writeIdx == 0) yBufA else yBufB
        val uvBuf = if (writeIdx == 0) uvBufA else uvBufB
        if (yBuf == null || uvBuf == null) {
            Log.w(TAG, "copyYuvImage: buffers not allocated yet, drop frame"); return
        }

        yBuf.clear(); uvBuf.clear()

        // Y 平面：stride==width 时一次 bulk copy（用户日志 stride=3840=width）
        if (yRowStride == w) {
            yb.position(0); yb.limit(w * h); yBuf.put(yb)
        } else {
            for (row in 0 until h) {
                yb.position(row * yRowStride); yb.limit(yb.position() + w); yBuf.put(yb)
            }
        }
        yBuf.flip()

        // UV 交错：逐行 bulk read U/V → 数组交错 → 一次 bulk write。
        // 避免逐像素 ByteBuffer.get/put 带来的百万次级 bounds check。
        val hw = w / 2; val hh = h / 2
        if (uRowStride == hw && vRowStride == hw) {
            val uRow = ByteArray(hw); val vRow = ByteArray(hw); val uvRow = ByteArray(hw * 2)
            for (row in 0 until hh) {
                val off = row * hw
                ub.position(off); ub.get(uRow, 0, hw)
                vb.position(off); vb.get(vRow, 0, hw)
                for (col in 0 until hw) {
                    uvRow[col * 2] = uRow[col]; uvRow[col * 2 + 1] = vRow[col]
                }
                uvBuf.put(uvRow)
            }
        } else {
            for (row in 0 until hh) {
                for (col in 0 until hw) {
                    uvBuf.put(ub.get(row * uRowStride + col))
                    uvBuf.put(vb.get(row * vRowStride + col))
                }
            }
        }
        uvBuf.flip()

        readyIdx = writeIdx; writeIdx = 1 - writeIdx
        frameConsumed.set(false); newFrameReady = true
    }

    // ==================== 停止解码 ====================

    private fun stopVideoDecode() {
        if (yTexId == 0 && mediaCodec == null && extractor == null) return
        Log.i(TAG, "stopVideoDecode: releasing (yTex=$yTexId uvTex=$uvTexId)")
        videoRunning.set(false)
        decodeThread?.let { t -> try { t.join(2000) } catch (_: Exception) {} }
        decodeThread = null
        try { mediaCodec?.stop(); mediaCodec?.release() } catch (_: Exception) {}
        mediaCodec = null
        try { extractor?.release() } catch (_: Exception) {}
        extractor = null
        if (yTexId != 0) { GLES20.glDeleteTextures(1, intArrayOf(yTexId), 0); yTexId = 0 }
        if (uvTexId != 0) { GLES20.glDeleteTextures(1, intArrayOf(uvTexId), 0); uvTexId = 0 }
        yBufA = null; uvBufA = null; yBufB = null; uvBufB = null
        writeIdx = 0; readyIdx = -1
        allocatedTexW = 0; allocatedTexH = 0
        frameConsumed.set(true)
        newFrameReady = false
        videoW = 0; videoH = 0
        yStride = 0; videoRotation = 0
        isNv21 = false; isI420 = false
        loadedMediaPath = null
    }

    private fun isVideoPath(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        val lower = path.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm")
            || lower.endsWith(".avi") || lower.endsWith(".3gp") || lower.endsWith(".m4v")
            || lower.endsWith(".ts") || lower.endsWith(".mov") || lower.endsWith(".flv")
    }

    // ==================== 绘帧 ====================

    private fun drawFrame() {
        if (program == 0 || surfaceW <= 0 || surfaceH <= 0) return
        val now = (System.nanoTime() - startTime) / 1_000_000_000.0f
        GLES20.glViewport(0, 0, surfaceW, surfaceH)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val mode = when {
            yTexId != 0 && mediaCodec != null -> 2   // 视频
            texId != 0 -> 1                            // 图片
            else -> 0
        }
        if (mode != lastDrawMode) {
            Log.i(TAG, "drawFrame: mode $lastDrawMode→$mode (yTex=$yTexId uvTex=$uvTexId codec=${mediaCodec!=null} imgTex=$texId)")
            if (mode == 0 && isVideoPath(config.glImagePath)) {
                Log.w(TAG, "drawFrame: video path configured but mode=0 — init may have failed, check logs above")
            }
            lastDrawMode = mode
        }

        GLES20.glUseProgram(program)
        GLES20.glUniform1f(uTimeLoc, now)
        GLES20.glUniform2f(uResLoc, surfaceW.toFloat(), surfaceH.toFloat())
        val (uw, uh) = if (mode == 2) {
            val vw = if (videoW > 0) videoW else surfaceW
            val vh = if (videoH > 0) videoH else surfaceH
            vw.toFloat() to vh.toFloat()
        } else {
            imgW to imgH
        }
        GLES20.glUniform2f(uImgLoc, uw, uh)
        GLES20.glUniform1f(uOffsetLoc, (lastXOffset - 0.5f) * 2f)
        GLES20.glUniform1i(uModeLoc, mode)

        if (mode == 2) {
            // 上传最新的 Y+UV 数据到 GL 纹理
            if (newFrameReady && yTexId != 0 && uvTexId != 0) {
                uploadYuvTextures()
                newFrameReady = false
            }
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTexId)
            GLES20.glUniform1i(uTexLoc, 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uvTexId)
            GLES20.glUniform1i(uUvTexLoc, 1)
        } else if (mode == 1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
            GLES20.glUniform1i(uTexLoc, 0)
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    /** GL 线程调用：尺寸不变时用 SubImage（更快），尺寸变了回退 TexImage。 */
    private fun uploadYuvTextures() {
        val w = videoW; val h = videoH
        if (w <= 0 || h <= 0) return
        val ri = readyIdx
        if (ri < 0) return

        val yb = if (ri == 0) yBufA!! else yBufB!!
        val ub = if (ri == 0) uvBufA!! else uvBufB!!
        val sameSize = (w == allocatedTexW && h == allocatedTexH)

        if (sameSize) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTexId)
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, w, h,
                GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yb)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uvTexId)
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, w / 2, h / 2,
                GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, ub)
        } else {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTexId)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, w, h, 0,
                GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yb)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uvTexId)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, w / 2, h / 2, 0,
                GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, ub)
            allocatedTexW = w; allocatedTexH = h
        }

        newFrameReady = false
        frameConsumed.set(true)
    }
}
