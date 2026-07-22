package com.cn.core.ui.view.media

import android.content.Context
import android.media.MediaPlayer
import android.util.AttributeSet
import android.util.Log
import android.widget.VideoView
import androidx.core.content.withStyledAttributes
import com.cn.core.ui.R

/**
 * 循环视频播放视图
 *
 * 基于 VideoView 封装，显示即播放、隐藏即暂停。
 *
 * ## 特性
 * - 固定视频路径（XML 或代码设置）
 * - 循环播放（默认开启）
 * - 可见性感知：VISIBLE → 播放，GONE/INVISIBLE → 暂停
 * - 生命周期安全：detach 时自动释放
 * - 静音播放（默认开启）
 *
 * ## 使用示例
 *
 * ### XML
 * ```xml
 * <com.cn.core.ui.view.media.LoopVideoView
 *     android:id="@+id/loopVideo"
 *     android:layout_width="match_parent"
 *     android:layout_height="200dp"
 *     app:loopVideoPath="/sdcard/Movies/intro.mp4" />
 * ```
 *
 * ### 代码
 * ```kotlin
 * loopVideo.mVideoPath = "/sdcard/Movies/intro.mp4"
 * loopVideo.visibility = View.VISIBLE  // 播放
 * loopVideo.visibility = View.GONE     // 暂停
 * ```
 *
 * @constructor 创建循环视频播放视图
 * @param context 上下文
 * @param attrs XML 属性集
 * @param defStyleAttr 默认样式属性
 *
 * @Author: WorkBuddy
 * @Time: 2026/7/3 14:46
 * @Description: VideoView 封装，显示即播放、隐藏即暂停
 */
class LoopVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : VideoView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "LoopVideoView"
        private fun visibilityName(v: Int): String = when (v) {
            VISIBLE -> "VISIBLE"
            INVISIBLE -> "INVISIBLE"
            GONE -> "GONE"
            else -> "UNKNOWN($v)"
        }
    }

    // ── 公开属性 ──────────────────────────────────────────────

    /**
     * 视频路径（本地绝对路径或 URI 字符串）。
     * - 设为非空：停止旧视频，从开头重新播放
     * - 设为空串：停止并释放
     */
    var mVideoPath: String = ""
        set(value) {
            if (field == value) return
            Log.d(TAG, "mVideoPath changed: '$field' -> '$value'")
            field = value
            if (value.isEmpty()) {
                Log.d(TAG, "mVideoPath: empty -> stopPlayback")
                stopPlayback()
                isPrepared = false
                cachedMp = null
                pendingStart = false
                reinstallCallbacks()
            } else {
                Log.d(TAG, "mVideoPath: set '$value', restarting")
                stopPlayback()
                isPrepared = false
                cachedMp = null
                reinstallCallbacks()
                setVideoPath(value)
                // 如果当前可见，mark 待启动
                if (isShown) pendingStart = true
            }
        }

    /** 是否循环播放。直接委托 VideoView 的 OnCompletionListener */
    var isLooping: Boolean = true

    /** 是否静音 */
    var isMute: Boolean = true
        set(value) {
            field = value
            cachedMp?.setVolume(if (value) 0f else 1f, if (value) 0f else 1f)
        }

    // ── 内部状态 ──────────────────────────────────────────────

    private var cachedMp: MediaPlayer? = null
    private var isPrepared = false
    private var pendingStart = false

    // ── 回调 ──────────────────────────────────────────────────

    var onVideoErrorListener: ((what: Int, extra: Int) -> Unit)? = null

    // ── 初始化 ────────────────────────────────────────────────

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.LoopVideoView, defStyleAttr) {
                val path = getString(R.styleable.LoopVideoView_loopVideoPath)
                if (!path.isNullOrEmpty()) {
                    mVideoPath = path
                }
                isLooping = getBoolean(R.styleable.LoopVideoView_loopVideoLooping, true)
                isMute = getBoolean(R.styleable.LoopVideoView_loopVideoMute, true)
            }
        }
        reinstallCallbacks()
    }

    /** VideoView 每次调用 stopPlayback 后会清除回调，需要重新注册 */
    private fun reinstallCallbacks() {
        setOnCompletionListener { mp ->
            Log.d(TAG, "onCompletion: restarting loop")
            mp.seekTo(0)
            mp.start()
        }

        setOnPreparedListener { mp ->
            isPrepared = true
            cachedMp = mp
            Log.d(TAG, "onPrepared: pendingStart=$pendingStart, isShown=$isShown")
            if (isMute) mp.setVolume(0f, 0f)
            if (pendingStart || isShown) {
                Log.d(TAG, "onPrepared: starting")
                mp.start()
                pendingStart = false
            }
        }

        setOnErrorListener { _, what, extra ->
            Log.e(TAG, "onError: what=$what, extra=$extra")
            isPrepared = false
            cachedMp = null
            onVideoErrorListener?.invoke(what, extra)
            true
        }
    }

    // ── 可见性感知 ────────────────────────────────────────────

    override fun onWindowVisibilityChanged(visibility: Int) {
        Log.d(TAG, "onWindowVisibilityChanged: ${visibilityName(visibility)}, isPrepared=$isPrepared")
        super.onWindowVisibilityChanged(visibility)
        handleVisibilityChange(visibility)
    }

    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        Log.d(TAG, "onVisibilityChanged: ${visibilityName(visibility)}, isPrepared=$isPrepared")
        super.onVisibilityChanged(changedView, visibility)
        handleVisibilityChange(visibility)
    }

    private fun handleVisibilityChange(visibility: Int) {
        when (visibility) {
            VISIBLE -> {
                if (isPrepared) {
                    Log.d(TAG, "handleVisibilityChange: VISIBLE -> resume")
                    start()
                } else {
                    Log.d(TAG, "handleVisibilityChange: VISIBLE but not prepared, pending")
                    pendingStart = true
                }
            }
            GONE, INVISIBLE -> {
                Log.d(TAG, "handleVisibilityChange: ${visibilityName(visibility)} -> pause")
                pause()
                pendingStart = false
            }
        }
    }

    // ── 生命周期 ──────────────────────────────────────────────

    override fun onDetachedFromWindow() {
        Log.d(TAG, "onDetachedFromWindow")
        super.onDetachedFromWindow()
        stopPlayback()
        isPrepared = false
        cachedMp = null
    }
}
