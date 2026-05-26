package com.cn.core.ui.view.frosted

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withClip
import com.cn.core.ui.R
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 带流光效果的状态化发光视图
 *
 * 继承 StatefulGlowView，在描边上方添加沿路径顺时针循环的流光高亮效果。
 *
 * ## 流光原理
 * 1. 使用 PathMeasure 获取描边圆角矩形路径的总长度
 * 2. 通过 ValueAnimator 驱动 progress ∈ [0, 1) 持续循环
 * 3. 根据 progress 在路径上截取一段弧长作为"发光段"
 * 4. 发光段使用 LinearGradient（透明→亮色→透明）绘制，形成拖尾效果
 * 5. 当段跨越路径终点时自动拆分为两段绘制（保证无缝衔接）
 *
 * ## 显示条件
 * - 仅在焦点状态 (isFocused) 时显示流光效果
 */
@SuppressLint("Recycle", "ClickableViewAccessibility")
open class AnimatedStatefulGlowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : StatefulGlowView(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_FLOW_DURATION_MS = 2500L
        private const val DEFAULT_REFRESH_DURATION_MS = 3000L
        private const val DEFAULT_FLOW_COLOR = -0x22000000 // Color.WHITE with ~87% alpha
        private const val DEFAULT_REFRESH_COLOR = 0x64FFFFFF // ~39% white
        /** 流光段占路径总长的比例 */
        private const val DEFAULT_FLOW_SEGMENT_RATIO = 0.22f
        private const val FADE_IN_OUT_MS = 280L
        private const val MAX_GLOW_SEGMENT_LENGTH = 500f
    }

    // ==================== 公开属性 ====================

    /** 流光是否启用（设置后自动启动/停止） */
    var flowEnabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (value && isAttachedToWindow && isFocused) {
                startFlow()
            } else if (!value) {
                stopFlow()
            }
        }

    /** 流光颜色（默认亮白色） */
    @ColorInt
    var flowColor: Int = DEFAULT_FLOW_COLOR

    /** 流光一圈时长(ms) */
    var flowDurationMs: Long = DEFAULT_FLOW_DURATION_MS

    /** 流光段占路径总长的比例 [0.05, 0.6] */
    var flowSegmentRatio: Float = DEFAULT_FLOW_SEGMENT_RATIO

    /** 刷新效果是否启用 */
    var refreshEffectEnabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (value && isAttachedToWindow && isFocused) {
                startRefreshAnimation()
            } else if (!value) {
                stopRefreshAnimation()
            }
        }

    /** 刷新扫描颜色 */
    @ColorInt
    var refreshColor: Int = DEFAULT_REFRESH_COLOR
        set(value) {
            field = value
            refreshGradient = null
            invalidate()
        }

    // ==================== 内部状态 ====================

    private val flowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val flowPath = Path()
    private val segmentPath = Path()
    private val pathMeasure = PathMeasure()

    private var animator: ValueAnimator? = null
    private var fadeAnimator: ValueAnimator? = null

    // 刷新效果状态
    private val refreshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val refreshMatrix = Matrix()
    private var refreshGradient: LinearGradient? = null
    private var refreshAnimator: ValueAnimator? = null
    private var refreshRunnable: Runnable? = null
    private var refreshProgress = 0f
    private var diagonalLength = 0f
    private var refreshWidth = 0f

    private var progress = 0f          // [0, 1) 循环进度
    protected var flowAlpha = 0f         // 当前显示 alpha
    private var isRunningFlow = false
    private var isFadingOut = false

    // 缓存
    private var cachedWidth = 0f
    private var cachedHeight = 0f
    private var cachedTotalLength = 0f
    private var cachedSegmentLength = 0f
    private var pathDirty = true

    // ==================== 初始化 ====================

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.AnimatedStatefulGlowView, defStyleAttr, 0) {
                flowEnabled = getBoolean(R.styleable.AnimatedStatefulGlowView_asgv_flowEnabled, flowEnabled)
                flowColor = getColor(R.styleable.AnimatedStatefulGlowView_asgv_flowColor, flowColor)
                flowDurationMs =
                    getInt(R.styleable.AnimatedStatefulGlowView_asgv_flowDuration, flowDurationMs.toInt()).toLong()
                flowSegmentRatio =
                    getFloat(R.styleable.AnimatedStatefulGlowView_asgv_flowSegmentRatio, flowSegmentRatio)
                        .coerceIn(0.05f, 0.6f)
                refreshEffectEnabled = getBoolean(
                    R.styleable.AnimatedStatefulGlowView_asgv_refreshEffectEnabled,
                    refreshEffectEnabled
                )
                refreshColor = getColor(
                    R.styleable.AnimatedStatefulGlowView_asgv_refreshColor,
                    refreshColor
                )
            }
        }
    }

    // ==================== 绘制 ====================

    /**
     * 完全接管 dispatchDraw：背景 → 内发光 → 描边 → **流光** → restore → children
     * 流光位于描边上方、clipPath 裁剪范围内，仅焦点时显示
     */
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) {
            super.dispatchDraw(canvas)
            return
        }

        buildDispatchPath(w, h)
        canvas.withClip(dispatchClipPath) {
            // 蒙层
            drawOverlay(this, w, h)
            // 内发光
            drawInnerGlow(this, w, h/*, gr*/)
            // 描边
            drawStroke(this, w, h)
            // 顶部高光（描边上方）
            drawTopHighlight(this, w, h)
            // 流光（仅在焦点 + 启用 + 已附着窗口时显示）
            drawFlowEffect(this)
            // 刷新扫描效果（仅在焦点 + 启用时显示）
            drawRefreshEffect(this, w, h)

        }
    }

    /**
     * 绘制流光效果
     *
     * 核心算法：
     * 1. 用与焦点描边相同的圆角矩形路径 + PathMeasure 测量总长度
     * 2. 根据 progress 计算当前段的起止位置
     * 3. 若段未跨越路径终点 → 直接 getSegment 绘制
     * 4. 若段跨越终点 → 拆为两段绘制（保证无缝衔接）
     * 5. 每段使用起点→终点的 LinearGradient 实现透明→亮→透明渐变
     */
    protected fun drawFlowEffect(canvas: Canvas) {
        if (flowEnabled && isFocused && isAttachedToWindow && flowAlpha >= 0.01f) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0 || flowAlpha < 0.01f) return

            ensureFlowPath(w, h)
            if (cachedTotalLength <= 0) return

            // 始终使用焦点状态描边宽度作为线宽基准
            flowPaint.alpha = (255 * flowAlpha).toInt().coerceIn(0, 255)
            val focusedStrokeWidth = getStrokeWidthForState(State.FOCUSED)
            flowPaint.strokeWidth = focusedStrokeWidth.coerceAtLeast(1f) * 1.4f
            flowPaint.pathEffect = null

            // 计算当前段在路径上的起止距离
            val segLen = cachedSegmentLength
            val cycleStart = (progress * cachedTotalLength) % cachedTotalLength
            val cycleEnd = cycleStart + segLen

            if (cycleEnd <= cachedTotalLength) {
                drawFlowSegment(canvas, cycleStart, cycleEnd)
            } else {
                // 跨越路径末尾：拆成两段
                drawFlowSegment(canvas, cycleStart, cachedTotalLength)
                drawFlowSegment(canvas, 0f, cycleEnd - cachedTotalLength)
            }

            flowPaint.shader = null
        }
    }

    /** 确保流光路径已构建且最新（始终使用焦点状态的描边宽度） */
    private fun ensureFlowPath(w: Float, h: Float) {
        if (!pathDirty && cachedWidth == w && cachedHeight == h) return

        // 流光仅在焦点时显示，始终用焦点描边构建路径
        val focusedStrokeWidth = getStrokeWidthForState(State.FOCUSED)
        val halfStroke = focusedStrokeWidth / 2f
        val margin = halfStroke.coerceAtLeast(0.5f)

        flowPath.reset()
        flowPath.addRoundRect(
            margin, margin,
            w - margin, h - margin,
            radii, Path.Direction.CW
        )
        pathMeasure.setPath(flowPath, false)
        cachedTotalLength = pathMeasure.length
        cachedSegmentLength = min(
            cachedTotalLength * flowSegmentRatio,
            MAX_GLOW_SEGMENT_LENGTH
        ).coerceAtLeast(20f)

        cachedWidth = w
        cachedHeight = h
        pathDirty = false
    }

    /**
     * 绘制单段流光
     * 使用从段起点到终点的 LinearGradient，实现透明→flowColor→透明的渐变
     */
    private fun drawFlowSegment(canvas: Canvas, startDist: Float, endDist: Float) {
        segmentPath.reset()
        if (!pathMeasure.getSegment(startDist, endDist, segmentPath, true) || segmentPath.isEmpty) return

        val startPos = FloatArray(2)
        val endPos = FloatArray(2)
        pathMeasure.getPosTan(startDist, startPos, null)
        pathMeasure.getPosTan(endDist, endPos, null)

        flowPaint.shader = LinearGradient(
            startPos[0], startPos[1],
            endPos[0], endPos[1],
            intArrayOf(Color.TRANSPARENT, flowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(segmentPath, flowPaint)
    }

    /**
     * 绘制刷新扫描效果
     *
     * 对角线方向扫描：从左上角到右下角，使用 LinearGradient + Matrix 平移实现
     * 渐变带随 refreshProgress 从 (-rw, -rw) 移动到 (w+rw, h+rw)
     */
    protected fun drawRefreshEffect(canvas: Canvas, w: Float, h: Float) {
        if (refreshEffectEnabled && isFocused && isAttachedToWindow) {
            if (w <= 0 || h <= 0) return

            // 计算对角线长度和刷新带宽度（懒初始化）
            if (diagonalLength == 0f || refreshWidth == 0f) {
                diagonalLength = sqrt((w * w + h * h).toDouble()).toFloat()
                refreshWidth = diagonalLength / 6
            }

            if (diagonalLength == 0f || refreshWidth == 0f) return

            val rw = refreshWidth

            // 渐变的起止位置随 progress 移动
            val sx = -rw
            val sy = -rw
            val ex = w + rw
            val ey = h + rw

            val cx = sx + (ex - sx) * refreshProgress
            val cy = sy + (ey - sy) * refreshProgress

            // 创建或复用渐变
            if (refreshGradient == null) {
                refreshGradient = LinearGradient(
                    -rw, -rw,
                    rw, rw,
                    intArrayOf(Color.TRANSPARENT, refreshColor, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }

            // 用 Matrix 平移渐变到当前位置
            refreshMatrix.reset()
            refreshMatrix.setTranslate(cx, cy)
            refreshGradient?.setLocalMatrix(refreshMatrix)

            refreshPaint.shader = refreshGradient

            // 绘制 inset Rect，避免覆盖描边
            val focusedStrokeWidth = getStrokeWidthForState(State.FOCUSED)
            val inset = focusedStrokeWidth / 2f
            canvas.drawRect(inset, inset, w - inset, h - inset, refreshPaint)
        }
    }

    // ==================== 动画控制 ====================

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (gainFlowOnFocus()) {
            if (gainFocus) {
                startFlow()
                startRefreshAnimation()
            } else {
                stopFlow()
                stopRefreshAnimation()
            }
        }
    }

    /** 是否在获取焦点时自动启停流光 */
    protected open fun gainFlowOnFocus(): Boolean = true

    /** 启动流光动画 */
    fun startFlow() {
        if (isRunningFlow && !isFadingOut) return

        fadeAnimator?.cancel()
        isFadingOut = false

        if (flowAlpha >= 0.99f) {
            isRunningFlow = true
            setupLoopAnimation()
            return
        }

        isRunningFlow = true
        setupLoopAnimation()

        fadeAnimator = ValueAnimator.ofFloat(flowAlpha, 1f).apply {
            duration = ((1f - flowAlpha) * FADE_IN_OUT_MS).toLong().coerceAtLeast(100L)
            addUpdateListener { flowAlpha = it.animatedValue as Float; invalidate() }
            addListener(onFadeComplete { flowAlpha = 1f })
            start()
        }
    }

    /** 停止流光动画（淡出） */
    fun stopFlow() {
        if (flowAlpha <= 0.01f) {
            isRunningFlow = false
            cancelAll()
            return
        }

        isFadingOut = true
        fadeAnimator?.cancel()

        fadeAnimator = ValueAnimator.ofFloat(flowAlpha, 0f).apply {
            duration = (flowAlpha * FADE_IN_OUT_MS).toLong().coerceAtLeast(100L)
            addUpdateListener { flowAlpha = it.animatedValue as Float; invalidate() }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(a: Animator) {}
                override fun onAnimationEnd(a: Animator) {
                    isRunningFlow = false
                    isFadingOut = false
                    cancelAll()
                    invalidate()
                }
                override fun onAnimationCancel(a: Animator) {}
                override fun onAnimationRepeat(a: Animator) {}
            })
            start()
        }
    }

    private fun setupLoopAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = flowDurationMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener {
                val p = it.animatedValue as Float
                if (p != progress) {
                    progress = p
                    invalidate()
                }
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(a: Animator) {}
                override fun onAnimationEnd(a: Animator) {}
                override fun onAnimationCancel(a: Animator) {}
                override fun onAnimationRepeat(a: Animator) { invalidate() }
            })
            start()
        }
    }

    private fun cancelAll() {
        animator?.cancel(); animator = null
        fadeAnimator?.cancel(); fadeAnimator = null
        progress = 0f
        flowAlpha = 0f
    }

    // ==================== 刷新效果动画控制 ====================

    /** 启动刷新扫描动画 */
    fun startRefreshAnimation() {
        refreshAnimator?.cancel()

        refreshAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DEFAULT_REFRESH_DURATION_MS
            repeatCount = 0  // 单次播放
            interpolator = LinearInterpolator()
            addUpdateListener {
                val np = it.animatedValue as Float
                if (np != refreshProgress) {
                    refreshProgress = np
                    invalidate()
                }
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(a: Animator) {}
                override fun onAnimationEnd(a: Animator) {
                    // 动画结束后延迟重启，实现无限循环
                    refreshRunnable = Runnable {
                        refreshProgress = 0f
                        startRefreshAnimation()
                    }
                    postDelayed(refreshRunnable, 0)
                }
                override fun onAnimationCancel(a: Animator) {}
                override fun onAnimationRepeat(a: Animator) {}
            })
            start()
        }
    }

    /** 停止刷新扫描动画 */
    fun stopRefreshAnimation() {
        refreshAnimator?.cancel()
        refreshAnimator = null
        refreshRunnable?.let { removeCallbacks(it) }
        refreshRunnable = null
        refreshProgress = 0f
        refreshGradient = null
    }

    // ==================== 生命周期 ====================

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateFlowCache()
        invalidateRefreshCache()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) stopFlow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 窗口附着后如果已启用+已聚焦，自动启动
        if (flowEnabled && isFocused) startFlow()
        if (refreshEffectEnabled && isFocused) startRefreshAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAll()
        stopRefreshAnimation()
    }

    // ==================== 工具方法 ====================

    /** 使流光路径缓存失效 */
    fun invalidateFlowCache() {
        pathDirty = true
        cachedWidth = 0f
        cachedHeight = 0f
        cachedTotalLength = 0f
        invalidate()
    }

    /** 刷新效果缓存失效 */
    fun invalidateRefreshCache() {
        diagonalLength = 0f
        refreshWidth = 0f
        refreshGradient = null
        invalidate()
    }

    private fun onFadeComplete(action: () -> Unit): Animator.AnimatorListener =
        object : Animator.AnimatorListener {
            override fun onAnimationStart(a: Animator) {}
            override fun onAnimationEnd(a: Animator) { action(); fadeAnimator = null }
            override fun onAnimationCancel(a: Animator) {}
            override fun onAnimationRepeat(a: Animator) {}
        }
}
