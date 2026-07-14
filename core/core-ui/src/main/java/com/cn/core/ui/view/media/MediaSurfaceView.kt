package com.cn.core.ui.view.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.withStyledAttributes
import com.cn.core.ui.R
import java.io.File
import java.io.IOException

/**
 * 媒体播放视图
 *
 * 继承自 SurfaceView，支持播放静态图片和 MP4 视频。
 *
 * ## 特性
 * - 支持静态图片显示（Bitmap、资源ID、Uri、文件路径）
 * - 支持 MP4 视频播放（本地文件、资源文件、网络视频）
 * - 支持图片缩放类型（居中、裁剪、适应）
 * - 支持视频循环播放
 * - 支持自动播放
 * - 支持播放控制（播放、暂停、停止）
 * - 支持 XML 属性配置
 *
 * ## XML 属性
 * | 属性名 | 类型 | 默认值 | 说明 |
 * |--------|------|--------|------|
 * | mediaScaleType | enum | fitCenter | 图片/视频缩放类型 |
 * | mediaLoop | boolean | false | 是否循环播放视频 |
 * | mediaMute | boolean | false | 是否静音 |
 * | mediaAutoPlay | boolean | false | 是否自动播放视频 |
 *
 * ## 使用示例
 * ```xml
 * <com.cn.core.ui.view.media.MediaSurfaceView
 *     android:layout_width="200dp"
 *     android:layout_height="200dp"
 *     app:mediaScaleType="centerCrop"
 *     app:mediaLoop="true"
 *     app:mediaMute="true"
 *     app:mediaAutoPlay="true" />
 * ```
 *
 * ## 代码示例
 * ```kotlin
 * // 显示静态图片
 * mediaSurfaceView.setImage(bitmap)
 * mediaSurfaceView.setImage(R.drawable.my_image)
 * mediaSurfaceView.setImage(uri)
 *
 * // 播放视频
 * mediaSurfaceView.setVideo(R.raw.my_video)
 * mediaSurfaceView.setVideo(uri)
 * mediaSurfaceView.setVideo("https://example.com/video.mp4")
 *
 * // 播放控制
 * mediaSurfaceView.play()
 * mediaSurfaceView.pause()
 * mediaSurfaceView.stop()
 *
 * // 设置监听
 * mediaSurfaceView.setOnPreparedListener { }
 * mediaSurfaceView.setOnCompletionListener { }
 * mediaSurfaceView.setOnErrorListener { }
 * ```
 *
 * @constructor 创建媒体播放视图实例
 * @param context 上下文
 * @param attrs XML 属性集
 * @param defStyleAttr 默认样式属性
 */
class MediaSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    companion object {
        /** 缩放类型：居中显示，不缩放 */
        const val SCALE_TYPE_CENTER = 0
        /** 缩放类型：居中显示，等比缩放适应视图 */
        const val SCALE_TYPE_FIT_CENTER = 1
        /** 缩放类型：居中显示，等比缩放填充视图（可能裁剪） */
        const val SCALE_TYPE_CENTER_CROP = 2
        /** 缩放类型：拉伸填充视图（可能变形） */
        const val SCALE_TYPE_FIT_XY = 3
    }

    /** 图片画笔 */
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** 缩放类型 */
    var scaleType: Int = SCALE_TYPE_FIT_CENTER
        set(value) {
            field = value
            invalidateSurface()
        }

    /** 是否循环播放 */
    var isLooping: Boolean = false
        set(value) {
            field = value
            mediaPlayer?.isLooping = value
        }

    /** 是否静音 */
    var isMute: Boolean = false
        set(value) {
            field = value
            mediaPlayer?.setVolume(if (value) 0f else 1f, if (value) 0f else 1f)
        }

    /** 是否自动播放 */
    var autoPlay: Boolean = false
        set(value) {
            field = value
        }

    /** 当前显示的图片 */
    private var currentBitmap: Bitmap? = null

    /** 媒体播放器 */
    private var mediaPlayer: MediaPlayer? = null

    /** 是否已准备就绪 */
    private var isPrepared = false

    /** 是否在 Surface 创建后自动播放 */
    private var pendingPlay = false

    /** 待播放的视频路径 */
    private var pendingVideoPath: String? = null

    /** 绘制矩阵 */
    private val drawMatrix = Matrix()

    /** 绘制区域 */
    private val drawRect = RectF()

    /** 准备就绪监听器 */
    private var onPreparedListener: (() -> Unit)? = null

    /** 播放完成监听器 */
    private var onCompletionListener: (() -> Unit)? = null

    /** 错误监听器 */
    private var onErrorListener: ((what: Int, extra: Int) -> Unit)? = null

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.MediaSurfaceView, defStyleAttr) {
                scaleType = getInt(R.styleable.MediaSurfaceView_mediaScaleType, SCALE_TYPE_FIT_CENTER)
                isLooping = getBoolean(R.styleable.MediaSurfaceView_mediaLoop, false)
                isMute = getBoolean(R.styleable.MediaSurfaceView_mediaMute, false)
                autoPlay = getBoolean(R.styleable.MediaSurfaceView_mediaAutoPlay, false)
            }
        }

        holder.addCallback(this)
    }

    /**
     * Surface 创建回调
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        pendingVideoPath?.let { path ->
            pendingVideoPath = null
            prepareVideo(path)
        }

        currentBitmap?.let {
            drawBitmap(it)
        }

        if (pendingPlay) {
            pendingPlay = false
            play()
        }
    }

    /**
     * Surface 变化回调
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        currentBitmap?.let {
            drawBitmap(it)
        }
    }

    /**
     * Surface 销毁回调
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    /**
     * 设置静态图片
     *
     * @param bitmap 图片 Bitmap
     */
    fun setImage(bitmap: Bitmap) {
        releaseMediaPlayer()
        currentBitmap = bitmap
        invalidateSurface()
    }

    /**
     * 设置静态图片
     *
     * @param resId 图片资源 ID
     */
    fun setImage(resId: Int) {
        val bitmap = android.graphics.BitmapFactory.decodeResource(resources, resId)
        bitmap?.let { setImage(it) }
    }

    /**
     * 设置静态图片
     *
     * @param uri 图片 Uri
     */
    fun setImage(uri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.let { setImage(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置静态图片
     *
     * @param path 图片路径（本地文件路径）
     */
    fun setImage(path: String) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(path)
            bitmap?.let { setImage(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置视频
     *
     * @param resId 视频资源 ID
     */
    fun setVideo(resId: Int) {
        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
        setVideo(uri)
    }

    /**
     * 设置视频
     *
     * @param uri 视频 Uri
     */
    fun setVideo(uri: Uri) {
        setVideo(uri.toString())
    }

    /**
     * 设置视频
     *
     * @param path 视频路径（本地路径或网络 URL）。
     *             设为空串则停止并释放；设为非空则停止旧视频并从开头重新播放。
     */
    fun setVideo(path: String) {
        currentBitmap = null
        releaseMediaPlayer()
        pendingVideoPath = null

        if (path.isEmpty()) return

        if (holder.surface.isValid) {
            prepareVideo(path)
        } else {
            pendingVideoPath = path
        }
    }

    /**
     * 准备视频播放
     */
    private fun prepareVideo(path: String) {
        try {
            val mp = MediaPlayer()
            mp.setDataSource(context, Uri.parse(path))
            mp.setSurface(holder.surface)
            mp.isLooping = isLooping
            mp.setVolume(if (isMute) 0f else 1f, if (isMute) 0f else 1f)

            mp.setOnPreparedListener {
                isPrepared = true
                onPreparedListener?.invoke()
                if (autoPlay) {
                    mp.start()
                }
            }

            mp.setOnCompletionListener {
                onCompletionListener?.invoke()
            }

            mp.setOnErrorListener { _, what, extra ->
                onErrorListener?.invoke(what, extra)
                true
            }

            mp.prepareAsync()
            mediaPlayer = mp
        } catch (e: IOException) {
            e.printStackTrace()
            onErrorListener?.invoke(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
        }
    }

    /**
     * 开始播放
     */
    fun play() {
        mediaPlayer?.let { mp ->
            if (isPrepared) {
                mp.start()
            } else {
                pendingPlay = true
            }
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        mediaPlayer?.pause()
    }

    /**
     * 停止播放
     */
    fun stop() {
        mediaPlayer?.stop()
        isPrepared = false
    }

    /**
     * 获取当前播放位置
     *
     * @return 当前位置（毫秒）
     */
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    /**
     * 获取视频总时长
     *
     * @return 总时长（毫秒）
     */
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    /**
     * 跳转到指定位置
     *
     * @param msec 目标位置（毫秒）
     */
    fun seekTo(msec: Int) {
        mediaPlayer?.seekTo(msec)
    }

    /**
     * 是否正在播放
     *
     * @return 是否正在播放
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    /**
     * 设置准备就绪监听器
     */
    fun setOnPreparedListener(listener: () -> Unit) {
        onPreparedListener = listener
    }

    /**
     * 设置播放完成监听器
     */
    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }

    /**
     * 设置错误监听器
     */
    fun setOnErrorListener(listener: (what: Int, extra: Int) -> Unit) {
        onErrorListener = listener
    }

    /**
     * 刷新 Surface
     */
    private fun invalidateSurface() {
        if (!holder.surface.isValid) return

        currentBitmap?.let { bitmap ->
            drawBitmap(bitmap)
        }
    }

    /**
     * 绘制图片
     */
    private fun drawBitmap(bitmap: Bitmap) {
        if (!holder.surface.isValid) return

        val canvas = holder.lockCanvas() ?: return

        try {
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            drawMatrix.reset()

            when (scaleType) {
                SCALE_TYPE_CENTER -> {
                    val left = (viewWidth - bitmapWidth) / 2f
                    val top = (viewHeight - bitmapHeight) / 2f
                    drawMatrix.setTranslate(left, top)
                }
                SCALE_TYPE_FIT_CENTER -> {
                    val scale = minOf(viewWidth / bitmapWidth, viewHeight / bitmapHeight)
                    val dx = (viewWidth - bitmapWidth * scale) / 2f
                    val dy = (viewHeight - bitmapHeight * scale) / 2f
                    drawMatrix.setScale(scale, scale)
                    drawMatrix.postTranslate(dx, dy)
                }
                SCALE_TYPE_CENTER_CROP -> {
                    val scale = maxOf(viewWidth / bitmapWidth, viewHeight / bitmapHeight)
                    val dx = (viewWidth - bitmapWidth * scale) / 2f
                    val dy = (viewHeight - bitmapHeight * scale) / 2f
                    drawMatrix.setScale(scale, scale)
                    drawMatrix.postTranslate(dx, dy)
                }
                SCALE_TYPE_FIT_XY -> {
                    drawRect.set(0f, 0f, viewWidth, viewHeight)
                    canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint)
                    return
                }
            }

            canvas.drawBitmap(bitmap, drawMatrix, bitmapPaint)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    /**
     * 释放媒体播放器
     */
    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        mediaPlayer = null
        isPrepared = false
    }

    /**
     * 视图分离回调
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releaseMediaPlayer()
        currentBitmap?.recycle()
        currentBitmap = null
    }
}
