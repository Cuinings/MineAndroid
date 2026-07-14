package com.cn.core.ui.view.frosted

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.withStyledAttributes
import com.cn.core.ui.R

/**
 * 焦点缩放视图 — 继承 [FrostedAnimatedGlowView]，在获取焦点时容器整体缩放到指定比例，
 * 同时内部子 View 通过反向缩放补偿保持原始大小不变。
 *
 * ## 原理
 * - 焦点获得时：通过 [ValueAnimator] 将 [View.scaleX]/[View.scaleY] 动画过渡到 [focusScale]
 * - 焦点失去时：动画恢复 [View.scaleX]/[View.scaleY] 至 1.0
 * - 子 View 防缩放：重写 [drawChild]，在其中对画布施加 1/[scaleX] 反向缩放，
 *   使 child 始终以原始大小和位置渲染，不受父容器缩放影响
 * - 所有 FrostedAnimatedGlowView 的装饰效果（毛玻璃、发光、描边、流光等）
 *   随容器正常缩放，形成视觉突出的聚焦效果
 *
 * ## 使用
 * ```kotlin
 * val view = FocusScaleView(context).apply {
 *     focusScale = 1.08f             // 获得焦点时放大到 108%
 *     scaleAnimationDuration = 250L  // 动画时长 250ms
 * }
 * ```
 *
 * XML 属性：
 * - `app:fsv_focusScale`：焦点缩放比例，默认 1.05
 * - `app:fsv_scaleDuration`：缩放动画时长 (ms)，默认 200
 */
@SuppressLint("Recycle", "ClickableViewAccessibility")
open class FocusScaleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrostedAnimatedGlowView(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_FOCUS_SCALE = 1.05f
        private const val DEFAULT_SCALE_DURATION = 200L
    }

    // ==================== 公开属性 ====================

    /** 焦点缩放比例，默认 1.05。设置后若当前已聚焦则立即应用。 */
    var focusScale: Float = DEFAULT_FOCUS_SCALE
        set(value) {
            field = value.coerceAtLeast(1.0f)
            if (isFocused) {
                isScaleActive = true
                animateToScale(field)
            }
        }

    /** 缩放动画时长 (ms)，默认 200 */
    var scaleAnimationDuration: Long = DEFAULT_SCALE_DURATION

    // ==================== 内部状态 ====================

    private var scaleAnimator: ValueAnimator? = null

    /** 当前实际 scaleX（作为 scaleY 的同步值），用于 drawChild 中精确反向缩放 */
    private var visualScale: Float = 1.0f

    /** 动画目标值，用于去重（避免重复触发相同目标的动画） */
    private var targetScaleValue: Float = 1.0f

    /**
     * 缩放补偿是否激活。仅焦点发生转移时置为 true，drawChild 据此决定
     * 是否做反向缩放，避免首次加载（初始焦点分配）时误触发。
     */
    private var isScaleActive: Boolean = false

    /**
     * 是否已走过首帧绘制。Android 初始焦点分配在首帧 draw() 之前触发，
     * 用此标志区分"系统初始分配"与"用户真实焦点转移"。
     */
    private var hasDrawn: Boolean = false

    // ==================== 初始化 ====================

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.FocusScaleView, defStyleAttr, 0) {
                focusScale = getFloat(
                    R.styleable.FocusScaleView_fsv_focusScale,
                    DEFAULT_FOCUS_SCALE
                ).coerceAtLeast(1.0f)
                scaleAnimationDuration = getInt(
                    R.styleable.FocusScaleView_fsv_scaleDuration,
                    DEFAULT_SCALE_DURATION.toInt()
                ).toLong().coerceAtLeast(0L)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        // 确保缩放轴心始终在视图中心
        pivotX = width / 2f
        pivotY = height / 2f
    }

    /**
     * 首帧绘制标记。draw() 在所有 View 绘制链中必定被调用，
     * 且位于 onFocusChanged 之后，是区分"初始焦点"与"真实转移"的可靠分界点。
     */
    override fun draw(canvas: Canvas) {
        hasDrawn = true
        super.draw(canvas)
    }

    // ==================== 焦点缩放 ====================

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: android.graphics.Rect?,
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)

        // 首帧绘制尚未完成 → onFocusChanged 来自 Android 初始焦点分配，跳过
        // 首帧之后 → 真实用户焦点转移，正常动画
        if (!hasDrawn) return

        if (gainFocus) {
            isScaleActive = true
            animateToScale(focusScale)
        } else {
            // isScaleActive 由 animateToScale(1.0f) 的 onAnimationEnd 关闭，
            // 保证失焦缩回动画期间 drawChild 仍做反向补偿
            animateToScale(1.0f)
        }
    }

    /** 动画过渡到目标缩放比例 */
    private fun animateToScale(target: Float) {
        // 去重：已在目标值或正在向目标动画则不重复触发
        if (targetScaleValue == target && scaleAnimator?.isRunning == true) return
        targetScaleValue = target

        // 先摘掉旧 animator 监听器再取消：cancel() 会连带触发 onAnimationEnd，
        // 若旧动画 target==1.0f 则 isScaleActive 被误关，导致新放大动画期间不做反向补偿
        scaleAnimator?.removeAllUpdateListeners()
        scaleAnimator?.removeAllListeners()
        scaleAnimator?.cancel()

        if (width <= 0 || height <= 0) {
            // 布局尚未完成，延迟到布局结束后再执行
            post { animateToScale(target) }
            return
        }

        val from = visualScale
        scaleAnimator = ValueAnimator.ofFloat(from, target).apply {
            duration = scaleAnimationDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val s = it.animatedValue as Float
                visualScale = s
                scaleX = s
                scaleY = s
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(a: android.animation.Animator) {}
                override fun onAnimationCancel(a: android.animation.Animator) {}
                override fun onAnimationRepeat(a: android.animation.Animator) {}
                override fun onAnimationEnd(a: android.animation.Animator) {
                    // 缩放回到 1.0 后关闭补偿，后续 drawChild 零开销
                    if (target == 1.0f) {
                        isScaleActive = false
                    }
                }
            })
            start()
        }
    }

    // ==================== 子 View 反向缩放补偿 ====================

    /**
     * 重写 [drawChild]，对画布施加反向缩放使子 View 不受父容器 scaleX/scaleY 影响。
     *
     * 仅当 [isScaleActive] 为 true（焦点已发生实际转移）时才做反向补偿；
     * 首次加载的初始焦点不算"变化"，直接走默认绘制，零开销。
     *
     * 原理：
     * - 父容器设置 scaleX=n 后，View 系统会在绘制前对画布施加变换矩阵
     *   M = T(pivot) · S(n,n) · T(-pivot)
     * - 此处对画布施加 M 的逆变换 M⁻¹ = T(pivot) · S(1/n,1/n) · T(-pivot)
     *   即 canvas.scale(1/n, 1/n, pivotX, pivotY)
     * - 子 View 在该逆变换后的画布上绘制，视觉上保持原始尺寸和位置不变
     */
    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        if (!isScaleActive) {
            return super.drawChild(canvas, child, drawingTime)
        }

        val s = scaleX
        val invScale = 1.0f / s
        val result: Boolean
        canvas.save()
        canvas.scale(invScale, invScale, pivotX, pivotY)
        result = super.drawChild(canvas, child, drawingTime)
        canvas.restore()
        return result
    }

    // ==================== 生命周期 ====================

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scaleAnimator?.cancel()
        scaleAnimator = null
        isScaleActive = false
        hasDrawn = false
    }
}
