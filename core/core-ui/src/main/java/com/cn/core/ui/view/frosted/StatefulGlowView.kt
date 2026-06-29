package com.cn.core.ui.view.frosted

import com.cn.core.ui.R
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import androidx.core.graphics.withSave
import kotlin.math.exp

/**
 * @author:
 * @time: 2026/4/25 15:23
 * @history
 * @description:StatefulGlowImageView - 支持圆角、多状态背景色、描边、内发光的自定义View, 继承自ConstraintLayout，支持 elevation 阴影
 */
@SuppressLint("AppCompatCustomView", "NewApi")
open class StatefulGlowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // ======================== 圆角 ========================
    @SuppressLint("UnnecessaryArrayInit")
    protected var radii = FloatArray(8) { 0f }
        private set

    // ======================== 背景色（4状态）========================
    data class StateColors(
        @ColorInt var normal: Int = Color.TRANSPARENT,
        @ColorInt var pressed: Int = Color.TRANSPARENT,
        @ColorInt var focused: Int = Color.TRANSPARENT,
        @ColorInt var selected: Int = Color.TRANSPARENT,
        @ColorInt var disabled: Int = Color.TRANSPARENT,
    )

    var overlayEnabled: Boolean? = null
        set(value) { value.takeIf { it != field }?.let {
            field = it
            invalidate()
        } }
    var overlayColor = StateColors()
        private set
    @ColorInt
    protected var currentOverlayColor: Int = Color.TRANSPARENT
        private set

    // ======================== 描边（4状态）========================
    var strokeEnabled: Boolean? = null
        set(value) { value.takeIf { it != field }?.let {
            field = it
            invalidate()
        } }

    data class StateWidth(
        var normal: Float = 0f,
        var pressed: Float = 0f,
        var focused: Float = 0f,
        var selected: Float = 0f,
        var disabled: Float = 0f,
    ) {
        fun get(state: State): Float = when (state) {
            State.NORMAL -> normal
            State.PRESSED -> pressed
            State.FOCUSED -> focused
            State.SELECTED -> selected
            State.DISABLED -> disabled
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        val newState = when {
            !isEnabled -> State.DISABLED
            isPressed -> State.PRESSED
            isSelected -> State.SELECTED
            hasFocus() -> State.FOCUSED
            else -> State.NORMAL
        }
        if (newState != currentState && newState != targetState) {
            animateStateChange(newState)
        }
    }

    var strokeWidths = StateWidth()
        private set

    val strokeColors by lazy {
        StateColors().apply {
            this.focused = resources.getColor(R.color.common_color_stroke_focused, context.theme)
        }
    }

    protected var currentStrokeColor: Int = Color.TRANSPARENT
        private set

    protected var currentStrokeWidth: Float = 0f
        private set

    // ======================== 内发光（4状态）========================
    var glowEnabled: Boolean = true
    var glowRadii = StateWidth()
        private set

    val glowColors by lazy {
        StateColors().apply {
            focused = resources.getColor(R.color.common_color_glow_focused, context.theme)
        }
    }

    // ======================== 顶部高光 ========================
    /** 是否显示顶部边缘高光（模拟上方光源反射） */
    var topHighlightEnabled: Boolean? = null
        set(value) { value.takeIf { it != field }?.let {
            field = it
            invalidate()
        } }

    /** 顶部高光颜色，默认亮白色 */
    @ColorInt
    var topHighlightColor: Int = "#7FFFFFFF".toColorInt() // ~87% 白色

    /** 顶部高光线高度（向内延伸距离） */
    var topHighlightHeight: Float = 4f

    // ======================== 状态机 ========================
    enum class State { NORMAL, PRESSED, FOCUSED, SELECTED, DISABLED }
    private var currentState:State = State.NORMAL
        set(value) { value.takeIf { it != field }?.let {
            field = it
            Log.d(StatefulGlowView::class.simpleName, "currentState: ${it.name}")
        } }
    private var targetState = State.NORMAL

    // ======================== 动画相关 ========================
    private var colorAnimator: ValueAnimator? = null
    private var strokeAnimator: ValueAnimator? = null
    private var glowRadiusAnimator: ValueAnimator? = null

    private var animFromBgColor: Int = Color.TRANSPARENT
    private var animToBgColor: Int = Color.TRANSPARENT
    private var animFromStrokeColor: Int = Color.TRANSPARENT
    private var animToStrokeColor: Int = Color.TRANSPARENT
    private var animFromStrokeWidth: Float = 0f
    private var animToStrokeWidth: Float = 0f
    private var animFromGlowRadius: Float = 0f
    private var animToGlowRadius: Float = 0f

    companion object {
        private const val ANIM_DURATION = 200L
        /** 内发光光环层数 — EVEN_ODD 每层仅1次draw，32层已足够平滑 */
        private const val INNER_GLOW_LAYERS = 32
        /** 相邻环重叠率: 0.5 = 每个环向外扩展+向内收缩各半个步长, 重叠率50% */
        private const val GLOW_OVERLAP_RATIO = 0.5f
        /** 整体柔化模糊半径: 仅消除层间微纹, 不影响结构清晰度 */
        private const val GLOW_SOFTEN_BLUR = 1.5f
    }

    // ======================== Path / Paint ========================
    /** 供子类 dispatchDraw 复用的裁剪路径 */
    protected val dispatchClipPath = Path()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        // 系统背景设为透明，避免覆盖自定义绘制并支持 elevation 阴影
        super.setBackground(Color.TRANSPARENT.toDrawable())

        setWillNotDraw(false)

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.StatefulGlowView, defStyleAttr, 0) {
                val cornerRadius = getDimension(R.styleable.StatefulGlowView_sgv_cornerRadius, -1f)
                if (cornerRadius >= 0) {
                    setCornerRadius(cornerRadius)
                } else {
                    val tl = getDimension(R.styleable.StatefulGlowView_sgv_cornerRadiusTL, 0f)
                    val tr = getDimension(R.styleable.StatefulGlowView_sgv_cornerRadiusTR, 0f)
                    val br = getDimension(R.styleable.StatefulGlowView_sgv_cornerRadiusBR, 0f)
                    val bl = getDimension(R.styleable.StatefulGlowView_sgv_cornerRadiusBL, 0f)
                    setCornerRadii(tl, tr, br, bl)
                }

                overlayEnabled = getBoolean(R.styleable.StatefulGlowView_sgv_overlayEnabled, false)
                if (overlayEnabled == true) {
                    overlayColor.normal = getColor(R.styleable.StatefulGlowView_sgv_overlayColorNormal, overlayColor.normal)
                    overlayColor.pressed = getColor(R.styleable.StatefulGlowView_sgv_overlayColorPressed, overlayColor.pressed)
                    overlayColor.focused = getColor(R.styleable.StatefulGlowView_sgv_overlayColorFocused, overlayColor.focused)
                    overlayColor.selected = getColor(R.styleable.StatefulGlowView_sgv_overlayColorSelected, overlayColor.selected)
                    overlayColor.disabled = getColor(R.styleable.StatefulGlowView_sgv_overlayColorDisabled, overlayColor.disabled)
                }
                strokeEnabled = getBoolean(R.styleable.StatefulGlowView_sgv_strokeEnabled, false)
                if (strokeEnabled == true) {
                    strokeWidths.normal = getDimension(R.styleable.StatefulGlowView_sgv_strokeWidthNormal, strokeWidths.normal)
                    strokeWidths.pressed = getDimension(R.styleable.StatefulGlowView_sgv_strokeWidthPressed, strokeWidths.pressed)
                    strokeWidths.focused = getDimension(R.styleable.StatefulGlowView_sgv_strokeWidthFocused, strokeWidths.focused)
                    strokeWidths.selected = getDimension(R.styleable.StatefulGlowView_sgv_strokeWidthSelected, strokeWidths.selected)
                    strokeWidths.disabled = getDimension(R.styleable.StatefulGlowView_sgv_strokeWidthDisabled, strokeWidths.disabled)

                    strokeColors.normal = getColor(R.styleable.StatefulGlowView_sgv_strokeColorNormal, strokeColors.normal)
                    strokeColors.pressed = getColor(R.styleable.StatefulGlowView_sgv_strokeColorPressed, strokeColors.pressed)
                    strokeColors.focused = getColor(R.styleable.StatefulGlowView_sgv_strokeColorFocused, strokeColors.focused)
                    strokeColors.selected = getColor(R.styleable.StatefulGlowView_sgv_strokeColorSelected, strokeColors.selected)
                    strokeColors.disabled = getColor(R.styleable.StatefulGlowView_sgv_strokeColorDisabled, strokeColors.disabled)
                }

                glowEnabled = getBoolean(R.styleable.StatefulGlowView_sgv_glowEnabled, glowEnabled)
                if (glowEnabled) {
                    glowRadii.normal = getDimension(R.styleable.StatefulGlowView_sgv_glowRadiusNormal, glowRadii.normal)
                    glowRadii.pressed = getDimension(R.styleable.StatefulGlowView_sgv_glowRadiusPressed, glowRadii.pressed)
                    glowRadii.focused = getDimension(R.styleable.StatefulGlowView_sgv_glowRadiusFocused, glowRadii.focused)
                    glowRadii.selected = getDimension(R.styleable.StatefulGlowView_sgv_glowRadiusSelected, glowRadii.selected)
                    glowRadii.disabled = getDimension(R.styleable.StatefulGlowView_sgv_glowRadiusDisabled, glowRadii.disabled)

                    glowColors.normal = getColor(R.styleable.StatefulGlowView_sgv_glowColorNormal, glowColors.normal)
                    glowColors.pressed = getColor(R.styleable.StatefulGlowView_sgv_glowColorPressed, glowColors.pressed)
                    glowColors.focused = getColor(R.styleable.StatefulGlowView_sgv_glowColorFocused, glowColors.focused)
                    glowColors.selected = getColor(R.styleable.StatefulGlowView_sgv_glowColorSelected, glowColors.selected)
                    glowColors.disabled = getColor(R.styleable.StatefulGlowView_sgv_glowColorDisabled, glowColors.disabled)
                }

                // 顶部高光
                topHighlightEnabled = getBoolean(R.styleable.StatefulGlowView_sgv_topHighlightEnabled, false)
                topHighlightColor = getColor(R.styleable.StatefulGlowView_sgv_topHighlightColor, topHighlightColor)
            }
        }

        applyStateInstantly(State.NORMAL)
    }




