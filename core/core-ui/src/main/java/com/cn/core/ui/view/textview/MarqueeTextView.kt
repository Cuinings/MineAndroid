package com.cn.core.ui.view.textview

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * @Author: CuiNing
 * @Time: 2025/11/5 15:16
 * @Description:
 */
class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var textPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var text: String = "循环滚动的文字内容示例"
    private var textColor: Int = 0xFF333333.toInt()
    private var textSize: Float = 16f * resources.displayMetrics.density

    private var animator: ValueAnimator? = null
    private var currentDirection = Direction.LEFT
    private var animationDuration = 5000L
    private var loopCount = ValueAnimator.INFINITE
    private var currentLoop = 0
    private var offset = 0f
    private var textWidth = 0f
    private var textHeight = 0f
    private var fontMetrics: Paint.FontMetrics = Paint.FontMetrics()

    private var completeListener: OnAnimationCompleteListener? = null

    enum class Direction {
        LEFT, RIGHT, UP, DOWN
    }

    interface OnAnimationCompleteListener {
        fun onAnimationComplete()
    }

    init {
        textPaint.color = textColor
        textPaint.textSize = textSize
        textPaint.isAntiAlias = true
        calculateTextMetrics()
    }

    fun setText(text: String) {
        this.text = text
        calculateTextMetrics()
        invalidate()
    }

    fun setTextColor(color: Int) {
        this.textColor = color
        textPaint.color = color
        invalidate()
    }

    fun setTextSize(size: Float) {
        this.textSize = size * resources.displayMetrics.density
        textPaint.textSize = this.textSize
        calculateTextMetrics()
        invalidate()
    }

    fun setDirection(direction: Direction) {
        this.currentDirection = direction
        if (width > 0 && height > 0) {
            startAnimation()
        }
    }

    fun setAnimationDuration(duration: Long) {
        this.animationDuration = duration
        animator?.duration = duration
    }

    fun setLoopCount(count: Int) {
        this.loopCount = count
        this.currentLoop = 0
        animator?.repeatCount = count
    }

    fun setOnAnimationCompleteListener(listener: OnAnimationCompleteListener) {
        this.completeListener = listener
    }

    private fun calculateTextMetrics() {
        textWidth = textPaint.measureText(text)
        textPaint.getFontMetrics(fontMetrics)
        textHeight = fontMetrics.descent - fontMetrics.ascent
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            startAnimation()
        }
    }

    private fun startAnimation() {
        animator?.cancel()

        val (startValue, endValue) = when (currentDirection) {
            Direction.LEFT -> Pair(width.toFloat(), -textWidth)
            Direction.RIGHT -> Pair(-textWidth, width.toFloat())
            Direction.UP -> Pair(height.toFloat(), -textHeight)
            Direction.DOWN -> Pair(-textHeight, height.toFloat())
        }

        // 重置偏移量
        offset = startValue

        animator = ValueAnimator.ofFloat(startValue, endValue).apply {
            duration = animationDuration
            interpolator = LinearInterpolator()
            repeatCount = loopCount
            repeatMode = ValueAnimator.RESTART

            addUpdateListener { animation ->
                offset = animation.animatedValue as Float
                invalidate()
            }

            addListener(object : AnimatorListener {

                override fun onAnimationStart(animation: Animator) {
                    currentLoop = 0
                }

                override fun onAnimationEnd(animation: Animator) {
                    completeListener?.onAnimationComplete()
                }

                override fun onAnimationCancel(animation: Animator) {
                    offset = 0f
                    invalidate()
                }

                override fun onAnimationRepeat(animation: Animator) {
                    currentLoop++
                    if (loopCount != ValueAnimator.INFINITE && currentLoop >= loopCount) {
                        animation.end()
                    }
                }
            })
        }

        animator?.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (currentDirection) {
            Direction.LEFT, Direction.RIGHT -> {
                // 水平滚动：文本垂直居中
                val baselineY = (height + textPaint.textSize) / 2 - fontMetrics.descent

                // 绘制主文本
                canvas.drawText(text, offset, baselineY, textPaint)

                // 绘制第二个文本实现无缝循环（向左滚动时在右侧，向右滚动时在左侧）
                val secondTextOffset = if (currentDirection == Direction.LEFT) {
                    offset /*+ textWidth + 20f*/ // 向右偏移一个文本宽度加间距
                } else {
                    offset /*- textWidth - 20f*/ // 向左偏移一个文本宽度加间距
                }
                canvas.drawText(text, secondTextOffset, baselineY, textPaint)
            }
            Direction.UP, Direction.DOWN -> {
                // 垂直滚动：文本水平居中
                val centerX = (width - textWidth) / 2

                // 计算基准线位置，确保文本在垂直方向正确显示
                val baseline = when (currentDirection) {
                    Direction.UP -> offset - fontMetrics.ascent
                    Direction.DOWN -> offset - fontMetrics.ascent
                    else -> 0f
                }

                // 绘制主文本
                canvas.drawText(text, centerX, baseline, textPaint)

                // 绘制第二个文本实现无缝循环
                val secondTextOffset = if (currentDirection == Direction.UP) {
                    baseline /*+ textHeight + 20f*/ // 向下偏移一个文本高度加间距
                } else {
                    baseline /*- textHeight - 20f*/ // 向上偏移一个文本高度加间距
                }
                canvas.drawText(text, centerX, secondTextOffset, textPaint)
            }
        }
    }

    fun getCurrentLoop(): Int {
        return currentLoop
    }

    fun stopAnimation() {
        animator?.cancel()
        offset = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}