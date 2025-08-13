package com.cn.sample.test

/**
 * @Author: CuiNing
 * @Time: 2025/8/12 16:34
 * @Description:
 */
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class HeartBeatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 画笔配置
    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
        maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#B71C1C")
        maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.OUTER)
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFCDD2")
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }

    // 动画参数
    private var scaleFactor = 1.0f
    private val animator = ValueAnimator.ofFloat(0.9f, 1.1f).apply {
        duration = 800
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            scaleFactor = animatedValue as Float
            invalidate()
        }
    }

    // 爱心路径
    private val heartPath = Path()
    private val shadowPath = Path()
    private val highlightPath = Path()

    // 光效渐变
    private lateinit var radialGradient: RadialGradient
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        animator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateHeartPath(w, h)
        radialGradient = RadialGradient(
            w / 2f, h / 2f, w / 3f,
            intArrayOf(Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0f, 0.8f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = radialGradient
    }

    private fun updateHeartPath(width: Int, height: Int) {
        val centerX = width / 2f
        val centerY = height / 2f
        val size = minOf(width, height) * 0.4f

        // 主爱心路径
        heartPath.reset()
        heartPath.moveTo(centerX, centerY + size / 4)
        heartPath.cubicTo(
            centerX - size, centerY - size / 2,
            centerX - size / 3, centerY - size,
            centerX, centerY - size / 3
        )
        heartPath.cubicTo(
            centerX + size / 3, centerY - size,
            centerX + size, centerY - size / 2,
            centerX, centerY + size / 4
        )
        heartPath.close()

        // 阴影路径（偏移）
        shadowPath.set(heartPath)
        shadowPath.offset(size * 0.08f, size * 0.08f)

        // 高光路径（缩小）
        highlightPath.set(heartPath)
        highlightPath.transform(Matrix().apply {
            setScale(0.7f, 0.7f, centerX, centerY)
        })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 应用跳动动画
        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)

        // 绘制阴影
        canvas.drawPath(shadowPath, shadowPaint)

        // 绘制主爱心
        canvas.drawPath(heartPath, heartPaint)

        // 绘制立体高光
        canvas.drawPath(highlightPath, highlightPaint)

        // 绘制光效
        canvas.drawCircle(width / 2f, height / 2f, width / 3f, glowPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}