// ======================== 公开API ========================

    fun setCornerRadius(radius: Float) {
        radii.fill(radius)
        updateRadiiArray()
    }

    fun setCornerRadii(tl: Float, tr: Float, br: Float, bl: Float) {
        radii[0] = tl; radii[1] = tl
        radii[2] = tr; radii[3] = tr
        radii[4] = br; radii[5] = br
        radii[6] = bl; radii[7] = bl
        updateRadiiArray()
    }

    fun setOverlayColorForState(@ColorInt color: Int, state: State) {
        when (state) {
            State.NORMAL -> overlayColor.normal = color
            State.PRESSED -> overlayColor.pressed = color
            State.FOCUSED -> overlayColor.focused = color
            State.SELECTED -> overlayColor.selected = color
            State.DISABLED -> overlayColor.disabled = color
        }
        if (currentState == state && targetState == state) {
            currentOverlayColor = color
            invalidate()
        }
    }

    fun setStrokeWidthForState(width: Float, state: State) {
        when (state) {
            State.NORMAL -> strokeWidths.normal = width
            State.PRESSED -> strokeWidths.pressed = width
            State.FOCUSED -> strokeWidths.focused = width
            State.SELECTED -> strokeWidths.selected = width
            State.DISABLED -> strokeWidths.disabled = width
        }
        if (currentState == state && targetState == state) {
            currentStrokeWidth = width
            strokePaint.strokeWidth = width
            invalidate()
        }
    }

    /** 获取指定状态的描边宽度 */
    fun getStrokeWidthForState(state: State): Float = strokeWidths.get(state)

    fun setStrokeColorForState(@ColorInt color: Int, state: State) {
        when (state) {
            State.NORMAL -> strokeColors.normal = color
            State.PRESSED -> strokeColors.pressed = color
            State.FOCUSED -> strokeColors.focused = color
            State.SELECTED -> strokeColors.selected = color
            State.DISABLED -> strokeColors.disabled = color
        }
        if (currentState == state && targetState == state) {
            currentStrokeColor = color
            strokePaint.color = color
            invalidate()
        }
    }

    fun setGlowRadiusForState(radius: Float, state: State) {
        when (state) {
            State.NORMAL -> glowRadii.normal = radius
            State.PRESSED -> glowRadii.pressed = radius
            State.FOCUSED -> glowRadii.focused = radius
            State.SELECTED -> glowRadii.selected = radius
            State.DISABLED -> glowRadii.disabled = radius
        }
        if (currentState == state && targetState == state) {
            invalidate()
        }
    }

    fun getGlowRadiusForState(state: State): Float = glowRadii.get(state)

    /** 获取当前动画中的发光半径（供子类使用） */
    protected fun getCurrentGlowRadius(): Float {
        return if (glowRadiusAnimator?.isRunning == true) {
            (glowRadiusAnimator?.animatedValue as? Float) ?: glowRadii.get(targetState)
        } else {
            glowRadii.get(currentState)
        }
    }

