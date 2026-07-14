package com.cn.board.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.opengl.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 服务级模糊壁纸 — 在 WallpaperService 中 blur 视频帧后渲染到壁纸 Surface。
 *
 * ## 模糊引擎
 * | API 等级 | 引擎 | 特性 |
 * |---------|------|------|
 * | 31+ | RenderNode + RenderEffect | GPU 实时模糊，零额外分配 |
 * | < 31  | RenderScript (RS) | CPU 硬件加速模糊 |                                                     |
 *
 * ## 使用方式
 * 1. 用户在系统设置中设为动态壁纸
 * 2. 或通过 `Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)` 引导设置
 * 3. App 端通过 `FLAG_SHOW_WALLPAPER` 显示模糊壁纸作为背景
 * 4. App 端的 FrostedAnimatedGlowView 只绘制 glow 效果，零 capture 开销
 */
class BoardWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "BoardWallpaperService"
        private const val BLUR_RADIUS = 10f

        var wallpaperPath: String? = "/storage/emulated/0/Download/wallpaper_dy_1.mp4"
            set(value) { value.takeIf { it != field }?.let {
                field = it
                activeEngines.forEach { it.loadWallpaper() }
            } }

        private val activeEngines = mutableListOf<BoardWallpaperEngine>()
    }

    override fun onCreateEngine(): Engine = BoardWallpaperEngine()

    private inner class BoardWallpaperEngine : Engine(), SurfaceTexture.OnFrameAvailableListener {

        init { activeEngines.add(this) }

        private var isVisible = false
        private var currentDrawable: Drawable? = null
        private var mediaPlayer: MediaPlayer? = null
        private var surfaceTexture: SurfaceTexture? = null
        private var glRenderer: GLRenderer? = null

        // ── 模糊引擎 ──
        private var blurWidth = 0
        private var blurHeight = 0

        // RS (API < 31)
        private var rs: RenderScript? = null
        private var rsBlur: ScriptIntrinsicBlur? = null
        private var rsAllocIn: Allocation? = null
        private var rsAllocOut: Allocation? = null
        private val rsLock = ReentrantLock()

        // HW RenderNode (API 31+)
        private var hwBlurNode: RenderNode? = null

        private val handler = Handler(Looper.getMainLooper())

        // ==================== Surface 生命周期 ====================

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            loadWallpaper()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            blurWidth = width
            blurHeight = height
            initOpenGL(width, height)
            initBlurBackend()
            mediaPlayer?.let { mp ->
                surfaceTexture?.let { st ->
                    if (mp.isPlaying) mp.pause()
                    // 重新绑定 surface
                    mp.setSurface(android.view.Surface(st))
                    if (isVisible) mp.start()
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) mediaPlayer?.start() else mediaPlayer?.pause()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isVisible = false
            releaseMediaPlayer()
            releaseBlurBackend()
            activeEngines.remove(this)
        }

        // ==================== 加载壁纸 ====================

        fun loadWallpaper() {
            val path = wallpaperPath
            if (path != null && File(path).exists()) {
                try {
                    releaseMediaPlayer()
                    currentDrawable = null
                    if (isVideoFile(path)) loadVideoWallpaper(path)
                    else currentDrawable = Drawable.createFromPath(path)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load wallpaper: $e")
                    currentDrawable = null
                    releaseMediaPlayer()
                }
            } else {
                Log.w(TAG, "Wallpaper path not set or file not found")
                currentDrawable = null
                releaseMediaPlayer()
            }
        }

        private fun isVideoFile(path: String): Boolean =
            listOf(".mp4", ".3gp", ".mov", ".avi", ".wmv", ".mkv").any { path.lowercase().endsWith(it) }

        private fun loadVideoWallpaper(path: String) {
            try {
                surfaceTexture = SurfaceTexture(0).also { it.setOnFrameAvailableListener(this) }
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(path)
                    surfaceTexture?.let { setSurface(android.view.Surface(it)) }
                    isLooping = true
                    prepare()
                    if (isVisible) start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load video: $e")
                releaseMediaPlayer()
            }
        }

        // ==================== 模糊后端初始化 ====================

        private fun initBlurBackend() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return // HW 路径无需初始化
            try {
                rsLock.withLock {
                    if (rs == null) {
                        rs = RenderScript.create(this@BoardWallpaperService)
                        rsBlur = ScriptIntrinsicBlur.create(rs!!, Element.U8_4(rs!!))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "RS init failed", e)
            }
        }

        private fun initOpenGL(width: Int, height: Int) {
            glRenderer?.release()
            glRenderer = GLRenderer(width, height)
        }

        // ==================== 帧处理（核心） ====================

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
            if (!isVisible) return
            handler.post { processFrame() }
        }

        private fun processFrame() {
            val holder = surfaceHolder ?: return
            val canvas = holder.lockCanvas() ?: return
            try {
                // 1. 获取视频帧 Bitmap
                val videoBitmap = glRenderer?.renderFrame(this.surfaceTexture!!)
                if (videoBitmap == null || videoBitmap.isRecycled) {
                    canvas.drawColor(0xFF000000.toInt())
                    return
                }

                // 2. 模糊并绘制到壁纸 Canvas
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blurAndDrawHw(canvas, videoBitmap, BLUR_RADIUS)
                } else {
                    blurAndDrawRs(canvas, videoBitmap, BLUR_RADIUS)
                }

                videoBitmap.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Frame error", e)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        // ──────────── API 31+ HW 路径 ────────────

        @RequiresApi(Build.VERSION_CODES.S)
        private fun blurAndDrawHw(canvas: Canvas, bitmap: Bitmap, radius: Float) {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return

            // 复用 RenderNode
            if (hwBlurNode == null) {
                hwBlurNode = RenderNode("BlurNode")
            }
            hwBlurNode!!.apply {
                setPosition(0, 0, w, h)
                val rCanvas: android.graphics.RecordingCanvas = beginRecording(w, h)
                rCanvas.drawBitmap(bitmap, 0f, 0f, null)
                endRecording()
                setRenderEffect(
                    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                )
            }

            // 部分 Canvas（lockHardwareCanvas）支持 drawRenderNode
            // 如不支持则走 RS 回退
            try {
                canvas.drawRenderNode(hwBlurNode!!)
            } catch (e: Exception) {
                // Canvas 不支持 HW drawRenderNode → 回退到 CPU RS blur
                hwBlurNode?.discardDisplayList()
                hwBlurNode = null
                blurAndDrawRs(canvas, bitmap, radius)
            }
        }

        // ──────────── API < 31 RS 路径 ────────────

        private fun blurAndDrawRs(canvas: Canvas, bitmap: Bitmap, radius: Float) {
            try {
                rsLock.withLock {
                    val rs = this.rs ?: return
                    val blur = rsBlur ?: return

                    val allocIn: Allocation
                    val allocOut: Allocation

                    if (rsAllocIn != null && rsAllocIn!!.type.x == bitmap.width
                        && rsAllocIn!!.type.y == bitmap.height) {
                        allocIn = rsAllocIn!!
                        allocOut = rsAllocOut!!
                        allocIn.copyFrom(bitmap)
                    } else {
                        rsAllocIn?.destroy()
                        rsAllocOut?.destroy()
                        allocIn = Allocation.createFromBitmap(rs, bitmap,
                            Allocation.MipmapControl.MIPMAP_NONE,
                            Allocation.USAGE_SCRIPT or Allocation.USAGE_SHARED)
                        allocOut = Allocation.createTyped(rs, allocIn.type)
                        rsAllocIn = allocIn
                        rsAllocOut = allocOut
                    }

                    blur.setRadius(radius.coerceIn(0.1f, 25f))
                    blur.setInput(allocIn)
                    blur.forEach(allocOut)

                    val output = createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                    allocOut.copyTo(output)
                    canvas.drawBitmap(output, 0f, 0f, null)
                    output.recycle()
                }
            } catch (e: Exception) {
                Log.e(TAG, "RS blur failed", e)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
            }
        }

        // ==================== 资源释放 ====================

        private fun releaseBlurBackend() {
            rsLock.withLock {
                rsAllocIn?.destroy(); rsAllocIn = null
                rsAllocOut?.destroy(); rsAllocOut = null
                rsBlur?.destroy(); rsBlur = null
                rs?.destroy(); rs = null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                hwBlurNode?.discardDisplayList()
                hwBlurNode = null
            }
        }

        private fun releaseMediaPlayer() {
            try { mediaPlayer?.stop() } catch (_: Exception) {}
            mediaPlayer?.release()
            mediaPlayer = null
            surfaceTexture?.release()
            surfaceTexture = null
            glRenderer?.release()
            glRenderer = null
        }

        // ==================== OpenGL 渲染器 ====================

        private inner class GLRenderer(private val width: Int, private val height: Int) {
            private var eglDisplay: EGLDisplay? = null
            private var eglContext: EGLContext? = null
            private var eglSurface: EGLSurface? = null
            private var program = 0
            private var vertexShader = 0
            private var fragmentShader = 0
            private var positionHandle = 0
            private var textureHandle = 0
            private var textureId = 0
            private var framebufferId = 0

            init { initOpenGL() }

            private fun initOpenGL() {
                try {
                    eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                    val version = IntArray(2)
                    if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return

                    val configAttrs = intArrayOf(
                        EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
                        EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE
                    )
                    val configs = arrayOfNulls<EGLConfig>(1)
                    val numConfigs = IntArray(1)
                    if (!EGL14.eglChooseConfig(eglDisplay, configAttrs, 0, configs, 0, 1, numConfigs, 0)
                        || numConfigs[0] == 0) return

                    val ctxAttrs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                    eglContext = EGL14.eglCreateContext(eglDisplay, configs[0],
                        EGL14.EGL_NO_CONTEXT, ctxAttrs, 0) ?: return

                    val surfAttrs = intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE)
                    eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfAttrs, 0) ?: return

                    EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

                    vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
                    fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

                    program = GLES20.glCreateProgram().also {
                        GLES20.glAttachShader(it, vertexShader)
                        GLES20.glAttachShader(it, fragmentShader)
                        GLES20.glLinkProgram(it)
                    }
                    positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
                    textureHandle = GLES20.glGetUniformLocation(program, "uTexture")

                    val texIds = IntArray(1)
                    GLES20.glGenTextures(1, texIds, 0)
                    textureId = texIds[0]

                    val fbIds = IntArray(1)
                    GLES20.glGenFramebuffers(1, fbIds, 0)
                    framebufferId = fbIds[0]
                } catch (e: Exception) {
                    Log.e(TAG, "GL init error", e)
                }
            }

            fun renderFrame(st: SurfaceTexture): Bitmap? {
                try {
                    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
                    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
                    st.updateTexImage()

                    GLES20.glUseProgram(program)

                    // 全屏四边形
                    val verts = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
                    val buf = java.nio.ByteBuffer.allocateDirect(32).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
                    buf.put(verts).position(0)
                    GLES20.glEnableVertexAttribArray(positionHandle)
                    GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, buf)
                    GLES20.glUniform1i(textureHandle, 0)

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId)
                    GLES20.glClearColor(0f, 0f, 0f, 1f)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

                    // 读像素
                    val pxBuf = java.nio.ByteBuffer.allocateDirect(width * height * 4)
                        .order(java.nio.ByteOrder.nativeOrder())
                    GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pxBuf)

                    val pixels = IntArray(width * height)
                    pxBuf.rewind()
                    for (i in 0 until width * height) {
                        val b = pxBuf.get().toInt() and 0xFF
                        val g = pxBuf.get().toInt() and 0xFF
                        val r = pxBuf.get().toInt() and 0xFF
                        val a = pxBuf.get().toInt() and 0xFF
                        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }

                    val bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bm.setPixels(pixels, 0, width, 0, 0, width, height)
                    val matrix = android.graphics.Matrix().apply { postScale(1f, -1f) }
                    return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true).also { bm.recycle() }
                } catch (e: Exception) {
                    Log.e(TAG, "Render error", e)
                    return null
                }
            }

            fun release() {
                try {
                    GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                    GLES20.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
                    GLES20.glDeleteProgram(program)
                    GLES20.glDeleteShader(vertexShader)
                    GLES20.glDeleteShader(fragmentShader)
                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    EGL14.eglTerminate(eglDisplay)
                } catch (_: Exception) {}
            }

            private fun loadShader(type: Int, code: String): Int =
                GLES20.glCreateShader(type).also { GLES20.glShaderSource(it, code); GLES20.glCompileShader(it) }

            private val vertexShaderCode = """
                attribute vec4 aPosition;
                varying vec2 vTextureCoord;
                void main() {
                    gl_Position = aPosition;
                    vTextureCoord = vec2((aPosition.x + 1.0) / 2.0, (aPosition.y + 1.0) / 2.0);
                }
            """

            private val fragmentShaderCode = """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES uTexture;
                varying vec2 vTextureCoord;
                void main() {
                    gl_FragColor = texture2D(uTexture, vTextureCoord);
                }
            """
        }
    }
}
