package com.cn.board.meet.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.toColorInt
import kotlin.math.min

/**
 * @author: cn
 * @time: 2026/4/29 10:23
 * @history
 * @description:
 */

class ConcentricRoundedRectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    // 外矩形
    private val outerRect = RectF()
    private val outerPath = Path()

    // 内矩形
    private val innerRect = RectF()
    private val innerPath = Path()

    // 渐变路径
    private val gradientPath = Path()

    // 绘制相关
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gradientShader: Shader? = null

    // 可配置属性
    var outerRadius: Float = 50f
        set(value) {
            field = value
            invalidate()
        }

    var innerRadius: Float = 25f
        set(value) {
            field = value
            invalidate()
        }

    var gap: Float = 40f
        set(value) {
            field = value
            invalidate()
        }

    var startColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            updateGradientShader()
        }

    var endColor: Int = "#80FFFFFF".toColorInt()
        set(value) {
            field = value
            updateGradientShader()
        }

    init {
        // 从XML属性读取配置
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.ConcentricRoundedRectView) {
                outerRadius = getDimension(
                    R.styleable.ConcentricRoundedRectView_outerRadius,
                    50f
                )
                innerRadius = getDimension(
                    R.styleable.ConcentricRoundedRectView_innerRadius,
                    25f
                )
                gap = getDimension(
                    R.styleable.ConcentricRoundedRectView_gap,
                    40f
                )
                startColor = getColor(
                    R.styleable.ConcentricRoundedRectView_startColor,
                    startColor
                )
                endColor = getColor(
                    R.styleable.ConcentricRoundedRectView_endColor,
                    endColor
                )
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val padding = 20f
        val width = w - 2 * padding
        val height = h - 2 * padding

        // 计算外矩形
        outerRect.set(padding, padding, padding + width, padding + height)

        // 计算内矩形（等比缩小）
        val innerPadding = gap
        innerRect.set(
            outerRect.left + innerPadding,
            outerRect.top + innerPadding,
            outerRect.right - innerPadding,
            outerRect.bottom - innerPadding
        )

        // 创建路径
        updatePaths()
        updateGradientShader()
    }

    private fun updatePaths() {
        // 重置路径
        outerPath.reset()
        innerPath.reset()
        gradientPath.reset()

        // 添加外矩形圆角路径
        outerPath.addRoundRect(
            outerRect,
            floatArrayOf(
                outerRadius, outerRadius,
                outerRadius, outerRadius,
                outerRadius, outerRadius,
                outerRadius, outerRadius
            ),
            Path.Direction.CW
        )

        // 添加内矩形圆角路径
        innerPath.addRoundRect(
            innerRect,
            floatArrayOf(
                innerRadius, innerRadius,
                innerRadius, innerRadius,
                innerRadius, innerRadius,
                innerRadius, innerRadius
            ),
            Path.Direction.CW
        )

        // 创建渐变路径（外矩形减去内矩形）
        gradientPath.op(outerPath, innerPath, Path.Op.DIFFERENCE)
    }

    private fun updateGradientShader() {
        // 创建从外边界向内边界的径向渐变
        val centerX = outerRect.centerX()
        val centerY = outerRect.centerY()
        val radius = min(outerRect.width(), outerRect.height()) / 2
        if (radius != 0f) {
            Log.d(ConcentricRoundedRectView::class.simpleName, "updateGradientShader: $radius")
            gradientShader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(startColor, startColor, startColor, startColor, endColor), floatArrayOf(0f, 0.1f, 0.2f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )

            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (outerRect.isEmpty || innerRect.isEmpty) return

        // 设置绘制属性
        paint.shader = gradientShader
        paint.style = Paint.Style.FILL

        // 绘制渐变区域
        canvas.drawPath(gradientPath, paint)

        // 可选：绘制边框以便调试
        drawDebugBorders(canvas)
    }

    private fun drawDebugBorders(canvas: Canvas) {
        val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = "#80FFFFFF".toColorInt()
        }

        // 绘制外矩形边框
        debugPaint.color = "#80FFFFFF".toColorInt()
        canvas.drawPath(outerPath, debugPaint)

        // 绘制内矩形边框
        debugPaint.color = Color.TRANSPARENT
        canvas.drawPath(innerPath, debugPaint)
    }

    /**
     * 设置渐变方向
     * @param isOuterToInner true: 外到内渐变, false: 内到外渐变
     */
    fun setGradientDirection(isOuterToInner: Boolean) {
        if (isOuterToInner) {
            startColor = "#80FFFFFF".toColorInt()
            endColor = Color.TRANSPARENT
        } else {
            startColor = Color.TRANSPARENT
            endColor = "#80FFFFFF".toColorInt()
        }
        updateGradientShader()
    }

    /**
     * 设置自定义圆角半径（每个角可以不同）
     */
    fun setCustomCornerRadii(
        outerRadii: FloatArray,
        innerRadii: FloatArray,
    ) {
        require(outerRadii.size == 8 && innerRadii.size == 8) {
            "Radii arrays must have 8 elements"
        }

        outerPath.reset()
        innerPath.reset()

        outerPath.addRoundRect(outerRect, outerRadii, Path.Direction.CW)
        innerPath.addRoundRect(innerRect, innerRadii, Path.Direction.CW)

        gradientPath.reset()
        gradientPath.op(outerPath, innerPath, Path.Op.DIFFERENCE)

        invalidate()
    }
}