// ======================== 内部方法 ========================

    private fun updateRadiiArray() {
        invalidateOutline()
        invalidate()
    }

    /** 构建裁剪路径（子类可调用） */
    protected fun buildDispatchPath(w: Float, h: Float) {
        dispatchClipPath.reset()
        dispatchClipPath.addRoundRect(0f, 0f, w, h, radii, Path.Direction.CW)
    }

    protected open fun applyStateInstantly(state: State) {
        currentState = state
        targetState = state
        currentOverlayColor = overlayColor.get(state)
        currentStrokeColor = strokeColors.get(state)
        currentStrokeWidth = strokeWidths.get(state)

        bgPaint.color = currentOverlayColor
        strokePaint.color = currentStrokeColor
        strokePaint.strokeWidth = currentStrokeWidth
        invalidate()
    }

    private fun StateColors.get(state: State): Int = when (state) {
        State.NORMAL -> normal
        State.PRESSED -> pressed
        State.FOCUSED -> focused
        State.SELECTED -> selected
        State.DISABLED -> disabled
    }

    private fun animateStateChange(newState: State) {
        targetState = newState

        val toBg = overlayColor.get(newState)
        val toStroke = strokeColors.get(newState)
        val toSw = strokeWidths.get(newState)
        val toGr = glowRadii.get(newState)

        colorAnimator?.cancel()
        strokeAnimator?.cancel()
        glowRadiusAnimator?.cancel()

        animFromBgColor = currentOverlayColor
        animToBgColor = toBg
        animFromStrokeColor = currentStrokeColor
        animToStrokeColor = toStroke
        animFromStrokeWidth = currentStrokeWidth
        animToStrokeWidth = toSw
        animFromGlowRadius = glowRadii.get(currentState)
        animToGlowRadius = toGr

        currentState = newState

        colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIM_DURATION
            addUpdateListener {
                val f = it.animatedFraction
                currentOverlayColor = lerpColor(f, animFromBgColor, animToBgColor)
                currentStrokeColor = lerpColor(f, animFromStrokeColor, animToStrokeColor)
                bgPaint.color = currentOverlayColor
                strokePaint.color = currentStrokeColor
                invalidate()
            }
            start()
        }

        if (animFromStrokeWidth != animToStrokeWidth) {
            strokeAnimator = ValueAnimator.ofFloat(animFromStrokeWidth, animToStrokeWidth).apply {
                duration = ANIM_DURATION
                addUpdateListener {
                    currentStrokeWidth = it.animatedValue as Float
                    strokePaint.strokeWidth = currentStrokeWidth
                    invalidate()
                }
                start()
            }
        } else {
            strokeAnimator = null
        }

        if (animFromGlowRadius != animToGlowRadius) {
            glowRadiusAnimator = ValueAnimator.ofFloat(animFromGlowRadius, animToGlowRadius).apply {
                duration = ANIM_DURATION
                addUpdateListener { invalidate() }
                start()
            }
        } else {
            glowRadiusAnimator = null
        }
    }

    /** 线性插值颜色，替代已废弃的 ArgbEvaluator */
    private fun lerpColor(fraction: Float, from: Int, to: Int): Int {
        val f = fraction.coerceIn(0f, 1f)
        val a = (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * f).toInt()
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * f).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * f).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * f).toInt()
        return Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

