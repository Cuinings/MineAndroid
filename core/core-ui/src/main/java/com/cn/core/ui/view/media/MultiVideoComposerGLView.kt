//package com.cn.core.ui.view.media
//
//import android.content.Context
//import android.graphics.RectF
//import android.graphics.SurfaceTexture
//import android.media.MediaCodec
//import android.media.MediaExtractor
//import android.media.MediaFormat
//import android.net.Uri
//import android.opengl.GLES11Ext
//import android.opengl.GLES20
//import android.opengl.GLSurfaceView
//import android.opengl.Matrix
//import android.util.AttributeSet
//import android.util.Log
//import android.view.Surface
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.nio.FloatBuffer
//import java.util.concurrent.atomic.AtomicBoolean
//import javax.microedition.khronos.egl.EGLConfig
//import javax.microedition.khronos.opengles.GL10
//import kotlin.math.max
//import kotlin.math.min
//
///**
// * 终极性能多视频合成视图
// *
// * **GLSurfaceView + MediaCodec + SurfaceTexture 方案**
// *
// * ## 架构
// * ```
// * MultiVideoComposerGLView (GLSurfaceView)
// * ├── Renderer (onSurfaceCreated / onDrawFrame / onSurfaceChanged)
// * │   ├── N × SurfaceTexture (GL_TEXTURE_EXTERNAL_OES)   ← 零拷贝 GPU 纹理
// * │   └── Shader: 逐 quad 绘制 + 混合合成
// * │
// * ├── N × DecodeThread (独立运行)
// * │   ├── MediaExtractor → 读取轨道数据
// * │   └── MediaCodec → Surface (来自 SurfaceTexture)     ← 零拷贝，不经过 CPU
// * │
// * └── Public API
// *     ├── setVideos(vararg VideoConfig)   — 设置视频源
// *     └── setLayout(VideoLayout)          — 设置布局
// * ```
// *
// * ## 与项目现有方案对比
// *
// * | 方案 | 解码 | 渲染 | 拷贝次数 | 帧控制 | 布局灵活 |
// * |------|------|------|:--------:|:------:|:--------:|
// * | SurfaceLoopVideoView | MediaCodec | SurfaceView+Canvas | **1次 CPU** | 帧级 | ⭐⭐⭐ |
// * | CodecLoopVideoView | MediaCodec | TextureView | **0次** | 帧级 | ⭐ |
// * | GpuVideoWallpaperView | MediaPlayer | GLSurfaceView | **0次** | 播放器级 | ⭐ |
// * | **→ MultiVideoComposerGLView** | **MediaCodec × N** | **GLSurfaceView** | **0次** | **帧级** | **⭐⭐⭐⭐⭐** |
// *
// * ## 使用示例
// *
// * ```kotlin
// * val composer = MultiVideoComposerGLView(context)
// *
// * // 四宫格播放
// * composer.setVideos(
// *     VideoConfig("/sdcard/video1.mp4", normalizedRect = RectF(0f, 0f, 0.5f, 0.5f)),
// *     VideoConfig("/sdcard/video2.mp4", normalizedRect = RectF(0.5f, 0f, 1f, 0.5f)),
// *     VideoConfig("/sdcard/video3.mp4", normalizedRect = RectF(0f, 0.5f, 0.5f, 1f)),
// *     VideoConfig("/sdcard/video4.mp4", normalizedRect = RectF(0.5f, 0.5f, 1f, 1f)),
// * )
// *
// * // 画中画
// * composer.setVideos(
// *     VideoConfig("/sdcard/main.mp4", normalizedRect = RectF(0f, 0f, 1f, 1f), layer = 0),
// *     VideoConfig("/sdcard/pip.mp4",  normalizedRect = RectF(0.6f, 0.6f, 0.95f, 0.95f), layer = 1),
// * )
// * ```
// *
// * @constructor 创建多视频合成 GLSurfaceView
// * @param context 上下文
// * @param attrs XML 属性集
// * @param defStyleAttr 默认样式属性
// */
//class MultiVideoComposerGLView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0,
//) : GLSurfaceView(context, attrs, defStyleAttr), GLSurfaceView.Renderer {
//
//    companion object {
//        private const val TAG = "MultiVideoGL"
//
//        /** 最大支持视频路数 */
//        const val MAX_VIDEOS = 6
//
//        /** MediaCodec 超时（微秒） */
//        private const val TIMEOUT_US = 10_000L
//
//        // ── 最大视频分辨率（用于 SurfaceTexture 的默认缓冲区） ──
//        private const val MAX_VIDEO_WIDTH = 1920
//        private const val MAX_VIDEO_HEIGHT = 1080
//
//        // ── Shader ──
//        private const val VERTEX_SHADER = """
//            attribute vec4 aPosition;
//            attribute vec2 aTexCoord;
//            uniform mat4 uTexMatrix;
//            varying vec2 vTexCoord;
//            void main() {
//                gl_Position = aPosition;
//                vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
//            }
//        """
//
//        private const val FRAGMENT_SHADER = """
//            #extension GL_OES_EGL_image_external : require
//            precision mediump float;
//            uniform samplerExternalOES uTexture;
//            varying vec2 vTexCoord;
//            void main() {
//                gl_FragColor = texture2D(uTexture, vTexCoord);
//            }
//        """
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  配置类
//    // ════════════════════════════════════════════════════════════
//
//    /**
//     * 单个视频的配置
//     * @param path 视频文件路径
//     * @param normalizedRect 在画面中的归一化位置 [0,1]（left, top, right, bottom）
//     * @param layer 绘制层级（大的在上层），默认 0
//     * @param isLooping 是否循环播放，默认 true
//     * @param isMute 是否静音（仅解码视频），默认 true
//     */
//    data class VideoConfig(
//        val path: String,
//        val normalizedRect: RectF = RectF(0f, 0f, 1f, 1f),
//        val layer: Int = 0,
//        val isLooping: Boolean = true,
//        val isMute: Boolean = true,
//    )
//
//    // ════════════════════════════════════════════════════════════
//    //  内部状态
//    // ════════════════════════════════════════════════════════════
//
//    /** 每路视频的运行时状态（GL 线程持有） */
//    private class VideoSlot {
//        // ── 配置（不变） ──
//        var config: VideoConfig = VideoConfig("")
//        // ── GL 资源（GL 线程创建/销毁） ──
//        var textureId: Int = 0
//        var surfaceTexture: SurfaceTexture? = null
//        /** MediaCodec 输出 Surface */
//        var producerSurface: Surface? = null
//        // ── 解码管线 ──
//        var extractor: MediaExtractor? = null
//        var codec: MediaCodec? = null
//        var decodeThread: Thread? = null
//        var videoWidth: Int = 0
//        var videoHeight: Int = 0
//        // ── 同步 ──
//        @Volatile var frameAvailable: Boolean = false
//        val isDecoding = AtomicBoolean(false)
//        val shouldBePlaying = AtomicBoolean(false)
//        // ── 顶点数据（NDC 空间） ──
//        val vertices = FloatArray(8)      // 4 个顶点 × 2 坐标
//        val texCoords = FloatArray(8)     // 保持不变：全纹理映射
//        val texMatrix = FloatArray(16)
//
//        fun initTexCoords() {
//            texCoords[0] = 0f; texCoords[1] = 0f   // bottom-left
//            texCoords[2] = 1f; texCoords[3] = 0f   // bottom-right
//            texCoords[4] = 0f; texCoords[5] = 1f   // top-left
//            texCoords[6] = 1f; texCoords[7] = 1f   // top-right
//            Matrix.setIdentityM(texMatrix, 0)
//        }
//
//        /** 根据归一化矩形更新 NDC 顶点 */
//        fun updateVertices() {
//            val r = config.normalizedRect
//            val l = r.left * 2f - 1f
//            val rgt = r.right * 2f - 1f
//            val t = 1f - r.top * 2f
//            val b = 1f - r.bottom * 2f
//            vertices[0] = l;   vertices[1] = b   // bottom-left
//            vertices[2] = rgt; vertices[3] = b   // bottom-right
//            vertices[4] = l;   vertices[5] = t   // top-left
//            vertices[6] = rgt; vertices[7] = t   // top-right
//        }
//    }
//
//    // ── 视频槽位 ──
//    private val slots = Array<VideoSlot?>(MAX_VIDEOS) { null }
//    private var activeCount = 0
//
//    // ── GL 状态 ──
//    private var program = 0
//    private var aPositionLoc = 0
//    private var aTexCoordLoc = 0
//    private var uTexMatrixLoc = 0
//    private var uTextureLoc = 0
//
//    // ── 缓冲区 ──
//    private var vertexBuf: FloatBuffer? = null
//    private var texCoordBuf: FloatBuffer? = null
//
//    // ── 生命周期 ──
//    private var glReady = false
//    private var destroyed = false
//
//    // ── 回调 ──
//    var onPreparedListener: (() -> Unit)? = null
//    var onErrorListener: ((msg: String) -> Unit)? = null
//
//    // ════════════════════════════════════════════════════════════
//    //  初始化
//    // ════════════════════════════════════════════════════════════
//
//    init {
//        setEGLContextClientVersion(2)
//        setEGLConfigChooser(8, 8, 8, 0, 0, 0)  // 无 alpha（合成由 shader 控制）
//        setRenderer(this)
//        // RENDERMODE_WHEN_DIRTY：有帧到达时才渲染，省电
//        renderMode = RENDERMODE_WHEN_DIRTY
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  公开 API
//    // ════════════════════════════════════════════════════════════
//
//    /**
//     * 设置多路视频源。
//     * 首次调用将创建解码管线；后续调用会停止旧的并启动新的。
//     *
//     * @param videos 视频配置，最多 [MAX_VIDEOS] 路
//     */
//    fun setVideos(vararg videos: VideoConfig) {
//        if (videos.size > MAX_VIDEOS) {
//            Log.w(TAG, "setVideos: too many videos (${videos.size}), truncating to $MAX_VIDEOS")
//        }
//        activeCount = min(videos.size, MAX_VIDEOS)
//
//        // 保存配置
//        for (i in 0 until activeCount) {
//            val slot = slots[i] ?: VideoSlot().also { slots[i] = it }
//            slot.config = videos[i]
//            slot.initTexCoords()
//            slot.updateVertices()
//        }
//
//        // 清理多余的 slot
//        for (i in activeCount until MAX_VIDEOS) {
//            slots[i]?.let { releaseSlot(it) }
//            slots[i] = null
//        }
//
//        if (glReady) {
//            initAllSurfaceTextures()
//            startAllDecodeThreads()
//        } else {
//            // GL 尚未就绪，等待 onSurfaceCreated
//        }
//    }
//
//    /**
//     * 更新某路视频的布局（无需重启解码管线）
//     */
//    fun updateVideoRect(index: Int, rect: RectF) {
//        if (index < 0 || index >= activeCount) return
//        val slot = slots[index] ?: return
//        slot.config = slot.config.copy(normalizedRect = rect)
//        slot.updateVertices()
//        requestRender()
//    }
//
//    /**
//     * 便利方法：设置四宫格布局（2×2）
//     */
//    fun setQuadLayout(vararg paths: String) {
//        require(paths.size <= 4) { "Quad layout supports at most 4 videos" }
//        val configs = paths.mapIndexed { i, path ->
//            val col = i % 2
//            val row = i / 2
//            VideoConfig(
//                path = path,
//                normalizedRect = RectF(
//                    col * 0.5f, row * 0.5f,
//                    (col + 1) * 0.5f, (row + 1) * 0.5f
//                ),
//                layer = i,
//            )
//        }
//        setVideos(*configs.toTypedArray())
//    }
//
//    /**
//     * 便利方法：设置画中画布局
//     * @param mainPath 主视频路径（全屏）
//     * @param pipPaths 子视频路径列表（右下角叠加）
//     */
//    fun setPipLayout(mainPath: String, vararg pipPaths: String) {
//        val configs = mutableListOf(
//            VideoConfig(mainPath, normalizedRect = RectF(0f, 0f, 1f, 1f), layer = 0)
//        )
//        pipPaths.forEachIndexed { i, path ->
//            // 每个子视频按 1/4 大小层叠在右下角，偏移递增
//            val inset = 0.15f * (i + 1)
//            val size = 0.25f
//            configs += VideoConfig(
//                path = path,
//                normalizedRect = RectF(
//                    1f - size - inset, 1f - size - inset,
//                    1f - inset, 1f - inset
//                ),
//                layer = i + 1,
//            )
//        }
//        setVideos(*configs.toTypedArray())
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  生命周期
//    // ════════════════════════════════════════════════════════════
//
//    override fun onAttachedToWindow() {
//        Log.d(TAG, "onAttachedToWindow")
//        destroyed = false
//        super.onAttachedToWindow()
//    }
//
//    override fun onDetachedFromWindow() {
//        Log.d(TAG, "onDetachedFromWindow")
//        destroyed = true
//        glReady = false
//        releaseAllCodecs()
//        releaseAllGLResources()
//        super.onDetachedFromWindow()
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  GLSurfaceView.Renderer 实现
//    // ════════════════════════════════════════════════════════════
//
//    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        Log.d(TAG, "onSurfaceCreated")
//        if (destroyed) return
//
//        // 1. 编译 Shader
//        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
//        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
//        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
//        uTexMatrixLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")
//        uTextureLoc = GLES20.glGetUniformLocation(program, "uTexture")
//
//        // 2. 创建顶点缓冲区
//        vertexBuf = createFloatBuffer(8)
//        texCoordBuf = createFloatBuffer(8)
//
//        // 3. 为每个活跃视频创建 SurfaceTexture
//        initAllSurfaceTextures()
//
//        GLES20.glUseProgram(program)
//        glReady = true
//
//        // 4. 启动解码线程
//        startAllDecodeThreads()
//    }
//
//    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//        Log.d(TAG, "onSurfaceChanged: ${width}x$height")
//        GLES20.glViewport(0, 0, width, height)
//    }
//
//    override fun onDrawFrame(gl: GL10?) {
//        if (destroyed || !glReady) return
//
//        // 清屏
//        GLES20.glClearColor(0f, 0f, 0f, 1f)
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//
//        GLES20.glUseProgram(program)
//        GLES20.glEnableVertexAttribArray(aPositionLoc)
//        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
//
//        // 启用混合（支持叠加区域透明）
//        GLES20.glEnable(GLES20.GL_BLEND)
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
//
//        // 按 layer 排序，从低到高绘制（低 layer 先画，高 layer 叠在上面）
//        val sorted = (0 until activeCount)
//            .map { slots[it] }
//            .filterNotNull()
//            .sortedBy { it.config.layer }
//
//        for (slot in sorted) {
//            drawSlot(slot)
//        }
//
//        GLES20.glDisable(GLES20.GL_BLEND)
//
//        GLES20.glDisableVertexAttribArray(aPositionLoc)
//        GLES20.glDisableVertexAttribArray(aTexCoordLoc)
//    }
//
//    /** 绘制一路视频 quad */
//    private fun drawSlot(slot: VideoSlot) {
//        val texId = slot.textureId
//        if (texId == 0) return
//
//        // 更新 SurfaceTexture 纹理内容
//        val st = slot.surfaceTexture
//        if (st != null && slot.frameAvailable) {
//            try {
//                st.updateTexImage()
//                st.getTransformMatrix(slot.texMatrix)
//            } catch (e: Exception) {
//                Log.w(TAG, "updateTexImage failed for slot", e)
//                return
//            }
//            slot.frameAvailable = false
//        }
//
//        // 上传顶点数据
//        val vBuf = vertexBuf ?: return
//        vBuf.clear()
//        vBuf.put(slot.vertices)
//        vBuf.position(0)
//        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vBuf)
//
//        // 上传纹理坐标
//        val tBuf = texCoordBuf ?: return
//        tBuf.clear()
//        tBuf.put(slot.texCoords)
//        tBuf.position(0)
//        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, tBuf)
//
//        // 绑定纹理
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
//        GLES20.glUniform1i(uTextureLoc, 0)
//        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, slot.texMatrix, 0)
//
//        // 绘制 quad（三角形条带：4 个顶点 → 2 个三角形）
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  SurfaceTexture 管理（GL 线程执行）
//    // ════════════════════════════════════════════════════════════
//
//    /** 创建或重建所有 SurfaceTexture */
//    private fun initAllSurfaceTextures() {
//        for (i in 0 until activeCount) {
//            initOneSurfaceTexture(i)
//        }
//    }
//
//    /** 为单路视频创建 SurfaceTexture（GL 线程） */
//    private fun initOneSurfaceTexture(index: Int) {
//        val slot = slots[index] ?: return
//
//        // 释放旧的
//        slot.surfaceTexture?.let {
//            it.setOnFrameAvailableListener(null)
//            it.release()
//        }
//        slot.producerSurface?.release()
//        if (slot.textureId != 0) {
//            GLES20.glDeleteTextures(1, intArrayOf(slot.textureId), 0)
//        }
//
//        // 生成新的纹理 ID
//        val textures = IntArray(1)
//        GLES20.glGenTextures(1, textures, 0)
//        slot.textureId = textures[0]
//
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, slot.textureId)
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
//            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
//            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
//            GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
//            GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
//
//        val surfaceTexture = SurfaceTexture(slot.textureId)
//        surfaceTexture.setOnFrameAvailableListener { onFrameAvailable(slot) }
//        // 设置默认缓冲区大小，避免纹理拉伸
//        surfaceTexture.setDefaultBufferSize(
//            max(slot.videoWidth, MAX_VIDEO_WIDTH),
//            max(slot.videoHeight, MAX_VIDEO_HEIGHT)
//        )
//
//        slot.surfaceTexture = surfaceTexture
//        slot.producerSurface = Surface(surfaceTexture)
//        slot.frameAvailable = false
//    }
//
//    /** SurfaceTexture 帧可用回调（任意线程） */
//    private fun onFrameAvailable(slot: VideoSlot) {
//        slot.frameAvailable = true
//        requestRender()
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  MediaCodec 解码管理
//    // ════════════════════════════════════════════════════════════
//
//    /** 启动所有活跃视频的解码线程 */
//    private fun startAllDecodeThreads() {
//        for (i in 0 until activeCount) {
//            val slot = slots[i] ?: continue
//            if (slot.config.path.isEmpty()) continue
//            if (slot.isDecoding.get()) continue
//            if (slot.producerSurface == null) continue
//
//            startDecodeThread(i)
//        }
//        onPreparedListener?.invoke()
//    }
//
//    /** 启动单路解码线程 */
//    private fun startDecodeThread(index: Int) {
//        val slot = slots[index] ?: return
//        val surface = slot.producerSurface ?: return
//        val path = slot.config.path
//        val isLooping = slot.config.isLooping
//
//        slot.isDecoding.set(true)
//        slot.shouldBePlaying.set(true)
//
//        slot.decodeThread = Thread({
//            decodeLoop(slot, path, surface, isLooping)
//        }, "mvc-decoder-$index").apply { start() }
//    }
//
//    /** 解码循环（后台线程） */
//    private fun decodeLoop(
//        slot: VideoSlot,
//        path: String,
//        surface: Surface,
//        isLooping: Boolean,
//    ) {
//        Log.d(TAG, "decodeLoop: started for $path")
//
//        var extractor: MediaExtractor? = null
//        var codec: MediaCodec? = null
//
//        try {
//            // 1. 初始化 Extractor
//            val ex = MediaExtractor().apply {
//                setDataSource(context, Uri.parse(path), emptyMap())
//            }
//            extractor = ex
//
//            val videoTrackIndex = findVideoTrack(ex)
//            if (videoTrackIndex < 0) {
//                Log.e(TAG, "decodeLoop: no video track in $path")
//                return
//            }
//
//            val format = ex.getTrackFormat(videoTrackIndex)
//            val mime = format.getString(MediaFormat.KEY_MIME) ?: "video/avc"
//            slot.videoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
//            slot.videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
//            Log.d(TAG, "decodeLoop: ${slot.videoWidth}x${slot.videoHeight}, mime=$mime")
//
//            ex.selectTrack(videoTrackIndex)
//
//            // 2. 设置 SurfaceTexture 缓冲区大小
//            queueEvent {
//                slot.surfaceTexture?.setDefaultBufferSize(
//                    slot.videoWidth, slot.videoHeight
//                )
//            }
//
//            // 3. 创建 MediaCodec
//            val cd = MediaCodec.createDecoderByType(mime)
//            cd.configure(format, surface, null, 0)
//            cd.start()
//            slot.codec = cd
//            slot.extractor = ex
//
//            // 4. 解码循环
//            val bufferInfo = MediaCodec.BufferInfo()
//            var inputEos = false
//            var baseTimeNs = 0L
//            var basePtsUs = 0L
//            var isFirstFrame = true
//
//            while (slot.isDecoding.get() && !destroyed) {
//                // 暂停控制
//                if (!slot.shouldBePlaying.get()) {
//                    try { Thread.sleep(50) } catch (_: InterruptedException) { break }
//                    continue
//                }
//
//                // ── 喂输入 ──
//                if (!inputEos) {
//                    val inputIndex = cd.dequeueInputBuffer(TIMEOUT_US)
//                    if (inputIndex >= 0) {
//                        val buf = cd.getInputBuffer(inputIndex) ?: continue
//                        val sampleSize = ex.readSampleData(buf, 0)
//                        if (sampleSize < 0) {
//                            if (isLooping) {
//                                ex.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
//                                cd.flush()
//                                inputEos = false
//                                isFirstFrame = true
//                                continue
//                            } else {
//                                cd.queueInputBuffer(inputIndex, 0, 0, 0,
//                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
//                                inputEos = true
//                            }
//                        } else {
//                            cd.queueInputBuffer(inputIndex, 0, sampleSize,
//                                ex.sampleTime, 0)
//                            ex.advance()
//                        }
//                    }
//                }
//
//                // ── 取输出 ──
//                val outputIndex = cd.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
//                when {
//                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
//                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {}
//                    outputIndex >= 0 -> {
//                        val doRender = bufferInfo.size > 0
//                        cd.releaseOutputBuffer(outputIndex, doRender)
//
//                        // PTS 时序同步
//                        if (doRender && bufferInfo.size > 0) {
//                            val ptsUs = bufferInfo.presentationTimeUs
//                            if (isFirstFrame) {
//                                baseTimeNs = System.nanoTime()
//                                basePtsUs = ptsUs
//                                isFirstFrame = false
//                            } else {
//                                val targetNs = baseTimeNs + (ptsUs - basePtsUs) * 1000
//                                val now = System.nanoTime()
//                                if (now < targetNs) {
//                                    val sleep = targetNs - now
//                                    Thread.sleep(
//                                        sleep / 1_000_000,
//                                        (sleep % 1_000_000).toInt()
//                                    )
//                                }
//                            }
//                        }
//
//                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
//                            if (isLooping && slot.isDecoding.get()) {
//                                ex.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
//                                cd.flush()
//                                inputEos = false
//                                isFirstFrame = true
//                            } else {
//                                break
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (e: IllegalStateException) {
//            Log.d(TAG, "decodeLoop: codec stopped externally, exiting")
//        } catch (e: Exception) {
//            Log.e(TAG, "decodeLoop: error for $path", e)
//            onErrorListener?.invoke("Decode error: ${e.message}")
//        } finally {
//            // 清理资源
//            try { codec?.stop() } catch (_: Exception) {}
//            codec?.release()
//            extractor?.release()
//            slot.codec = null
//            slot.extractor = null
//            slot.isDecoding.set(false)
//            Log.d(TAG, "decodeLoop: exited for $path")
//        }
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  资源释放
//    // ════════════════════════════════════════════════════════════
//
//    /** 停止并释放所有解码器 */
//    private fun releaseAllCodecs() {
//        for (i in 0 until MAX_VIDEOS) {
//            slots[i]?.let { releaseCodec(it) }
//        }
//    }
//
//    /** 释放单路解码器 */
//    private fun releaseCodec(slot: VideoSlot) {
//        slot.isDecoding.set(false)
//        slot.shouldBePlaying.set(false)
//        val thread = slot.decodeThread
//        slot.decodeThread = null
//        thread?.interrupt()
//        try { thread?.join(500) } catch (_: InterruptedException) {}
//
//        try { slot.codec?.stop() } catch (_: Exception) {}
//        slot.codec?.release()
//        slot.extractor?.release()
//        slot.codec = null
//        slot.extractor = null
//    }
//
//    /** 释放某个 slot 所有资源 */
//    private fun releaseSlot(slot: VideoSlot) {
//        releaseCodec(slot)
//        // GL 资源需要在 GL 线程释放
//        queueEvent {
//            slot.surfaceTexture?.let {
//                it.setOnFrameAvailableListener(null)
//                it.release()
//            }
//            slot.producerSurface?.release()
//            if (slot.textureId != 0) {
//                try { GLES20.glDeleteTextures(1, intArrayOf(slot.textureId), 0) } catch (_: Exception) {}
//            }
//            slot.surfaceTexture = null
//            slot.producerSurface = null
//            slot.textureId = 0
//        }
//    }
//
//    /** 释放所有 GL 资源 */
//    private fun releaseAllGLResources() {
//        for (i in 0 until MAX_VIDEOS) {
//            val slot = slots[i] ?: continue
//            slot.surfaceTexture?.let {
//                it.setOnFrameAvailableListener(null)
//                it.release()
//            }
//            slot.producerSurface?.release()
//            slot.surfaceTexture = null
//            slot.producerSurface = null
//            if (slot.textureId != 0) {
//                try { GLES20.glDeleteTextures(1, intArrayOf(slot.textureId), 0) } catch (_: Exception) {}
//                slot.textureId = 0
//            }
//        }
//        if (program != 0) {
//            try { GLES20.glDeleteProgram(program) } catch (_: Exception) {}
//            program = 0
//        }
//        glReady = false
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  工具函数
//    // ════════════════════════════════════════════════════════════
//
//    private fun findVideoTrack(extractor: MediaExtractor): Int {
//        for (i in 0 until extractor.trackCount) {
//            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
//            if (mime.startsWith("video/")) return i
//        }
//        return -1
//    }
//
//    private fun createFloatBuffer(size: Int): FloatBuffer {
//        return ByteBuffer.allocateDirect(size * 4)
//            .order(ByteOrder.nativeOrder())
//            .asFloatBuffer()
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  Shader 编译
//    // ════════════════════════════════════════════════════════════
//
//    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
//        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
//        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
//        val prog = GLES20.glCreateProgram()
//        GLES20.glAttachShader(prog, vs)
//        GLES20.glAttachShader(prog, fs)
//        GLES20.glLinkProgram(prog)
//        val linkStatus = IntArray(1)
//        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
//        GLES20.glDeleteShader(vs)
//        GLES20.glDeleteShader(fs)
//        if (linkStatus[0] == 0) {
//            val error = GLES20.glGetProgramInfoLog(prog)
//            GLES20.glDeleteProgram(prog)
//            throw IllegalStateException("Link program failed: $error")
//        }
//        return prog
//    }
//
//    private fun compileShader(type: Int, source: String): Int {
//        val shader = GLES20.glCreateShader(type)
//        GLES20.glShaderSource(shader, source)
//        GLES20.glCompileShader(shader)
//        val compileStatus = IntArray(1)
//        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
//        if (compileStatus[0] == 0) {
//            val error = GLES20.glGetShaderInfoLog(shader)
//            GLES20.glDeleteShader(shader)
//            throw IllegalStateException("Compile shader failed: $error")
//        }
//        return shader
//    }
//}
