package com.cn.board.meet.home

/**
 * @author: cn
 * @time: 2026/4/29 11:01
 * @history
 * @description:
 * 自定义 View，实现延边内发光效果。
 * 绘制一个深灰色正方形，带有白色模糊内发光轮廓，背景为纯黑色。
 */
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

/**
 * 自定义 View，实现延边内发光效果，且内发光具有从边缘到中心的渐变透明。
 */
class InnerGlowSquareView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val squarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    private var squareSize = 300f
    private var glowRadius = 100f
    private var radialGradient: RadialGradient? = null

    private val squareRect = RectF()
    private val glowRect = RectF()

    init {
        // 设置图层类型为硬件加速，提高绘制性能
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateRectsAndGradient()
    }

    /**
     * 更新正方形区域和渐变 Shader。
     */
    private fun updateRectsAndGradient() {
        val centerX = width / 2f
        val centerY = height / 2f
        val halfSize = squareSize / 2f

        // 正方形区域
        squareRect.set(
            centerX - halfSize,
            centerY - halfSize,
            centerX + halfSize,
            centerY + halfSize
        )

        // 发光绘制区域（与正方形区域相同）
        glowRect.set(squareRect)

        // 创建径向渐变
        val gradientCenterX = squareRect.centerX()
        val gradientCenterY = squareRect.centerY()
        val gradientRadius = halfSize

        // 计算渐变位置
        val gradientStart = 1f - (glowRadius / gradientRadius)
        val positions = floatArrayOf(
            gradientStart.coerceAtLeast(0f),
            1f
        )

        radialGradient = RadialGradient(
            gradientCenterX, gradientCenterY,
            gradientRadius,
            intArrayOf(Color.WHITE, Color.TRANSPARENT),
            positions,
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = radialGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. 绘制纯黑色背景
        canvas.drawColor(Color.TRANSPARENT)

        // 2. 绘制深灰色正方形
        canvas.drawRect(squareRect, squarePaint)

        // 3. 绘制渐变内发光
        val saved = canvas.saveLayer(glowRect, null)
        canvas.drawRect(glowRect, glowPaint)
        canvas.restoreToCount(saved)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = width.coerceAtMost(height)
        setMeasuredDimension(size, size)
    }

    // ---------- 公共方法 ----------

    /**
     * 设置正方形边长。
     * @param size 边长（像素），建议大于 0
     */
    fun setSquareSize(size: Float) {
        squareSize = size.coerceAtLeast(0f)
        updateRectsAndGradient()
        invalidate()
    }

    /**
     * 设置发光渐变区域的宽度。
     * @param radius 从边缘向内延伸的发光区域宽度（像素），建议大于 0
     */
    fun setGlowRadius(radius: Float) {
        glowRadius = radius.coerceAtLeast(0f)
        updateRectsAndGradient()
        invalidate()
    }

    /**
     * 设置正方形颜色。
     * @param color 颜色值
     */
    fun setSquareColor(@ColorInt color: Int) {
        squarePaint.color = color
        invalidate()
    }

    /**
     * 设置内发光颜色（渐变起始颜色）。
     * @param color 边缘发光颜色，中心始终为透明
     */
    fun setGlowColor(@ColorInt color: Int) {
        val centerX = squareRect.centerX()
        val centerY = squareRect.centerY()
        val gradientRadius = squareSize / 2f

        // 计算渐变位置
        val gradientStart = 1f - (glowRadius / gradientRadius)
        val positions = floatArrayOf(
            gradientStart.coerceAtLeast(0f),
            1f
        )

        radialGradient = RadialGradient(
            centerX, centerY,
            gradientRadius,
            intArrayOf(color, Color.TRANSPARENT),
            positions,
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = radialGradient
        invalidate()
    }

    /**
     * 设置发光效果的透明度。
     * @param alpha 透明度，范围 0-255
     */
    fun setGlowAlpha(alpha: Int) {
        glowPaint.alpha = alpha.coerceIn(0, 255)
        invalidate()
    }

    /**
     * 获取当前正方形边长。
     */
    fun getSquareSize(): Float = squareSize

    /**
     * 获取当前发光渐变区域宽度。
     */
    fun getGlowRadius(): Float = glowRadius
}