// ======================== 绘制 ========================

    override fun dispatchDraw(canvas: Canvas) {
//    Log.d(StatefulGlowView::class.simpleName, "dispatchDraw: ")
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) {
            super.dispatchDraw(canvas)
            return
        }
//    Log.d(StatefulGlowView::class.simpleName, "dispatchDraw: 1")
        buildDispatchPath(w, h)
        canvas.withClip(dispatchClipPath) {
            // 蒙层
            drawOverlay(this, w, h)
            // 内发光
            drawInnerGlow(this, w, h/*, gr*/)
            // 描边
            drawStroke(this, w, h)
            // 顶部高光（在描边上方）
            drawTopHighlight(this, w, h)
        }
        super.dispatchDraw(canvas)
    }

    protected open fun drawOverlay(canvas: Canvas, w: Float, h: Float) {
        if (overlayEnabled == true || currentOverlayColor != Color.TRANSPARENT) {
            canvas.drawRect(0f, 0f, w, h, bgPaint)
        }
    }

    protected open fun drawStroke(canvas: Canvas, w: Float, h: Float) {
        if (strokeEnabled != true || currentStrokeWidth <= 0) return
        canvas.drawPath(dispatchClipPath, strokePaint)
    }

    /**
     * 顶部高光 — 仅顶部边缘（含两圆角）的亮线
     */
    protected open fun drawTopHighlight(canvas: Canvas, w: Float, h: Float) {
        if (topHighlightEnabled == true || hasFocus()) {
            // 高光线宽跟随当前描边宽度（仅局部使用，不修改成员变量）
            val highlightStroke = strokeWidths.get(currentState)
            // 高光紧贴描边：margin ≈ 0 让高光中心与描边中心基本重合
            // 负值微重叠确保无间隙
            val m = 0f

            // 完整圆角矩形路径
            val path = Path()
            path.addRoundRect(m, m, w - m, h - m, radii, Path.Direction.CW)

            // 水平渐变：两端透明 → 中间亮 → 两端透明
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = highlightStroke
                isDither = true
                shader = LinearGradient(
                    0f, 0f, w, 0f,
                    intArrayOf(Color.TRANSPARENT, topHighlightColor, topHighlightColor, topHighlightColor, topHighlightColor, topHighlightColor, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.125f, 0.25f, 0.5f, 0.75f, 0.875f, 1f),
                    Shader.TileMode.CLAMP
                )
            }

            // 只裁剪显示顶部区域（含圆角），去除底部高光
            canvas.withSave() {
                val clipRect =
                    RectF(0f, 0f, w, (m + highlightStroke * 4).coerceAtLeast(highlightStroke * 2))
                clipRect(clipRect)
                drawPath(path, paint)
            }
        }
    }

    // 内发光 — 多层 EVEN_ODD 环形纯色，四边均匀零线条零GC
    private val ringPath = Path()
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
    }
    // 预分配数组（零 GC）
    private val outerRadiiCache = FloatArray(8)
    private val innerRadiiCache = FloatArray(8)
    // alpha 缓存
    private var glowAlphaTable: IntArray? = null
    private var lastGlowBaseAlpha = -1

    /**
     * 内发光效果 — 多层 EVEN_ODD 纯色环形（高斯衰减，类真实光晕）
     *
     * ════════════════ 技术选型决策树 ════════════════
     *
     * 需求: 四边均匀发光 + 零线条/条纹 + 高性能 + 边缘柔和
     *
     * ① 单 Shader 方案?
     *    LinearGradient → 方向偏置（只有一边亮）❌
     *    RadialGradient → 圆形扩散不适矩形（四边暗角亮）❌
     *    ComposeShader → 实测不均匀 ❌
     *    结论: 不存在能产生"同心矩形等亮度线"的单Shader ✗
     *
     * ② STROKE 方案?
     *    本质是"线"的并排排列，无论多少层都有几何边界
     *    alpha 高时边界对比度↑ → 线条显现 ❌
     *    BlurMaskFilter 反而放大莫尔条纹 ❌
     *    结论: STROKE 架构无法彻底消除线条 ✗
     *
     * ③ FILL 方案?
     *    EVEN_ODD 环形 → 每层是"面"不是"线" → 零几何边界 ✅
     *    DST_OUT 挖空 → 需要 saveLayer（软件渲染）→ 性能差 ⚠️
     *    EVEN_ODD 差集 → GPU 原生支持 → 零 saveLayer ✅
     *    结论: **EVEN_ODD + FILL = 最优解** ✓
     *
     * ④ 衰减曲线选型?
     *    五次方 (1-t)^5 → 衰减过陡，光集中于外缘窄带 ❌
     *    高斯 exp(-ratio²/(2σ²)) → 平滑连续，类真实光晕 ✅
     *    结论: **高斯 σ=0.35 + 1.5x 亮度补偿** ✓
     *
     * ════════════════ 性能指标 ════════════════
     *
     * | 指标                | 值                        |
     * |--------------------|--------------------------|
     * | drawPath 调用/帧   | 32 次 (INNER_GLOW_LAYERS) |
     * | Path 对象          | 1 个 (ringPath 复用)      |
     * | Paint 对象         | 1 个 (ringPaint 复用)     |
     * | saveLayer          | 0 次 (纯硬件加速)         |
     * | 离屏 Bitmap        | 0                        |
     * | GC 分配/帧         | 0 (全部预分配)            |
     * | Alpha 计算/帧      | 0 (glowAlphaTable 缓存)   |
     */
    protected open fun drawInnerGlow(canvas: Canvas, w: Float, h: Float/*, glowRadius: Float*/) {
        if (!glowEnabled) return
        val glowRadius = getCurrentGlowRadius()
        if (glowRadius <= 0) return
        val gc = glowColors.get(currentState)
        if (gc == Color.TRANSPARENT) return
        val baseAlpha = Color.alpha(gc).coerceAtLeast(60)
        val cr = Color.red(gc)
        val cg = Color.green(gc)
        val cb = Color.blue(gc)

        val layers = INNER_GLOW_LAYERS

        // 预计算 alpha 表（仅 baseAlpha 变化时重算）
        if (glowAlphaTable == null || glowAlphaTable!!.size != layers || lastGlowBaseAlpha != baseAlpha) {
            glowAlphaTable = IntArray(layers) { i ->
                val ratio = i.toFloat() / (layers - 1)
                // 高斯衰减曲线 (σ=0.35)：边缘最亮→向内平滑衰减，零硬边
                // 边缘(ratio=0): falloff=1.0 全亮
                // σ 位置(ratio=0.35): falloff≈0.607
                // 50%位置(ratio=0.50): falloff≈0.361
                // 70%位置(ratio=0.70): falloff≈0.135
                // 内部(ratio=1): falloff≈0.017 几乎透明
                val sigma = 0.35f
                val falloff = exp(-(ratio * ratio) / (2f * sigma * sigma))
                // 亮度补偿 1.5x 抵消高斯扩散后的视觉衰减
                (baseAlpha * falloff * 1.5f).toInt().coerceIn(0, 255)
            }
//        Log.d(StatefulGlowView::class.simpleName, "drawInnerGlow: baseAlpha:$baseAlpha")
            lastGlowBaseAlpha = baseAlpha
        }

        ringPath.fillType = Path.FillType.EVEN_ODD

        repeat(layers) { i ->
            val a = glowAlphaTable!![i]
            if (a < 2) return@repeat  // alpha<2 视觉不可见，跳过节省绘制

            // 外边界 / 内边界 — 相邻环无缝衔接
            val outerMargin = glowRadius * i.toFloat() / layers
            val innerMargin = glowRadius * ((i + 1).toFloat() / layers)

            // === 复用预分配数组（零 GC）===
            for (j in 0 until 8) {
                outerRadiiCache[j] = (radii[j] - outerMargin).coerceAtLeast(0f)
                innerRadiiCache[j] = (radii[j] - innerMargin).coerceAtLeast(0f)
            }

            ringPath.reset()
            ringPath.addRoundRect(
                outerMargin, outerMargin,
                w - outerMargin, h - outerMargin,
                outerRadiiCache, Path.Direction.CW
            )
            ringPath.addRoundRect(
                innerMargin, innerMargin,
                w - innerMargin, h - innerMargin,
                innerRadiiCache, Path.Direction.CW
            )

            ringPaint.color = Color.argb(a, cr, cg, cb)
            canvas.drawPath(ringPath, ringPaint)
        }
    }

