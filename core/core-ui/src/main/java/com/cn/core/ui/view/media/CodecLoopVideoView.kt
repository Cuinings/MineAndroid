package com.cn.core.ui.view.media

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.core.content.withStyledAttributes
import com.cn.core.ui.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 循环视频播放视图（MediaCodec + TextureView 方案）
 *
 * 基于 TextureView 渲染、MediaCodec 硬解码，完全手动控制解码管线。
 *
 * ## 特性
 * - 固定视频路径（代码设置）
 * - 循环播放（默认开启）
 * - 硬解码（MediaCodec），零拷贝渲染到 TextureView Surface
 * - 可见性感知：VISIBLE → 播放，GONE/INVISIBLE → 暂停
 * - 路径变更为非空 → 停止旧视频从开头重播
 * - 路径变更为空串 → 停止并释放
 * - 生命周期安全：detach 时自动释放
 * - 仅解码视频轨道（如需音频可扩展 AudioTrack）
 *
 * ## 与 VideoView / SurfaceView 方案对比
 *
 * | 特性 | CodecLoopVideoView | LoopVideoView | SurfaceLoopVideoView |
 * |------|-------------------|---------------|---------------------|
 * | 渲染层 | TextureView | VideoView(SurfaceView) | SurfaceView |
 * | 解码 | MediaCodec 硬解 | MediaPlayer | MediaPlayer |
 * | 控制粒度 | 帧级 | 播放器级 | 播放器级 |
 * | 零拷贝 | 是（Surface 直通） | 是 | 是 |
 * | 线程 | 独立解码线程 | VideoView 内部 | MediaPlayer 内部 |
 *
 * ## 使用示例
 * ```kotlin
 * val view = CodecLoopVideoView(context).apply {
 *     mVideoPath = "/sdcard/Movies/intro.mp4"
 *     visibility = View.VISIBLE  // 播放
 *     visibility = View.GONE     // 暂停
 * }
 * ```
 *
 * @constructor 创建 Codec 循环视频播放视图
 * @param context 上下文
 * @param attrs XML 属性集
 * @param defStyleAttr 默认样式属性
 *
 * @Author: WorkBuddy
 * @Time: 2026/7/3 16:45
 * @Description: MediaCodec + TextureView 方案，帧级控制循环视频播放
 */
class CodecLoopVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    companion object {
        private const val TAG = "CodecLoopVideo"
        private const val TIMEOUT_US = 10_000L
        private fun visibilityName(v: Int): String = when (v) {
            VISIBLE -> "VISIBLE"
            INVISIBLE -> "INVISIBLE"
            GONE -> "GONE"
            else -> "UNKNOWN($v)"
        }
    }

    // ── 公开属性 ──────────────────────────────────────────────

    /**
     * 视频路径。
     * - 设为非空：停旧播新，从开头重播
     * - 设为空串：停止释放
     */
    var videoPath: String = "/skyconfig/skyui/Pictures/touchx_wallpaper_dy_1.mp4"
        set(value) {
            if (field == value) return
            Log.d(TAG, "mVideoPath changed: '$field' -> '$value', surfaceReady=$isSurfaceReady")
            field = value
            if (value.isEmpty()) {
                stopAndRelease()
            } else if (isSurfaceReady) {
                restartCodec()
            }
        }

    /** 是否循环播放 */
    var isLooping: Boolean = true

    /** 是否静音（Codec 方案默认仅解码视频轨道，此属性保留用于扩展） */
    var isMute: Boolean = true
        set(value) { field = value }

    // ── 回调 ──────────────────────────────────────────────────

    var onPreparedListener: (() -> Unit)? = null
    var onCompletionListener: (() -> Unit)? = null
    var onErrorListener: ((msg: String) -> Unit)? = null

    /** 外部渲染 Surface。设置后视频输出到此 Surface 而非 TextureView，并自动重启解码器 */
    var outputSurface: Surface? = null
        set(value) {
            if (field == value) return
            field = value
            if (value != null && isSurfaceReady && videoPath.isNotEmpty()) {
                restartCodec()
            }
        }

    // ── 内部状态 ──────────────────────────────────────────────

    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    @Volatile private var decodeThread: Thread? = null
    private var decodeSurface: Surface? = null

    private var isSurfaceReady = false
    private val isDecoding = AtomicBoolean(false)
    /** 外部可见性驱动：true = 应播放，false = 应暂停 */
    private val shouldBePlaying = AtomicBoolean(false)

    private var videoWidth = 0
    private var videoHeight = 0

    // ── 初始化 ────────────────────────────────────────────────
    init {
        surfaceTextureListener = this
        isOpaque = false

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.LoopVideoView, defStyleAttr) {
                val path = getString(R.styleable.LoopVideoView_loopVideoPath)
                if (!path.isNullOrEmpty()) {
                    videoPath = path
                }
                isLooping = getBoolean(R.styleable.LoopVideoView_loopVideoLooping, true)
                isMute = getBoolean(R.styleable.LoopVideoView_loopVideoMute, true)
            }
        }
    }

    // ── SurfaceTextureListener ────────────────────────────────

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceTextureAvailable: ${width}x$height")
        decodeSurface = Surface(surface)
        isSurfaceReady = true
        if (videoPath.isNotEmpty()) {
            restartCodec()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceTextureSizeChanged: ${width}x$height")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.d(TAG, "onSurfaceTextureDestroyed")
        isSurfaceReady = false
        stopCodec()
        decodeSurface?.release()
        decodeSurface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    // ── 可见性感知 ────────────────────────────────────────────

    override fun onWindowVisibilityChanged(visibility: Int) {
        Log.d(TAG, "onWindowVisibilityChanged: ${visibilityName(visibility)}")
        super.onWindowVisibilityChanged(visibility)
        handleVisibility(visibility)
    }

    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        Log.d(TAG, "onVisibilityChanged: ${visibilityName(visibility)}")
        super.onVisibilityChanged(changedView, visibility)
        handleVisibility(visibility)
    }

    private fun handleVisibility(visibility: Int) {
        when (visibility) {
            VISIBLE -> {
                Log.d(TAG, "handleVisibility: VISIBLE -> request play")
                if (videoPath.isNotEmpty()) {
                    shouldBePlaying.set(true)
                    ensureDecoding()
                }
            }
            GONE, INVISIBLE -> {
                Log.d(TAG, "handleVisibility: ${visibilityName(visibility)} -> pause")
                shouldBePlaying.set(false)
                // 不解码，但保持 codec 存活（下次 VISIBLE 时 flush 续播）
            }
        }
    }

    // ── Codec 控制 ────────────────────────────────────────────

    private fun restartCodec() {
        stopCodec()
        shouldBePlaying.set(isShown)
        if (shouldBePlaying.get()) {
            startCodec()
        }
    }

    private fun ensureDecoding() {
        if (isDecoding.get()) return
        if (!isSurfaceReady || videoPath.isEmpty()) return
        startCodec()
    }

    private fun startCodec() {
        if (isDecoding.get()) return
        if (videoPath.isEmpty() || !isSurfaceReady) return

        Log.d(TAG, "startCodec: path='$videoPath'")

        try {
            // ── 1. 创建 Extractor 并选视频轨道 ──
            val ex = MediaExtractor().apply {
                setDataSource(context, Uri.parse(videoPath), emptyMap())
            }

            val videoTrackIndex = findVideoTrack(ex)
            if (videoTrackIndex < 0) {
                ex.release()
                onErrorListener?.invoke("No video track found")
                return
            }

            val format = ex.getTrackFormat(videoTrackIndex)
            videoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
            videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
            Log.d(TAG, "startCodec: video track found, ${videoWidth}x${videoHeight}, mime=${format.getString(MediaFormat.KEY_MIME)}")

            // 设置 Surface 缓冲区大小为视频分辨率，避免拉伸/黑边
            surfaceTexture?.setDefaultBufferSize(videoWidth, videoHeight)

            ex.selectTrack(videoTrackIndex)

            // ── 2. 创建解码器 ──
            val mime = format.getString(MediaFormat.KEY_MIME) ?: "video/avc"
            val cd = MediaCodec.createDecoderByType(mime)
            cd.configure(format, outputSurface ?: decodeSurface, null, 0)
            cd.start()

            extractor = ex
            codec = cd
            isDecoding.set(true)

            onPreparedListener?.invoke()

            // ── 3. 启动解码线程 ──
            decodeThread = Thread({
                decodeLoop(ex, cd, videoTrackIndex)
            }, "CodecLoopVideo-decoder").apply { start() }

        } catch (e: Exception) {
            Log.e(TAG, "startCodec failed", e)
            onErrorListener?.invoke(e.message ?: "Codec init failed")
            stopAndRelease()
        }
    }

    private fun stopCodec() {
        // 1. 发停止信号 → 解码线程退出 while 循环
        isDecoding.set(false)

        // 2. 打断线程（中断可能正在 sleep/dequeue 的阻塞调用）
        val thread = decodeThread
        decodeThread = null
        thread?.interrupt()

        // 3. 等待线程退出后，再安全地 stop codec
        //    （避免 codec 进入 Stopped 后线程仍在 releaseOutputBuffer → IllegalStateException）
        try { thread?.join(500) } catch (_: InterruptedException) {}

        // 4. 线程已退出，安全停止
        codec?.stop()

        extractor?.release()
        codec?.release()
        extractor = null
        codec = null
        Log.d(TAG, "stopCodec: released")
    }

    private fun stopAndRelease() {
        stopCodec()
        shouldBePlaying.set(false)
        Log.d(TAG, "stopAndRelease")
    }

    // ── 解码循环（后台线程） ──────────────────────────────────

    private fun decodeLoop(extractor: MediaExtractor, codec: MediaCodec, trackIndex: Int) {
        val bufferInfo = MediaCodec.BufferInfo()
        var inputEos = false
        var baseTimeNs = 0L
        var basePtsUs = 0L
        var isFirstFrame = true
        var wasPaused = false

        Log.d(TAG, "decodeLoop: started (PTS-based timing)")

        try {
        while (isDecoding.get()) {
            // ── 暂停控制 ──
            if (!shouldBePlaying.get()) {
                wasPaused = true
                try { Thread.sleep(50) } catch (_: InterruptedException) { break }
                continue
            }
            // 暂停→恢复时重置 PTS 基准，避免追赶式快放
            if (wasPaused) {
                isFirstFrame = true
                wasPaused = false
            }

            // ── 喂输入 ──
            if (!inputEos) {
                val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {
                    val buf = codec.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(buf, 0)
                    if (sampleSize < 0) {
                        // 文件读完
                        if (isLooping) {
                            Log.d(TAG, "decodeLoop: input EOS, seeking to start for loop")
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            codec.flush()
                            inputEos = false
                            isFirstFrame = true  // 重置 PTS 基准，否则第二圈会加速追赶
                            // flush 后旧 inputIndex 已失效，跳过本次循环让下一轮重新 dequeue
                            continue
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEos = true
                        }
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            // ── 取输出 ──
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    Log.d(TAG, "decodeLoop: output format changed")
                }
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // 没有输出，继续循环
                }
                outputIndex >= 0 -> {
                    val doRender = bufferInfo.size > 0
                    codec.releaseOutputBuffer(outputIndex, doRender)

                    // ── PTS 时序：与 MediaPlayer 一致的帧率 ──
                    if (doRender) {
                        val ptsUs = bufferInfo.presentationTimeUs
                        if (isFirstFrame) {
                            baseTimeNs = System.nanoTime()
                            basePtsUs = ptsUs
                            isFirstFrame = false
                        } else {
                            val targetTimeNs = baseTimeNs + (ptsUs - basePtsUs) * 1000
                            val now = System.nanoTime()
                            if (now < targetTimeNs) {
                                val sleepNs = targetTimeNs - now
                                try {
                                    Thread.sleep(sleepNs / 1_000_000, (sleepNs % 1_000_000).toInt())
                                } catch (_: InterruptedException) { break }
                            }
                        }
                    }

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        Log.d(TAG, "decodeLoop: output EOS")
                        if (isLooping && isDecoding.get()) {
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            codec.flush()
                            inputEos = false
                            isFirstFrame = true
                        } else {
                            break
                        }
                    }
                }
            }
        }
        } catch (e: IllegalStateException) {
            Log.d(TAG, "decodeLoop: codec stopped externally, exiting")
        }

        Log.d(TAG, "decodeLoop: exited")
    }

    // ── 工具 ──────────────────────────────────────────────────

    /** 在 Extractor 中查找第一个视频轨道 */
    private fun findVideoTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) return i
        }
        return -1
    }

    // ── 生命周期 ──────────────────────────────────────────────

    override fun onDetachedFromWindow() {
        Log.d(TAG, "onDetachedFromWindow")
        super.onDetachedFromWindow()
        stopAndRelease()
    }
}
