package com.cn.core.ui.view

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * 首页视频播放视图（SurfaceView 方案，需 minSdk 29 以支持 SurfaceControlViewHost）。
 *
 * ## 为什么是 ABOVE_WINDOW
 * 本视图作为 [com.cn.core.ui.view.frosted.FrostedAnimatedGlowView]
 * （即 `main_activity_frag_app_manager_constraintLayout`）的子 View。
 *
 * [SurfaceView] 的画面是 **独立 SurfaceFlinger 图层**，不在窗口
 * 的 View 表面之内，只能整体在窗口内容「之下」(BELOW_WINDOW) 或「之上」(ABOVE_WINDOW)。
 * 卡片的「毛玻璃」（SYSTEM 跨窗口模糊）是 SurfaceFlinger 合成效果，会画一张模糊背景
 * 在**窗口内容**里。BELOW_WINDOW 的视频会被它盖住（模糊覆盖）；
 * 因此这里用 **[setZOrderOnTop]`(true)`** 把视频抬到 **窗口之上**
 * （框架原生、跨 relayout 持久，不会被 `SurfaceView.updateSurface()` 重置），
 * 它便位于窗口内容（含模糊背景）之上 → 不被模糊影响，保持清晰。
 *
 * 但 ABOVE_WINDOW 的视频会盖住窗口内容里的卡片文字/列表。所以 UI（标题/管理按钮/列表）
 * 由 `AppAggregatorFrag` 借助 [android.view.SurfaceControlViewHost] 放进独立的 Surface，
 * 再用 [SurfaceControl.Transaction.setRelativeLayer] 抬到视频之上 —— 三者同时满足：
 * 毛玻璃保留（模糊的是壁纸背景）、视频清晰、上层 UI 可见。
 *
 * ## 使用
 * ```kotlin
 * val video = binding.homeVideoSurfaceView   // HomeVideoSurfaceView
 * video.videoPath = "/storage/emulated/0/Download/wallpaper_dy_1.mp4"
 * // 生命周期
 * override fun onPause()  { video.pause() }
 * override fun onResume() { video.resume() }
 * ```
 *
 * @author cn
 */
class HomeVideoSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "HomeVideoSurface"
    }

    // ── 公开属性 ──────────────────────────────────────────────

    /** 视频路径，可为本地路径或 Uri 字符串。赋值后自动准备播放。 */
    var videoPath: String = "/storage/emulated/0/Download/wallpaper_dy_1.mp4"
        set(value) {
            if (field == value) return
            Log.d(TAG, "videoPath: '$field' -> '$value'")
            field = value
            if (value.isEmpty()) releasePlayer()
            else if (isSurfaceReady) startPlayback()
        }

    /** 是否循环播放。 */
    var isLooping: Boolean = true
        set(value) {
            field = value
            player?.isLooping = value
        }

    /** 是否静音。 */
    var isMute: Boolean = true
        set(value) {
            field = value
            applyVolume()
        }

    // ── 回调 ──────────────────────────────────────────────────
    var onPreparedListener: (() -> Unit)? = null
    var onErrorListener: ((msg: String) -> Unit)? = null

    // ── 内部状态 ──────────────────────────────────────────────
    private var player: MediaPlayer? = null
    private var isSurfaceReady = false

    init {
        // BELOW_WINDOW 时 SurfaceView 自身的 View 背景必须为透明，
        // 否则窗口内容里的黑色背景会盖住下方的视频图层。
        setBackgroundColor(Color.TRANSPARENT)
        // 关键：框架原生把视频 Surface 抬到窗口内容之上（ABOVE_WINDOW）。
        // 与手动 Transaction 不同，setZOrderOnTop 会被 SurfaceView.updateSurface() 每次
        // 主动保持，因此跨 relayout / 重绘持久生效，不会被重置回窗口之下 → 不被卡片
        // 跨窗口模糊（blurBehindRadius）采样，视频始终清晰。
        setZOrderOnTop(true)
        holder.addCallback(this)
    }

    // ── SurfaceHolder.Callback ────────────────────────────────

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated (zOrderOnTop=true)")
        isSurfaceReady = true
        if (videoPath.isNotEmpty()) startPlayback()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // 层级由 setZOrderOnTop(true) 持久保证，无需此处再 apply。
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed")
        isSurfaceReady = false
        releasePlayer()
    }

    // ── 播放控制 ──────────────────────────────────────────────

    private fun startPlayback() {
        if (!isSurfaceReady || videoPath.isEmpty()) return
        releasePlayer()
        try {
            player = MediaPlayer().apply {
                setDataSource(context, Uri.parse(videoPath))
                setDisplay(this@HomeVideoSurfaceView.holder)
                isLooping = this@HomeVideoSurfaceView.isLooping
                setOnPreparedListener {
                    applyVolume()
                    it.start()
                    onPreparedListener?.invoke()
                    Log.d(TAG, "prepared & started")
                }
                setOnErrorListener { _, what, extra ->
                    val msg = "MediaPlayer error what=$what extra=$extra"
                    Log.e(TAG, msg)
                    onErrorListener?.invoke(msg)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "startPlayback failed", e)
            onErrorListener?.invoke(e.message ?: "startPlayback failed")
            releasePlayer()
        }
    }

    private fun applyVolume() {
        val v = if (isMute) 0f else 1f
        try {
            player?.setVolume(v, v)
        } catch (e: Exception) {
            Log.w(TAG, "applyVolume: ${e.message}")
        }
    }

    /** 暂停播放（建议在宿主 onPause 调用）。 */
    fun pause() {
        try {
            player?.takeIf { it.isPlaying }?.pause()
        } catch (e: Exception) {
            Log.w(TAG, "pause: ${e.message}")
        }
    }

    /** 恢复播放（建议在宿主 onResume 调用）。 */
    fun resume() {
        try {
            player?.start()
        } catch (e: Exception) {
            Log.w(TAG, "resume: ${e.message}")
        }
    }

    private fun releasePlayer() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        player = null
    }

    // ── 生命周期 ──────────────────────────────────────────────

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releasePlayer()
    }
}