// ======================== 触摸/焦点事件 ========================

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateOutline()
    }

    override fun setPressed(pressed: Boolean) {
        val newState = if (!isEnabled) State.DISABLED else if (pressed) State.PRESSED else State.NORMAL
        if (newState != currentState && newState != targetState) {
            animateStateChange(newState)
        }
        super.setPressed(pressed)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        val newState = if (!isEnabled) State.DISABLED else if (gainFocus) State.FOCUSED else State.NORMAL
        if (newState != currentState && newState != targetState) {
            animateStateChange(newState)
        }
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }


    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        val newState = if (selected) State.SELECTED else State.NORMAL
        if (newState != currentState && newState != targetState) {
            animateStateChange(newState)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        val newState = if (enabled) State.NORMAL else State.DISABLED
        if (newState != currentState && newState != targetState) {
            animateStateChange(newState)
        }
        super.setEnabled(enabled)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        colorAnimator?.cancel()
        strokeAnimator?.cancel()
        glowRadiusAnimator?.cancel()
        colorAnimator = null
        strokeAnimator = null
        glowRadiusAnimator = null
    }
    override fun setBackground(background: Drawable?) {
        // 禁止外部设置背景
    }

    /**
     * 子类专用：绕过 [setBackground] 限制，直接设置 View 底层背景。
     * 用于 BackgroundBlurDrawable 等需要实际渲染背景 drawable 的场景。
     */
    protected fun setBackgroundInternal(drawable: Drawable?) {
        super.setBackground(drawable)
    }
}
