//package com.cn.core.ui.view.media
//
//import android.graphics.RectF
//import android.graphics.SurfaceTexture
//import android.media.MediaCodec
//import android.media.MediaExtractor
//import android.media.MediaFormat
//import android.media.MediaMuxer
//import android.net.Uri
//import android.opengl.EGL14
//import android.opengl.EGLConfig
//import android.opengl.EGLContext
//import android.opengl.EGLDisplay
//import android.opengl.EGLSurface
//import android.opengl.GLES11Ext
//import android.opengl.GLES20
//import android.opengl.Matrix
//import android.util.Log
//import android.view.Surface
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.nio.FloatBuffer
//import java.util.concurrent.atomic.AtomicBoolean
//import kotlin.math.min
//
///**
// * 离线多视频合成编码器
// *
// * **GLES20 + MediaCodec (Dec × N) + MediaCodec (Enc) + EGL 全 GPU 管线**
// *
// * ## 架构
// * ```
// * GLMultiVideoEncoder (后台线程)
// * ├── EGL Display / Context / Surface     ← 编码器输入 Surface
// * ├── N × SurfaceTexture (GL_TEXTURE_EXTERNAL_OES)  ← 零拷贝 GPU 纹理
// * ├── N × DecodeThread (MediaCodec × N)  ← 各自解码到 SurfaceTexture
// * ├── GL 渲染管线                         ← 多纹理合成
// * └── MediaCodec Encoder + MediaMuxer     ← 产出 MP4 文件
// * ```
// *
// * ## 数据流
// * ```
// * ┌──────────┐   Surface    ┌──────────────┐   updateTexImage   ┌────────┐   quad draw   ┌────────────────┐
// * │ Codec #0 │ ──────────→ │ SurfaceTex #0 │ ←────────────── │ GL     │ ───────────→ │ Encoder Surface │
// * │ Codec #1 │ ──────────→ │ SurfaceTex #1 │ ←────────────── │ Shader │ ───────────→ │  → MediaCodec   │
// * │ Codec #2 │ ──────────→ │ SurfaceTex #2 │ ←────────────── │ 合成    │ ───────────→ │  → MediaMuxer   │
// * └──────────┘             └──────────────┘                   └────────┘              └────────────────┘
// *   解码线程（独立）          GL 纹理（GPU）              渲染循环（单线程）           编码线程（同步）
// * ```
// *
// * ## 使用示例
// * ```kotlin
// * val encoder = GLMultiVideoEncoder(
// *     outputPath = "/sdcard/composite.mp4",
// *     outputWidth = 1920,
// *     outputHeight = 1080,
// *     outputFps = 30,
// * ).apply {
// *     onProgressListener = { progress ->
// *         Log.d(TAG, "progress: ${"%.1f".format(progress * 100)}%")
// *     }
// *     onCompleteListener = { path ->
// *         Log.d(TAG, "completed: $path")
// *     }
// *     onErrorListener = { msg ->
// *         Log.e(TAG, "error: $msg")
// *     }
// * }
// *
// * // 四宫格合成
// * encoder.setVideos(
// *     VideoConfig("/sdcard/v1.mp4", RectF(0f, 0f, 0.5f, 0.5f)),
// *     VideoConfig("/sdcard/v2.mp4", RectF(0.5f, 0f, 1f, 0.5f)),
// *     VideoConfig("/sdcard/v3.mp4", RectF(0f, 0.5f, 0.5f, 1f)),
// *     VideoConfig("/sdcard/v4.mp4", RectF(0.5f, 0.5f, 1f, 1f)),
// * )
// * encoder.start()  // 同步阻塞直到完成，或在后台线程调用
// *
// * // 也可以异步
// * Thread { encoder.start() }.start()
// * ```
// *
// * ## 性能
// * - 解码到 SurfaceTexture：GPU 零拷贝
// * - 合成渲染到编码器 Surface：GPU 直通，无 CPU 介入
// * - 唯一延迟：eglSwapBuffers 触发编码器消费帧
// */
//class GLMultiVideoEncoder(
//    /** Android Context（用于 MediaExtractor 数据源） */
//    private val context: android.content.Context,
//    /** 输出文件路径 */
//    private val outputPath: String,
//    /** 输出视频宽度（px） */
//    private val outputWidth: Int,
//    /** 输出视频高度（px） */
//    private val outputHeight: Int,
//    /** 输出帧率 */
//    private val outputFps: Int = 30,
//    /** 输出码率（bps） */
//    private val outputBitrate: Int = 8_000_000,
//) {
//
//    companion object {
//        private const val TAG = "GLMultiVideoEnc"
//        private const val MAX_VIDEOS = 6
//        private const val TIMEOUT_US = 10_000L
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
//    //  配置
//    // ════════════════════════════════════════════════════════════
//
//    /**
//     * 单路视频配置
//     * @param path 视频文件路径
//     * @param normalizedRect 在输出画面中的归一化位置 [0,1]
//     * @param layer 绘制层级（大的在上层）
//     * @param isLooping 是否循环（编码场景通常 false）
//     */
//    data class VideoConfig(
//        val path: String,
//        val normalizedRect: RectF = RectF(0f, 0f, 1f, 1f),
//        val layer: Int = 0,
//        val isLooping: Boolean = false,
//    )
//
//    // ════════════════════════════════════════════════════════════
//    //  回调
//    // ════════════════════════════════════════════════════════════
//
//    /** 进度回调 [0, 1]，在编码线程中调用 */
//    var onProgressListener: ((progress: Float) -> Unit)? = null
//    /** 完成回调，参数为输出文件路径 */
//    var onCompleteListener: ((outputPath: String) -> Unit)? = null
//    /** 错误回调 */
//    var onErrorListener: ((errorMsg: String) -> Unit)? = null
//
//    // ════════════════════════════════════════════════════════════
//    //  内部状态
//    // ════════════════════════════════════════════════════════════
//
//    /** 每路视频的运行时状态 */
//    private inner class VideoSlot(
//        val config: VideoConfig,
//    ) {
//        // 解码器
//        var extractor: MediaExtractor? = null
//        var codec: MediaCodec? = null
//        var decodeThread: Thread? = null
//        var videoWidth: Int = 0
//        var videoHeight: Int = 0
//        var durationUs: Long = 0L    // 视频总时长
//
//        // GL
//        var textureId: Int = 0
//        var surfaceTexture: SurfaceTexture? = null
//        var producerSurface: Surface? = null
//
//        // 同步
//        @Volatile var frameAvailable: Boolean = false
//        val isDecoding = AtomicBoolean(false)
//        @Volatile var eosReached: Boolean = false
//
//        // 顶点数据
//        val vertices = FloatArray(8)
//        val texCoords = FloatArray(8)   // [0,0, 1,0, 0,1, 1,1]
//        val texMatrix = FloatArray(16)
//
//        init {
//            texCoords[0] = 0f; texCoords[1] = 0f
//            texCoords[2] = 1f; texCoords[3] = 0f
//            texCoords[4] = 0f; texCoords[5] = 1f
//            texCoords[6] = 1f; texCoords[7] = 1f
//            Matrix.setIdentityM(texMatrix, 0)
//            updateVertices()
//        }
//
//        fun updateVertices() {
//            val r = config.normalizedRect
//            val l = r.left * 2f - 1f
//            val rt = r.right * 2f - 1f
//            val t = 1f - r.top * 2f
//            val b = 1f - r.bottom * 2f
//            vertices[0] = l;  vertices[1] = b
//            vertices[2] = rt; vertices[3] = b
//            vertices[4] = l;  vertices[5] = t
//            vertices[6] = rt; vertices[7] = t
//        }
//    }
//
//    private val slots = mutableListOf<VideoSlot>()
//    private val started = AtomicBoolean(false)
//    private val cancelled = AtomicBoolean(false)
//
//    // ════════════════════════════════════════════════════════════
//    //  公开 API
//    // ════════════════════════════════════════════════════════════
//
//    /** 设置视频源（必须在 start() 前调用） */
//    fun setVideos(vararg videos: VideoConfig) {
//        if (started.get()) {
//            throw IllegalStateException("Cannot set videos after start()")
//        }
//        val count = min(videos.size, MAX_VIDEOS)
//        slots.clear()
//        for (i in 0 until count) {
//            slots.add(VideoSlot(videos[i]))
//        }
//    }
//
//    /** 设置四宫格快捷布局 */
//    fun setQuadLayout(vararg paths: String) {
//        require(paths.size <= 4)
//        val configs = paths.mapIndexed { i, path ->
//            val col = i % 2; val row = i / 2
//            VideoConfig(path, RectF(col * 0.5f, row * 0.5f, (col + 1) * 0.5f, (row + 1) * 0.5f))
//        }
//        setVideos(*configs.toTypedArray())
//    }
//
//    /** 取消正在进行的编码（可在任意线程调用） */
//    fun cancel() {
//        cancelled.set(true)
//    }
//
//    /**
//     * 开始编码，**同步阻塞**直到完成或出错。
//     *
//     * 建议在后台线程调用：
//     * ```kotlin
//     * thread { encoder.start() }
//     * ```
//     */
//    fun start() {
//        if (slots.isEmpty()) {
//            onErrorListener?.invoke("No videos configured")
//            return
//        }
//        if (!started.compareAndSet(false, true)) {
//            Log.w(TAG, "start() already in progress")
//            return
//        }
//
//        Log.i(TAG, "start: output=${outputPath}, ${outputWidth}x${outputHeight} @${outputFps}fps")
//
//        try {
//            encodeLoop()
//        } catch (e: Exception) {
//            Log.e(TAG, "encode failed", e)
//            onErrorListener?.invoke("Encode failed: ${e.message}")
//        } finally {
//            started.set(false)
//            cancelled.set(false)
//        }
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  主编码循环（后台线程执行）
//    // ════════════════════════════════════════════════════════════
//
//    private fun encodeLoop() {
//        // ── 1. 创建 EGL 上下文 ──
//        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
//        if (eglDisplay == EGL14.EGL_NO_DISPLAY) throw RuntimeException("eglGetDisplay failed")
//
//        val version = IntArray(2)
//        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1))
//            throw RuntimeException("eglInitialize failed")
//        Log.d(TAG, "EGL initialized: ${version[0]}.${version[1]}")
//
//        // ── 2. 选择 EGL 配置 ──
//        val configAttribs = intArrayOf(
//            EGL14.EGL_RED_SIZE, 8,
//            EGL14.EGL_GREEN_SIZE, 8,
//            EGL14.EGL_BLUE_SIZE, 8,
//            EGL14.EGL_ALPHA_SIZE, 8,
//            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
//            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
//            EGL14.EGL_NONE,
//        )
//        val configs = arrayOfNulls<EGLConfig>(1)
//        val numConfigs = IntArray(1)
//        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
//            throw RuntimeException("eglChooseConfig failed")
//        }
//        val eglConfig = configs[0]!!
//
//        // ── 3. 创建 EGL 上下文 ──
//        val contextAttribs = intArrayOf(
//            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
//            EGL14.EGL_NONE,
//        )
//        val eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
//        if (eglContext == EGL14.EGL_NO_CONTEXT) {
//            throw RuntimeException("eglCreateContext failed, error=${EGL14.eglGetError()}")
//        }
//
//        // ── 4. 创建编码器 Surface ──
//        val encoder = createVideoEncoder()
//        val encoderSurface = encoder.inputSurface
//        val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, encoderSurface, intArrayOf(EGL14.EGL_NONE), 0)
//        if (eglSurface == EGL14.EGL_NO_SURFACE) {
//            throw RuntimeException("eglCreateWindowSurface failed")
//        }
//
//        // ── 5. 绑定 EGL 上下文 ──
//        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
//            throw RuntimeException("eglMakeCurrent failed")
//        }
//
//        try {
//            // ── 6. 初始化 GL ──
//            GLES20.glViewport(0, 0, outputWidth, outputHeight)
//            val program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
//            val aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
//            val aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
//            val uTexMatrixLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")
//            val uTextureLoc = GLES20.glGetUniformLocation(program, "uTexture")
//
//            val vertexBuf = createFloatBuffer(8)
//            val texCoordBuf = createFloatBuffer(8)
//
//            GLES20.glUseProgram(program)
//
//            // ── 7. 创建 SurfaceTexture 并启动解码器 ──
//            for (slot in slots) {
//                initSurfaceTexture(slot)
//            }
//            startAllDecoders()
//
//            // ── 8. 获取最大时长（用于进度计算） ──
//            val maxDurationUs = slots.maxOfOrNull { it.durationUs } ?: 1_000_000L
//            val frameIntervalNs = 1_000_000_000L / outputFps
//
//            // ── 9. 获取解码器帧率（用于渲染节奏） ──
//            // 以所有视频中最慢的帧率为渲染基准，避免过快 eglSwapBuffers
//            val minFrameIntervalUs = slots.mapNotNull { slot ->
//                val fmt = slot.extractor?.getTrackFormat(
//                    findVideoTrackIndex(slot.extractor!!)
//                ) ?: return@mapNotNull null
//                val frameRate = fmt.getFloat(MediaFormat.KEY_FRAME_RATE)
//                    .takeIf { it > 0 } ?: outputFps.toFloat()
//                (1_000_000L / frameRate).toLong()
//            }.minOrNull() ?: (1_000_000L / outputFps)
//
//            Log.d(TAG, "encodeLoop: maxDuration=${maxDurationUs}us, minFrameInterval=${minFrameIntervalUs}us")
//
//            // ═════════════════════════════════════════════════════
//            //  渲染编码循环
//            // ═════════════════════════════════════════════════════
//            var frameIndex = 0L
//            val startTimeNs = System.nanoTime()
//            var expectedFrameCount = (maxDurationUs * outputFps / 1_000_000).toLong()
//
//            while (!cancelled.get()) {
//                val nowNs = System.nanoTime()
//                val elapsedSinceStartUs = (nowNs - startTimeNs) / 1000
//
//                // 检查是否所有视频都已 EOS
//                val allEos = slots.all { it.eosReached }
//                // 检查是否超过最大时长（容差 1 秒）
//                val overtime = elapsedSinceStartUs > maxDurationUs + 1_000_000L
//
//                if (allEos || overtime) {
//                    Log.d(TAG, "encodeLoop: done. allEos=$allEos overtime=$overtime")
//                    break
//                }
//
//                // 渲染当前帧
//                var anyFrameRendered = false
//                for (slot in slots) {
//                    val st = slot.surfaceTexture ?: continue
//                    if (slot.frameAvailable && !slot.eosReached) {
//                        try {
//                            st.updateTexImage()
//                            st.getTransformMatrix(slot.texMatrix)
//                        } catch (e: Exception) {
//                            // updateTexImage 可能阻塞等待，超时处理
//                        }
//                        slot.frameAvailable = false
//                        anyFrameRendered = true
//                    }
//                }
//
//                // 绘制合成画面 → 编码器 Surface
//                GLES20.glClearColor(0f, 0f, 0f, 1f)
//                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//                GLES20.glEnable(GLES20.GL_BLEND)
//                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
//
//                GLES20.glUseProgram(program)
//                val sortedSlots = slots.sortedBy { it.config.layer }
//                for (slot in sortedSlots) {
//                    drawSlot(slot, aPositionLoc, aTexCoordLoc, uTexMatrixLoc, uTextureLoc, vertexBuf, texCoordBuf)
//                }
//
//                GLES20.glDisable(GLES20.GL_BLEND)
//                EGL14.eglSwapBuffers(eglDisplay, eglSurface)
//
//                // 进度回调
//                val progress = min(1f, elapsedSinceStartUs.toFloat() / maxDurationUs)
//                if (frameIndex % (outputFps / 2) == 0L) {
//                    onProgressListener?.invoke(progress)
//                }
//
//                frameIndex++
//
//                // 时序控制：以视频帧率为准推进
//                val targetElapsedUs = frameIndex * minFrameIntervalUs
//                val currentElapsedUs = (System.nanoTime() - startTimeNs) / 1000
//                if (currentElapsedUs < targetElapsedUs) {
//                    val sleepUs = targetElapsedUs - currentElapsedUs
//                    if (sleepUs > 1000) {
//                        Thread.sleep(sleepUs / 1000, ((sleepUs % 1000) * 1000).toInt())
//                    }
//                }
//            }
//
//            // ═════════════════════════════════════════════════════
//            //  完成编码
//            // ═════════════════════════════════════════════════════
//            Log.d(TAG, "encodeLoop: rendered $frameIndex frames, finalizing...")
//            onProgressListener?.invoke(1f)
//
//            // Signal EOS to encoder
//            encoder.signalEndOfInputStream()
//
//            // Drain encoder
//            drainEncoder(encoder)
//
//            // Finalize muxer
//            muxer?.stop()
//            muxer?.release()
//            muxer = null
//            encoderRunning = false
//
//            Log.i(TAG, "encodeLoop: completed -> $outputPath")
//            onCompleteListener?.invoke(outputPath)
//
//        } catch (e: Exception) {
//            Log.e(TAG, "encodeLoop error", e)
//            onErrorListener?.invoke("Encode error: ${e.message}")
//        } finally {
//            // ── 清理 ──
//            releaseAllDecoders()
//            for (slot in slots) {
//                slot.surfaceTexture?.let {
//                    it.setOnFrameAvailableListener(null)
//                    it.release()
//                }
//                slot.producerSurface?.release()
//                if (slot.textureId != 0) {
//                    try { GLES20.glDeleteTextures(1, intArrayOf(slot.textureId), 0) } catch (_: Exception) {}
//                }
//            }
//            slots.clear()
//
//            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
//            EGL14.eglDestroySurface(eglDisplay, eglSurface)
//            EGL14.eglDestroyContext(eglDisplay, eglContext)
//            EGL14.eglTerminate(eglDisplay)
//
//            if (muxer != null) {
//                try { muxer?.stop() } catch (_: Exception) {}
//                try { muxer?.release() } catch (_: Exception) {}
//                muxer = null
//            }
//            encoderRunning = false
//            Log.d(TAG, "encodeLoop: cleanup done")
//        }
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  SurfaceTexture 初始化
//    // ════════════════════════════════════════════════════════════
//
//    private fun initSurfaceTexture(slot: VideoSlot) {
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
//        val st = SurfaceTexture(slot.textureId)
//        st.setOnFrameAvailableListener { slot.frameAvailable = true }
//        st.setDefaultBufferSize(
//            if (slot.videoWidth > 0) slot.videoWidth else 1920,
//            if (slot.videoHeight > 0) slot.videoHeight else 1080,
//        )
//        slot.surfaceTexture = st
//        slot.producerSurface = Surface(st)
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  解码器管理
//    // ════════════════════════════════════════════════════════════
//
//    private fun startAllDecoders() {
//        for (i in slots.indices) {
//            startDecoder(i)
//        }
//    }
//
//    private fun startDecoder(index: Int) {
//        val slot = slots[index]
//        val surface = slot.producerSurface ?: return
//        val path = slot.config.path
//
//        slot.isDecoding.set(true)
//        slot.decodeThread = Thread({
//            decoderLoop(slot, path, surface)
//        }, "enc-decoder-$index").apply { start() }
//    }
//
//    private fun decoderLoop(slot: VideoSlot, path: String, surface: Surface) {
//        var extractor: MediaExtractor? = null
//        var codec: MediaCodec? = null
//
//        try {
//            val ex = MediaExtractor().apply {
//                setDataSource(context, Uri.parse(path), emptyMap())
//            }
//            extractor = ex
//
//            val trackIndex = findVideoTrackIndex(ex)
//            if (trackIndex < 0) {
//                Log.e(TAG, "decoderLoop: no video track in $path")
//                slot.eosReached = true
//                return
//            }
//
//            val fmt = ex.getTrackFormat(trackIndex)
//            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: "video/avc"
//            slot.videoWidth = fmt.getInteger(MediaFormat.KEY_WIDTH)
//            slot.videoHeight = fmt.getInteger(MediaFormat.KEY_HEIGHT)
//
//            // 获取时长
//            slot.durationUs = fmt.getLong(MediaFormat.KEY_DURATION).takeIf { it > 0 }
//                ?: ex.run {
//                    // 找完所有帧确定时长
//                    var lastPts = 0L
//                    while (advance()) {
//                        lastPts = sampleTime
//                    }
//                    seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
//                    lastPts
//                }
//
//            // 设置 SurfaceTexture 缓冲区大小
//            slot.surfaceTexture?.setDefaultBufferSize(slot.videoWidth, slot.videoHeight)
//
//            ex.selectTrack(trackIndex)
//
//            val cd = MediaCodec.createDecoderByType(mime)
//            cd.configure(fmt, surface, null, 0)
//            cd.start()
//            slot.codec = cd
//            slot.extractor = ex
//
//            val bufferInfo = MediaCodec.BufferInfo()
//            var inputEos = false
//            var baseTimeNs = 0L
//            var basePtsUs = 0L
//            var isFirstFrame = true
//
//            while (slot.isDecoding.get() && !cancelled.get()) {
//                // ── 输入 ──
//                if (!inputEos) {
//                    val inputIndex = cd.dequeueInputBuffer(TIMEOUT_US)
//                    if (inputIndex >= 0) {
//                        val buf = cd.getInputBuffer(inputIndex) ?: continue
//                        val sz = ex.readSampleData(buf, 0)
//                        if (sz < 0) {
//                            cd.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
//                            inputEos = true
//                        } else {
//                            cd.queueInputBuffer(inputIndex, 0, sz, ex.sampleTime, 0)
//                            ex.advance()
//                        }
//                    }
//                }
//
//                // ── 输出 ──
//                val outputIndex = cd.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
//                when {
//                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
//                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {}
//                    outputIndex >= 0 -> {
//                        val doRender = bufferInfo.size > 0
//                        cd.releaseOutputBuffer(outputIndex, doRender)
//
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
//                                    Thread.sleep(sleep / 1_000_000, (sleep % 1_000_000).toInt())
//                                }
//                            }
//                        }
//
//                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
//                            slot.eosReached = true
//                            break
//                        }
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "decoderLoop error for $path", e)
//        } finally {
//            try { codec?.stop() } catch (_: Exception) {}
//            codec?.release()
//            extractor?.release()
//            slot.codec = null
//            slot.extractor = null
//            slot.isDecoding.set(false)
//            slot.eosReached = true
//            Log.d(TAG, "decoderLoop: exited for $path")
//        }
//    }
//
//    private fun releaseAllDecoders() {
//        for (slot in slots) {
//            slot.isDecoding.set(false)
//            val thread = slot.decodeThread
//            slot.decodeThread = null
//            thread?.interrupt()
//            try { thread?.join(500) } catch (_: InterruptedException) {}
//            try { slot.codec?.stop() } catch (_: Exception) {}
//            slot.codec?.release()
//            slot.extractor?.release()
//            slot.codec = null
//            slot.extractor = null
//        }
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  GL 渲染
//    // ════════════════════════════════════════════════════════════
//
//    private fun drawSlot(
//        slot: VideoSlot,
//        aPositionLoc: Int,
//        aTexCoordLoc: Int,
//        uTexMatrixLoc: Int,
//        uTextureLoc: Int,
//        vertexBuf: FloatBuffer,
//        texCoordBuf: FloatBuffer,
//    ) {
//        if (slot.textureId == 0) return
//
//        GLES20.glEnableVertexAttribArray(aPositionLoc)
//        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
//
//        vertexBuf.clear()
//        vertexBuf.put(slot.vertices)
//        vertexBuf.position(0)
//        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuf)
//
//        texCoordBuf.clear()
//        texCoordBuf.put(slot.texCoords)
//        texCoordBuf.position(0)
//        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuf)
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, slot.textureId)
//        GLES20.glUniform1i(uTextureLoc, 0)
//        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, slot.texMatrix, 0)
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
//
//        GLES20.glDisableVertexAttribArray(aPositionLoc)
//        GLES20.glDisableVertexAttribArray(aTexCoordLoc)
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  MediaCodec 编码器 + MediaMuxer
//    // ════════════════════════════════════════════════════════════
//
//    private var encoder: MediaCodec? = null
//    private var muxer: MediaMuxer? = null
//    private var encoderRunning = false
//    private var trackIndex = -1
//
//    private fun createVideoEncoder(): MediaCodec {
//        val mime = MediaFormat.MIMETYPE_VIDEO_AVC  // H.264
//        val format = MediaFormat.createVideoFormat(mime, outputWidth, outputHeight).apply {
//            setInteger(MediaFormat.KEY_COLOR_FORMAT,
//                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
//            setInteger(MediaFormat.KEY_BIT_RATE, outputBitrate)
//            setInteger(MediaFormat.KEY_FRAME_RATE, outputFps)
//            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
//        }
//
//        val enc = MediaCodec.createEncoderByType(mime)
//        enc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
//        val inputSurface = enc.createInputSurface()
//        enc.start()
//
//        // 创建 Muxer
//        val file = File(outputPath)
//        file.parentFile?.mkdirs()
//        muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
//
//        this.encoder = enc
//        return enc
//    }
//
//    private fun drainEncoder(enc: MediaCodec) {
//        val bufferInfo = MediaCodec.BufferInfo()
//        var outputDone = false
//        val timeoutUs = 10_000L
//
//        while (!outputDone && !cancelled.get()) {
//            val outputIndex = enc.dequeueOutputBuffer(bufferInfo, timeoutUs)
//            when {
//                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
//                    outputDone = true  // no more output
//                }
//                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
//                    if (trackIndex >= 0) {
//                        Log.w(TAG, "drainEncoder: format changed twice")
//                    }
//                    trackIndex = muxer!!.addTrack(enc.outputFormat)
//                    muxer!!.start()
//                    encoderRunning = true
//                }
//                outputIndex >= 0 -> {
//                    val outputBuf = enc.getOutputBuffer(outputIndex) ?: continue
//                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
//                        bufferInfo.size = 0
//                    }
//
//                    if (bufferInfo.size > 0 && encoderRunning) {
//                        outputBuf.position(bufferInfo.offset)
//                        outputBuf.limit(bufferInfo.offset + bufferInfo.size)
//                        muxer!!.writeSampleData(trackIndex, outputBuf, bufferInfo)
//                    }
//
//                    enc.releaseOutputBuffer(outputIndex, false)
//
//                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
//                        outputDone = true
//                    }
//                }
//            }
//        }
//    }
//
//    // ════════════════════════════════════════════════════════════
//    //  工具函数
//    // ════════════════════════════════════════════════════════════
//
//    private fun findVideoTrackIndex(extractor: MediaExtractor): Int {
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
//    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
//        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
//        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
//        val prog = GLES20.glCreateProgram()
//        GLES20.glAttachShader(prog, vs)
//        GLES20.glAttachShader(prog, fs)
//        GLES20.glLinkProgram(prog)
//        val status = IntArray(1)
//        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
//        GLES20.glDeleteShader(vs)
//        GLES20.glDeleteShader(fs)
//        if (status[0] == 0) {
//            val err = GLES20.glGetProgramInfoLog(prog)
//            GLES20.glDeleteProgram(prog)
//            throw RuntimeException("Link program failed: $err")
//        }
//        return prog
//    }
//
//    private fun compileShader(type: Int, source: String): Int {
//        val shader = GLES20.glCreateShader(type)
//        GLES20.glShaderSource(shader, source)
//        GLES20.glCompileShader(shader)
//        val status = IntArray(1)
//        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
//        if (status[0] == 0) {
//            val err = GLES20.glGetShaderInfoLog(shader)
//            GLES20.glDeleteShader(shader)
//            throw RuntimeException("Compile shader failed: $err")
//        }
//        return shader
//    }
//}
