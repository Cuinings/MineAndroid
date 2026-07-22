package com.cn.core.ui.view.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.Image
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 循环视频播放视图（SurfaceView + MediaCodec + ImageReader 合成方案）
 *
 * MediaCodec 输出到 ImageReader，在 decode 线程中通过 Canvas 手动合成：
 * 模糊背景 → 视频帧 → SurfaceView Surface。
 *
 * ## 特性
 * - 视频下方可叠加模糊背景（setBlurredBackground）
 * - PTS 时序播放，与 MediaPlayer 速度一致
 * - 可见性感知 + 路径变化重播 + 循环
 *
 * ## 使用示例
 * ```kotlin
 * view.setBlurredBackground(blurredWallpaper)
 * view.mVideoPath = "/sdcard/Movies/intro.mp4"
 * view.visibility = View.VISIBLE
 * ```
 */
class SurfaceLoopVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "SurfaceLoopVideo"
        private const val TIMEOUT_US = 10_000L
        private fun visibilityName(v: Int): String = when (v) {
            VISIBLE -> "VISIBLE"
            INVISIBLE -> "INVISIBLE"
            GONE -> "GONE"
            else -> "UNKNOWN($v)"
        }
    }

    // ── 公开属性 ──────────────────────────────────────────────

    var videoPath: String = ""
        set(value) {
            if (field == value) return
            Log.d(TAG, "mVideoPath: '$field' -> '$value'")
            field = value
            if (value.isEmpty()) stopAndRelease()
            else if (isSurfaceReady) restartCodec()
        }

    var isLooping: Boolean = true
    var isMute: Boolean = true

    // ── 回调 ──────────────────────────────────────────────────

    var onPreparedListener: (() -> Unit)? = null
    var onErrorListener: ((msg: String) -> Unit)? = null

    // ── 内部状态 ──────────────────────────────────────────────

    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var imageReader: ImageReader? = null
    private var decodeThread: Thread? = null
    private var videoWidth = 0
    private var videoHeight = 0

    private var isSurfaceReady = false
    private val isDecoding = AtomicBoolean(false)
    private val shouldBePlaying = AtomicBoolean(false)

    /** 模糊背景 Bitmap，绘制在视频帧下方 */
    private var blurredBg: Bitmap? = null
    /** Canvas 绘制区复用 */
    private val dstRect = Rect()

    // ── 公开方法 ──────────────────────────────────────────────

    fun setBlurredBackground(bitmap: Bitmap?) {
        blurredBg = bitmap
        Log.d(TAG, "setBlurredBackground: ${if (bitmap != null) "${bitmap.width}x${bitmap.height}" else "null"}")
    }

    // ── 初始化 ────────────────────────────────────────────────

    init {
        holder.addCallback(this)
        // 保证 SurfaceView 在 Window 层之上，下方 BlurView 可见
        setZOrderMediaOverlay(true)
    }

    // ── SurfaceHolder.Callback ────────────────────────────────

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated")
        isSurfaceReady = true
        if (videoPath.isNotEmpty()) restartCodec()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed")
        isSurfaceReady = false
        stopCodec()
    }

    // ── 可见性感知 ────────────────────────────────────────────

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        handleVisibility(visibility)
    }

    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        handleVisibility(visibility)
    }

    private fun handleVisibility(visibility: Int) {
        when (visibility) {
            VISIBLE -> {
                if (videoPath.isNotEmpty()) { shouldBePlaying.set(true); ensureDecoding() }
            }
            GONE, INVISIBLE -> shouldBePlaying.set(false)
        }
    }

    // ── Codec 控制 ────────────────────────────────────────────

    private fun restartCodec() {
        stopCodec()
        shouldBePlaying.set(isShown)
        if (shouldBePlaying.get()) startCodec()
    }

    private fun ensureDecoding() {
        if (isDecoding.get() || !isSurfaceReady || videoPath.isEmpty()) return
        startCodec()
    }

    private fun startCodec() {
        if (isDecoding.get() || videoPath.isEmpty() || !isSurfaceReady) return
        Log.d(TAG, "startCodec: '$videoPath'")

        try {
            val ex = MediaExtractor().apply { setDataSource(context, Uri.parse(videoPath), emptyMap()) }
            val vi = findVideoTrack(ex)
            if (vi < 0) { ex.release(); onErrorListener?.invoke("No video track"); return }

            val fmt = ex.getTrackFormat(vi)
            videoWidth = fmt.getInteger(MediaFormat.KEY_WIDTH)
            videoHeight = fmt.getInteger(MediaFormat.KEY_HEIGHT)
            Log.d(TAG, "startCodec: track found, ${videoWidth}x${videoHeight}")

            ex.selectTrack(vi)

            // ImageReader 接收解码输出，RGBA 格式方便 Canvas 绘制
            imageReader = ImageReader.newInstance(videoWidth, videoHeight, PixelFormat.RGBA_8888, 2)

            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: "video/avc"
            val cd = MediaCodec.createDecoderByType(mime)
            cd.configure(fmt, imageReader!!.surface, null, 0)
            cd.start()

            extractor = ex
            codec = cd
            isDecoding.set(true)
            onPreparedListener?.invoke()

            decodeThread = Thread({ decodeLoop(ex, cd, vi) }, "SurfaceLoopVideo-decoder").apply { start() }

        } catch (e: Exception) {
            Log.e(TAG, "startCodec failed", e)
            onErrorListener?.invoke(e.message ?: "Codec init failed")
            stopAndRelease()
        }
    }

    /** ImageReader 回调：帧可用时绘制到 SurfaceView */
    private var latestImage: Image? = null
    private val imageLock = Any()

    private fun onImageAvailable() {
        synchronized(imageLock) {
            latestImage?.close()
            latestImage = imageReader?.acquireLatestImage()
        }
    }

    private fun stopCodec() {
        isDecoding.set(false)
        codec?.stop()
        try { decodeThread?.join(200) } catch (_: InterruptedException) {}
        imageReader?.close()
        extractor?.release()
        codec?.release()
        extractor = null; codec = null; imageReader = null; decodeThread = null
        Log.d(TAG, "stopCodec: released")
    }

    private fun stopAndRelease() {
        stopCodec()
        shouldBePlaying.set(false)
    }

    // ── 解码循环（后台线程） ──────────────────────────────────

    private fun decodeLoop(extractor: MediaExtractor, codec: MediaCodec, trackIndex: Int) {
        val bufferInfo = MediaCodec.BufferInfo()
        var inputEos = false
        var baseTimeNs = 0L
        var basePtsUs = 0L
        var isFirstFrame = true
        var wasPaused = false

        Log.d(TAG, "decodeLoop: started")

        try {
        while (isDecoding.get()) {
            if (!shouldBePlaying.get()) { wasPaused = true; Thread.sleep(50); continue }
            if (wasPaused) { isFirstFrame = true; wasPaused = false }

            // 喂输入
            if (!inputEos) {
                val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {
                    val buf = codec.getInputBuffer(inputIndex) ?: continue
                    val sz = extractor.readSampleData(buf, 0)
                    if (sz < 0) {
                        if (isLooping) {
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            codec.flush(); inputEos = false; continue
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEos = true
                        }
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sz, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            // 取输出 → 渲染到 Surface
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {}
                outputIndex >= 0 -> {
                    // 渲染到 ImageReader Surface
                    codec.releaseOutputBuffer(outputIndex, true)

                    // 等 ImageReader 回调，然后 Canvas 合成到 SurfaceView
                    drawFrameToSurface()

                    // PTS 时序
                    if (bufferInfo.size > 0) {
                        val pts = bufferInfo.presentationTimeUs
                        if (isFirstFrame) { baseTimeNs = System.nanoTime(); basePtsUs = pts; isFirstFrame = false }
                        else {
                            val target = baseTimeNs + (pts - basePtsUs) * 1000
                            val now = System.nanoTime()
                            if (now < target) Thread.sleep((target - now) / 1_000_000, ((target - now) % 1_000_000).toInt())
                        }
                    }

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        if (isLooping && isDecoding.get()) {
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            codec.flush(); inputEos = false; isFirstFrame = true
                        } else break
                    }
                }
            }
        }
        } catch (e: Exception) {
            Log.d(TAG, "decodeLoop: $e")
        }
        Log.d(TAG, "decodeLoop: exited")
    }

    /** 从 ImageReader 同步取帧 → Canvas 合成(模糊背景 + 视频帧) → 绘制到 SurfaceView */
    private fun drawFrameToSurface() {
        val reader = imageReader ?: return
        // 同步轮询等待帧到达 ImageReader
        var img: Image? = null
        var retries = 20
        while (retries-- > 0 && img == null) {
            img = reader.acquireLatestImage()
            if (img == null) try { Thread.sleep(2) } catch (_: InterruptedException) { return }
        }
        if (img == null) return

        val canvas = holder.lockCanvas() ?: run { img.close(); return }
        try {
            // 1. 绘制模糊背景
            blurredBg?.let { bg ->
                dstRect.set(0, 0, canvas.width, canvas.height)
                canvas.drawBitmap(bg, null, dstRect, null)
            }

            // 2. 绘制视频帧（处理 stride 对齐）
            val plane = img.planes[0]
            val buf = plane.buffer
            val rowStride = plane.rowStride / 4  // RGBA → int 步长
            val pixelStride = plane.pixelStride / 4
            val pixels = IntArray(videoWidth * videoHeight)

            if (rowStride == videoWidth && pixelStride == 1) {
                // 无 padding，快速路径
                buf.asIntBuffer().get(pixels)
            } else {
                // 有 stride padding，逐行拷贝
                buf.rewind()
                for (row in 0 until videoHeight) {
                    buf.position(row * plane.rowStride)
                    buf.asIntBuffer().get(pixels, row * videoWidth, videoWidth)
                }
            }
            buf.rewind()

            val videoBmp = Bitmap.createBitmap(pixels, videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
            dstRect.set(0, 0, canvas.width, canvas.height)
            canvas.drawBitmap(videoBmp, null, dstRect, null)
            videoBmp.recycle()
        } finally {
            holder.unlockCanvasAndPost(canvas)
            img.close()
        }
    }

    // ── 工具 ──────────────────────────────────────────────────

    private fun findVideoTrack(ex: MediaExtractor): Int {
        for (i in 0 until ex.trackCount) {
            if (ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) return i
        }
        return -1
    }

    // ── 生命周期 ──────────────────────────────────────────────

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAndRelease()
    }
}
