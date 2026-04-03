package com.cn.core.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.cn.core.ui.R
import kotlin.math.max
import kotlin.math.min
import androidx.core.content.withStyledAttributes

/**
 * 内发光视图
 *
 * 在视图边缘内侧绘制发光效果，发光从边缘向内渐变。
 * 通过 LinearGradient 和 RadialGradient 实现四边和四角的发光效果。
 *
 * ## 特性
 * - 支持自定义发光颜色
 * - 支持自定义发光半径（默认 10dp）
 * - 支持圆角矩形
 * - 支持内描边
 * - 支持顶部内阴影效果
 * - 支持 XML 属性配置
 *
 * ## XML 属性
 * | 属性名 | 类型 | 默认值 | 说明 |
 * |--------|------|--------|------|
 * | glowColor | color | Color.MAGENTA | 发光颜色 |
 * | glowRadius | dimension | 10dp | 发光半径 |
 * | cornerRadius | dimension | 0 | 圆角半径 |
 * | strokeWidth | dimension | 1px | 描边宽度 |
 * | strokeColor | color | #26FFFFFF | 描边颜色 |
 * | innerShadowThickness | dimension | 0 | 内阴影厚度 |
 * | innerShadowColor | color | #40000000 | 内阴影颜色 |
 *
 * ## 使用示例
 * ```xml
 * <com.cn.core.ui.view.InnerGlowView
 *     android:layout_width="200dp"
 *     android:layout_height="200dp"
 *     app:glowColor="#FF00FF"
 *     app:glowRadius="10dp"
 *     app:cornerRadius="8dp"
 *     app:strokeWidth="1dp"
 *     app:strokeColor="#26FFFFFF"
 *     app:innerShadowThickness="20dp"
 *     app:innerShadowColor="#40000000" />
 * ```
 *
 * ## 代码示例
 * ```kotlin
 * val glowView = InnerGlowView(context).apply {
 *     setGlowColor(Color.parseColor("#FF00FF"))
 *     setGlowRadius(20f)
 *     setCornerRadius(16f)
 *     setStrokeWidth(2f)
 *     setStrokeColor(Color.WHITE)
 *     setInnerShadowThickness(30f)
 *     setInnerShadowColor(Color.parseColor("#80000000"))
 * }
 * ```
 *
 * ## 实现原理
 * 1. 使用 SOFTWARE 图层类型确保渐变正确渲染
 * 2. 四边使用 LinearGradient 从边缘向内渐变
 * 3. 四角使用 RadialGradient 实现圆形渐变
 * 4. 内描边通过在内部绘制描边路径实现
 * 5. 顶部内阴影使用 LinearGradient 从顶部向下渐变
 *
 * @constructor 创建内发光视图实例
 * @param context 上下文
 * @param attrs XML 属性集
 * @param defStyleAttr 默认样式属性
 *
 * @see View
 * @see LinearGradient
 * @see android.graphics.RadialGradient
 */
class InnerGlowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** 发光颜色 */
    private var glowColor: Int = Color.MAGENTA

    /** 发光半径（像素） */
    private var glowRadius: Float = 10f

    /** 圆角半径（像素） */
    private var cornerRadius: Float = 0f

    /** 描边宽度（像素） */
    private var strokeWidth: Float = 1f

    /** 描边颜色 */
    private var strokeColor: Int = Color.parseColor("#26FFFFFF")

    /** 内阴影厚度（像素），0 表示不显示内阴影 */
    private var innerShadowThickness: Float = 0f

    /** 内阴影颜色 */
    private var innerShadowColor: Int = Color.parseColor("#40000000")

    /** 发光绘制画笔 */
    private val glowPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 描边绘制画笔 */
    private val strokePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    /** 阴影绘制画笔 */
    private val shadowPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 绘制路径，用于复用减少对象创建 */
    private val path: Path = Path()

    /** 矩形区域，用于复用减少对象创建 */
    private val rectF: RectF = RectF()

    /**
     * 初始化视图属性
     *
     * 从 XML 属性中读取配置值，并设置 SOFTWARE 图层类型以支持渐变渲染。
     */
    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.InnerGlowView, defStyleAttr, 0) {
                glowColor = getColor(R.styleable.InnerGlowView_innerGlowColor, Color.MAGENTA)
                glowRadius = getDimension(R.styleable.InnerGlowView_innerGlowRadius, 10f * resources.displayMetrics.density)
                cornerRadius = getDimension(R.styleable.InnerGlowView_innerGlowCornerRadius, 0f)
                strokeWidth = getDimension(R.styleable.InnerGlowView_innerGlowStrokeWidth,
                    1f)
                strokeColor = getColor(R.styleable.InnerGlowView_innerGlowStrokeColor, Color.parseColor("#26FFFFFF"))
                innerShadowThickness = getDimension(R.styleable.InnerGlowView_innerGlowShadowThickness, 0f)
                innerShadowColor = getColor(R.styleable.InnerGlowView_innerGlowShadowColor, Color.parseColor("#40000000"))
            }
        }

        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * 设置发光颜色
     *
     * @param color 发光颜色
     */
    fun setGlowColor(color: Int) {
        this.glowColor = color
        invalidate()
    }

    /**
     * 设置发光半径
     *
     * @param radius 发光半径（像素）
     */
    fun setGlowRadius(radius: Float) {
        this.glowRadius = max(0f, radius)
        invalidate()
    }

    /**
     * 设置圆角半径
     *
     * @param radius 圆角半径（像素）
     */
    fun setCornerRadius(radius: Float) {
        this.cornerRadius = max(0f, radius)
        invalidate()
    }

    /**
     * 设置描边宽度
     *
     * @param width 描边宽度（像素）
     */
    fun setStrokeWidth(width: Float) {
        this.strokeWidth = max(0f, width)
        invalidate()
    }

    /**
     * 设置描边颜色
     *
     * @param color 描边颜色
     */
    fun setStrokeColor(color: Int) {
        this.strokeColor = color
        invalidate()
    }

    /**
     * 设置内阴影厚度
     *
     * @param thickness 内阴影厚度（像素）
     */
    fun setInnerShadowThickness(thickness: Float) {
        this.innerShadowThickness = max(0f, thickness)
        invalidate()
    }

    /**
     * 设置内阴影颜色
     *
     * @param color 内阴影颜色
     */
    fun setInnerShadowColor(color: Int) {
        this.innerShadowColor = color
        invalidate()
    }

    /**
     * 绘制视图内容
     *
     * 按顺序绘制：内发光 -> 顶部内阴影 -> 内描边
     *
     * @param canvas 画布
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) return

        val safeGlowRadius = min(glowRadius, min(width / 2f, height / 2f))
        if (safeGlowRadius <= 0f) return

        rectF.set(0f, 0f, width.toFloat(), height.toFloat())

        canvas.save()

        if (cornerRadius > 0f) {
            path.reset()
            path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.clipPath(path)
        }

        drawInnerGlow(canvas, rectF, safeGlowRadius)
        drawTopInnerShadow(canvas, rectF)
        drawInnerStroke(canvas, rectF)

        canvas.restore()
    }

    /**
     * 绘制内发光效果
     *
     * 分别绘制四边和四角的发光效果
     *
     * @param canvas 画布
     * @param rect 绘制区域
     * @param radius 发光半径
     */
    private fun drawInnerGlow(canvas: Canvas, rect: RectF, radius: Float) {
        drawTopGlow(canvas, rect, radius)
        drawBottomGlow(canvas, rect, radius)
        drawLeftGlow(canvas, rect, radius)
        drawRightGlow(canvas, rect, radius)
        drawCornerGlows(canvas, rect, radius)
    }

    /**
     * 绘制顶部边缘发光
     *
     * 使用 LinearGradient 从顶部边缘向下渐变
     *
     * @param canvas 画布
     * @param rect 绘制区域
     * @param radius 发光半径
     */
    private fun drawTopGlow(canvas: Canvas, rect: RectF, radius: Float) {
        val gradient = LinearGradient(
            rect.left, rect.top,
            rect.left, rect.top + radius,
            intArrayOf(glowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = gradient

        path.reset()
        path.addRect(rect.left, rect.top, rect.right, rect.top + radius, Path.Direction.CW)
        canvas.drawPath(path, glowPaint)
    }

    /**
     * 绘制底部边缘发光
     *
     * 使用 LinearGradient 从底部边缘向上渐变
     *
     * @param canvas 画布
     * @param rect 绘制区域
     * @param radius 发光半径
     */
    private fun drawBottomGlow(canvas: Canvas, rect: RectF, radius: Float) {
        val gradient = LinearGradient(
            rect.left, rect.bottom,
            rect.left, rect.bottom - radius,
            intArrayOf(glowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = gradient

        path.reset()
        path.addRect(rect.left, rect.bottom - radius, rect.right, rect.bottom, Path.Direction.CW)
        canvas.drawPath(path, glowPaint)
    }

    /**
     * 绘制左侧边缘发光
     *
     * 使用 LinearGradient 从左侧边缘向右渐变
     *
     * @param canvas 画布
     * @param rect 绘制区域
     * @param radius 发光半径
     */
    private fun drawLeftGlow(canvas: Canvas, rect: RectF, radius: Float) {
        val gradient = LinearGradient(
            rect.left, rect.top,
            rect.left + radius, rect.top,
            intArrayOf(glowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = gradient

        path.reset()
        path.addRect(rect.left, rect.top, rect.left + radius, rect.bottom, Path.Direction.CW)
        canvas.drawPath(path, glowPaint)
    }

    /**
     * 绘制右侧边缘发光
     *
     * 使用 LinearGradient 从右侧边缘向左渐变
     *
     * @param canvas 画布
     * @param rect 绘制区域
     * @param radius 发光半径
     */
    private fun drawRightGlow(canvas: Canvas, rect: RectF, radius: Float) {
        val gradient = LinearGradient(
            rect.right, rect.top,
            rect.right - radius, rect.top,
            intArrayOf(glowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = gradient

        path.reset()
        path.addRect(rect.right - radius, rect.top, rect.right, rect.bottom, Path.Direction.CW)
        canvas.drawPath(path, glowPaint)
    }

    /**
     * 绘制四个角的发光效果
     *
     * @param canvas 画布
     * @param rect 绘制区域
     * @param radius 发光半径
     */
    private fun drawCornerGlows(canvas: Canvas, rect: RectF, radius: Float) {
        drawCornerGlow(canvas, rect.left, rect.top, radius)
        drawCornerGlow(canvas, rect.right, rect.top, radius)
        drawCornerGlow(canvas, rect.left, rect.bottom, radius)
        drawCornerGlow(canvas, rect.right, rect.bottom, radius)
    }

    /**
     * 绘制单个角的发光效果
     *
     * 使用 RadialGradient 从角点向外圆形渐变
     *
     * @param canvas 画布
     * @param x 角点 X 坐标
     * @param y 角点 Y 坐标
     * @param radius 发光半径
     */
    private fun drawCornerGlow(canvas: Canvas, x: Float, y: Float, radius: Float) {
        val gradient = android.graphics.RadialGradient(
            x, y, radius,
            intArrayOf(glowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = gradient
        canvas.drawCircle(x, y, radius, glowPaint)
    }

    /**
     * 绘制顶部内阴影
     *
     * 从顶部边缘向下渐变的阴影效果，增强立体感
     *
     * @param canvas 画布
     * @param rect 绘制区域
     */
    private fun drawTopInnerShadow(canvas: Canvas, rect: RectF) {
        if (innerShadowThickness <= 0f) return

        val safeThickness = min(innerShadowThickness, min(width / 2f, height / 2f))

        val gradient = LinearGradient(
            rect.left, rect.top,
            rect.left, rect.top + safeThickness,
            intArrayOf(innerShadowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        shadowPaint.shader = gradient

        if (cornerRadius > 0f) {
            drawTopShadowWithCorners(canvas, rect, safeThickness)
        } else {
            path.reset()
            path.addRect(rect.left, rect.top, rect.right, rect.top + safeThickness, Path.Direction.CW)
            canvas.drawPath(path, shadowPaint)
        }
    }

    /**
     * 绘制带圆角的顶部内阴影
     *
     * 处理圆角区域的阴影渐变
     *
     * @param canvas 画布
     * @param rect 绘制区域
     * @param thickness 阴影厚度
     */
    private fun drawTopShadowWithCorners(canvas: Canvas, rect: RectF, thickness: Float) {
        path.reset()

        path.moveTo(rect.left + cornerRadius, rect.top)
        path.lineTo(rect.right - cornerRadius, rect.top)

        path.lineTo(rect.right, rect.top + cornerRadius)
        path.lineTo(rect.right, rect.top + thickness)
        path.lineTo(rect.left, rect.top + thickness)
        path.lineTo(rect.left, rect.top + cornerRadius)
        path.close()

        canvas.drawPath(path, shadowPaint)

        drawTopLeftCornerShadow(canvas, rect, thickness)
        drawTopRightCornerShadow(canvas, rect, thickness)
    }

    /**
     * 绘制左上角的内阴影
     *
     * 使用 RadialGradient 实现圆角区域的阴影渐变
     *
     * @param canvas 画布
     * @param rect 绘制区域
     * @param thickness 阴影厚度
     */
    private fun drawTopLeftCornerShadow(canvas: Canvas, rect: RectF, thickness: Float) {
        val cornerGradient = android.graphics.RadialGradient(
            rect.left + cornerRadius, rect.top + cornerRadius,
            cornerRadius,
            intArrayOf(innerShadowColor, innerShadowColor, Color.TRANSPARENT),
            floatArrayOf(0f, (cornerRadius - thickness) / cornerRadius, 1f),
            Shader.TileMode.CLAMP
        )
        shadowPaint.shader = cornerGradient

        path.reset()
        path.addArc(
            rect.left, rect.top,
            rect.left + cornerRadius * 2, rect.top + cornerRadius * 2,
            180f, 90f
        )
        path.lineTo(rect.left + cornerRadius, rect.top + cornerRadius)
        path.close()

        canvas.drawPath(path, shadowPaint)
    }

    /**
     * 绘制右上角的内阴影
     *
     * 使用 RadialGradient 实现圆角区域的阴影渐变
     *
     * @param canvas 画布
     * @param rect 绘制区域
     * @param thickness 阴影厚度
     */
    private fun drawTopRightCornerShadow(canvas: Canvas, rect: RectF, thickness: Float) {
        val cornerGradient = android.graphics.RadialGradient(
            rect.right - cornerRadius, rect.top + cornerRadius,
            cornerRadius,
            intArrayOf(innerShadowColor, innerShadowColor, Color.TRANSPARENT),
            floatArrayOf(0f, (cornerRadius - thickness) / cornerRadius, 1f),
            Shader.TileMode.CLAMP
        )
        shadowPaint.shader = cornerGradient

        path.reset()
        path.addArc(
            rect.right - cornerRadius * 2, rect.top,
            rect.right, rect.top + cornerRadius * 2,
            270f, 90f
        )
        path.lineTo(rect.right - cornerRadius, rect.top + cornerRadius)
        path.close()

        canvas.drawPath(path, shadowPaint)
    }

    /**
     * 绘制内描边
     *
     * 在视图内部边缘绘制描边效果，描边宽度为 strokeWidth
     *
     * @param canvas 画布
     * @param rect 绘制区域
     */
    private fun drawInnerStroke(canvas: Canvas, rect: RectF) {
        if (strokeWidth <= 0f) return

        strokePaint.color = strokeColor
        strokePaint.strokeWidth = strokeWidth * 2

        val strokeRect = RectF(
            rect.left + strokeWidth,
            rect.top + strokeWidth,
            rect.right - strokeWidth,
            rect.bottom - strokeWidth
        )

        if (cornerRadius > 0f) {
            val innerRadius = max(0f, cornerRadius - strokeWidth)
            path.reset()
            path.addRoundRect(strokeRect, innerRadius, innerRadius, Path.Direction.CW)
            canvas.drawPath(path, strokePaint)
        } else {
            path.reset()
            path.addRect(strokeRect, Path.Direction.CW)
            canvas.drawPath(path, strokePaint)
        }
    }
}
