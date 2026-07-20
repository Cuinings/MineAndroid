package com.cn.board.wallpaper

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import java.io.File

/**
 * 视频壁纸渲染器（MediaCodec 直渲）。
 *
 * 实现 [WallpaperRenderer]：把 MP4/WebM 视频用硬解码器解码后，**直接输出到壁纸 Surface**
 * （WindowManager.TYPE_WALLPAPER 层），由 SurfaceFlinger 合成到壁纸图层。
 * 全程零回读、零中间 Bitmap，性能最佳，与原 `BoardWallpaperService` 的直渲策略一致。
 *
 * 解码器输出 Surface = Engine 的壁纸 Surface，因此不需要 TextureView/ImageView。
 */
class VideoWallpaperRenderer(
    initialConfig: WallpaperConfig = WallpaperConfig.DEFAULT,
) : WallpaperRenderer {

    companion object {
        private const val TAG = "VideoWallpaperRenderer"
        private const val TIMEOUT_US = 10_000L
    }

    @Volatile private var config: WallpaperConfig = initialConfig

    @Volatile private var running = false
    private var isVisible = false

    private var decoder: MediaCodec? = null
    private var extractor: MediaExtractor? = null
    private var decoderThread: Thread? = null
    private var currentSurface: Surface? = null

    // ==================== WallpaperRenderer 生命周期 ====================

    override fun attach(holder: SurfaceHolder) {
        currentSurface = holder.surface
        if (isVisible) loadWallpaper()
    }

    override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val newSurface = holder.surface
        if (newSurface != currentSurface) {
            currentSurface = newSurface
            teardown()
            if (isVisible) setupAndPlay(newSurface)
        }
    }

    override fun onVisibilityChanged(visible: Boolean) {
        isVisible = visible
        if (visible) {
            if (decoder == null) loadWallpaper() else resume()
        } else {
            pause()
        }
    }

    override fun onConfigChanged(newConfig: WallpaperConfig) {
        config = newConfig
        val path = newConfig.videoPath
        if (path != null && File(path).exists()) {
            teardown()
            currentSurface?.let { setupAndPlay(it) }
        } else {
            Log.w(TAG, "config video path not set or file not found: $path")
            teardown()
        }
    }

    override fun onBatteryStateChanged(isCharging: Boolean, levelPercent: Int) {
        // 视频壁纸由解码器时序决定帧率，此处不做节流（避免卡顿）。
        // 若需极端省电，可在此暂停解码；当前保持播放体验。
    }

    override fun release() {
        isVisible = false
        teardown()
        currentSurface = null
    }

    override fun isRunning(): Boolean = running

    // ==================== 加载并播放视频 ====================

    private fun loadWallpaper() {
        val path = config.videoPath
        if (path != null && File(path).exists()) {
            teardown()
            currentSurface?.let { setupAndPlay(it) }
        } else {
            Log.w(TAG, "Wallpaper path not set or file not found: $path")
            teardown()
        }
    }

    private fun setupAndPlay(surface: Surface) {
        val path = config.videoPath
        if (path.isNullOrEmpty() || !File(path).exists()) {
            Log.w(TAG, "setupAndPlay: video path not set or file not found: $path")
            return
        }
        try {
            val ext = MediaExtractor()
            ext.setDataSource(path)
            var trackIndex = -1
            for (i in 0 until ext.trackCount) {
                val fmt = ext.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) { trackIndex = i; break }
            }
            if (trackIndex < 0) { ext.release(); Log.w(TAG, "no video track found"); return }
            ext.selectTrack(trackIndex)
            val format = ext.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val codec = MediaCodec.createDecoderByType(mime)
            // 关键：解码器输出 Surface = 壁纸 Surface（TYPE_WALLPAPER 层）
            codec.configure(format, surface, null, 0)
            extractor = ext
            decoder = codec
            startDecode()
        } catch (e: Exception) {
            Log.e(TAG, "setupAndPlay failed: $e")
            teardown()
        }
    }

    private fun startDecode() {
        val codec = decoder ?: return
        try { codec.start() } catch (e: Exception) {
            Log.e(TAG, "codec start failed: $e")
            teardown()
            return
        }
        running = true
        decoderThread = Thread(decodeRunnable, "WallpaperDecoder").also { it.start() }
    }

    private fun resume() {
        val codec = decoder ?: return
        try { codec.start() } catch (e: Exception) {
            Log.e(TAG, "resume failed, re-setup: $e")
            currentSurface?.let { setupAndPlay(it) }
            return
        }
        running = true
        decoderThread = Thread(decodeRunnable, "WallpaperDecoder").also { it.start() }
    }

    private fun pause() {
        running = false
        decoderThread?.let { try { it.join(1000) } catch (_: Exception) {} }
        decoderThread = null
        try { decoder?.stop() } catch (_: Exception) {}
        // 保留 decoder / extractor，便于 resume 直接 start()
    }

    private fun teardown() {
        running = false
        decoderThread?.let { try { it.join(1000) } catch (_: Exception) {} }
        decoderThread = null
        try { decoder?.stop() } catch (_: Exception) {}
        try { decoder?.release() } catch (_: Exception) {}
        decoder = null
        try { extractor?.release() } catch (_: Exception) {}
        extractor = null
    }

    private val decodeRunnable = object : Runnable {
        override fun run() {
            val codec = decoder ?: return
            val ext = extractor ?: return
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            try {
                while (running) {
                    if (!inputDone) {
                        val inIdx = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (inIdx >= 0) {
                            val buf = codec.getInputBuffer(inIdx)
                            if (buf != null) {
                                val size = ext.readSampleData(buf, 0)
                                if (size < 0) {
                                    codec.queueInputBuffer(
                                        inIdx, 0, 0, 0L,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                    )
                                    inputDone = true
                                } else {
                                    val pts = ext.sampleTime
                                    codec.queueInputBuffer(inIdx, 0, size, pts, 0)
                                    ext.advance()
                                }
                            } else {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, 0)
                            }
                        }
                    }
                    val outIdx = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                    when {
                        outIdx >= 0 -> {
                            try {
                                // true = 渲染到壁纸 Surface（TYPE_WALLPAPER 层）
                                codec.releaseOutputBuffer(outIdx, true)
                            } catch (e: Exception) {
                                Log.e(TAG, "releaseOutputBuffer failed: $e")
                            }
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                // 循环播放：flush + 回到起点
                                inputDone = false
                                try {
                                    codec.flush()
                                    ext.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                                } catch (_: Exception) {}
                            }
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* 格式变化，忽略 */ }
                        outIdx == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> { /* 已废弃，忽略 */ }
                        // INFO_TRY_AGAIN_LATER(-1) 等其它情况继续循环
                    }
                }
            } catch (e: Exception) {
                if (running) Log.e(TAG, "decode loop error: $e")
            }
        }
    }
}
