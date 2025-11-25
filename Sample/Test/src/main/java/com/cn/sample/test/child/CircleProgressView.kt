package com.cn.sample.test.child

/**
 * @Author: CuiNing
 * @Time: 2025/8/19 9:16
 * @Description:
 */
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.graphics.createBitmap
import com.cn.sample.test.R

class CircleProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 绘制属性
    private var progressColor = Color.BLUE
    private var bgColor = Color.LTGRAY
    private var progressWidth = 10f
    private var startAngle = -90f  // 默认从顶部开始[1,3](@ref)

    // 绘制工具
    private val bgPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = progressWidth
            color = bgColor
            strokeCap = Paint.Cap.ROUND
        }
    }
    private val progressPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = progressWidth
            color = progressColor
            strokeCap = Paint.Cap.ROUND
        }
    }
    private val iconPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = null
        }
    }

    // 状态
    // 图标内边距（图标与进度环的间距）
    private var iconPadding = 1f
    private var progress = 0f
    private val ovalRect = RectF()
    private var centerBitmap: Bitmap? = null
    private var croppedBitmap: Bitmap? = null // 缓存裁剪后的圆形图标
    private val iconRect = RectF()

    init {
        // 读取自定义属性[1,8](@ref)
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.CircleProgressView).apply {
                progressColor = getColor(R.styleable.CircleProgressView_progressColor, Color.BLUE)
                bgColor = getColor(R.styleable.CircleProgressView_bgColor, Color.TRANSPARENT)
                progressWidth = getDimension(R.styleable.CircleProgressView_progressWidth, progressWidth)
                startAngle = getFloat(R.styleable.CircleProgressView_startAngle, startAngle)
                iconPadding = getDimension(R.styleable.CircleProgressView_iconPadding, iconPadding)
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. 计算绘制区域（考虑padding）[1](@ref)
        val minSize = (width.coerceAtMost(height) - progressWidth).toFloat()
        ovalRect.set(
            (width - minSize) / 2 + paddingStart,
            (height - minSize) / 2 + paddingTop,
            (width + minSize) / 2 - paddingEnd,
            (height + minSize) / 2 - paddingBottom
        )

        // 2. 绘制背景圆环[3](@ref)
        canvas.drawOval(ovalRect, bgPaint)

        // 3. 绘制进度圆弧（从指定角度开始）[1,3](@ref)
        canvas.drawArc(ovalRect, startAngle, 360 * progress, false, progressPaint)

        // 4. 绘制中心图标[4](@ref)
        croppedBitmap?.let {
            updateIconRect()
            canvas.drawBitmap(it, null, iconRect, iconPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 保证View为正方形[1](@ref)
        val minSize = (progressWidth * 4).toInt() + paddingLeft + paddingRight
        val width = resolveSize(minSize, widthMeasureSpec)
        val height = resolveSize(minSize, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerBitmap?.let { cropToCircle(it) }
    }

    // 公开方法
    fun setProgress(value: Float, animate: Boolean = true) {
        val newProgress = value.coerceIn(0f, 1f)
        if (animate) {
            ValueAnimator.ofFloat(progress, newProgress).apply {
                duration = 800
                interpolator = AccelerateInterpolator()
                addUpdateListener { anim ->
                    progress = anim.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            progress = newProgress
            invalidate()
        }
    }

    // 动态设置图标间距
    fun setIconPadding(padding: Float) {
        iconPadding = padding
        updateIconRect()
        invalidate()
    }

    fun setCenterIcon(bitmap: Bitmap) {
        centerBitmap = bitmap
        cropToCircle(bitmap) // 预处理为圆形
        updateIconRect()
        invalidate()
        invalidate()
    }

    // 核心优化1：将图标裁剪为圆形
    private fun cropToCircle(source: Bitmap) {
        val size = source.width.coerceAtMost(source.height)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        // 绘制圆形裁剪区域
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        croppedBitmap = output
    }

    // 核心优化2：动态计算图标绘制区域（位于进度环内部）
    private fun updateIconRect() {
        val progressRadius = (ovalRect.width() - progressWidth) / 2f
        val iconRadius = progressRadius - iconPadding
        val centerX = width / 2f
        val centerY = height / 2f
        iconRect.set(
            centerX - iconRadius,
            centerY - iconRadius,
            centerX + iconRadius,
            centerY + iconRadius
        )
    }

}