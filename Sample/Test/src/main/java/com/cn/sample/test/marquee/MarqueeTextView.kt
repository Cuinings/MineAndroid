package com.cn.sample.test.marquee

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.TextView

/**
 * @Author: CuiNing
 * @Time: 2025/11/5 15:16
 * @Description:
 */
@SuppressLint("AppCompatCustomView")
class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

    private var animator: ObjectAnimator? = null
    private var currentDirection = Direction.LEFT
    private var animationDuration = 5000L // 默认5秒
    private var loopCount = ObjectAnimator.INFINITE // 默认无限循环
    private var currentLoop = 0
    private var completeListener: OnAnimationCompleteListener? = null

    enum class Direction {
        LEFT, RIGHT, UP, DOWN
    }

    interface OnAnimationCompleteListener {
        fun onAnimationComplete()
    }

    init {
        isSingleLine = true
        ellipsize = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            startAnimation()
        }
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

    private fun startAnimation() {
        animator?.cancel()

        val (propertyName, startValue, endValue) = when (currentDirection) {
            Direction.LEFT -> Triple(
                "translationX",
                width.toFloat(),
                -width.toFloat() - getTextWidth()
            )
            Direction.RIGHT -> Triple(
                "translationX",
                -width.toFloat() - getTextWidth(),
                width.toFloat()
            )
            Direction.UP -> Triple(
                "translationY",
                height.toFloat(),
                -height.toFloat() - getTextHeight()
            )
            Direction.DOWN -> Triple(
                "translationY",
                -height.toFloat() - getTextHeight(),
                height.toFloat()
            )
        }

        animator = ObjectAnimator.ofFloat(this, propertyName, startValue, endValue).apply {
            duration = animationDuration
            interpolator = LinearInterpolator()
            repeatCount = loopCount
            repeatMode = ObjectAnimator.RESTART

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    currentLoop = 0
                }

                override fun onAnimationEnd(animation: Animator) {
                    translationX = 0f
                    translationY = 0f
                    completeListener?.onAnimationComplete()
                }

                override fun onAnimationCancel(animation: Animator) {
                    translationX = 0f
                    translationY = 0f
                }

                override fun onAnimationRepeat(animation: Animator) {
                    currentLoop++
                    resetPosition()
                    if (loopCount != ObjectAnimator.INFINITE && currentLoop >= loopCount) {
                        animation.end()
                    }
                }
            })
        }

        animator?.start()
    }

    private fun resetPosition() {
        when (currentDirection) {
            Direction.LEFT -> translationX = width.toFloat()
            Direction.RIGHT -> translationX = -width.toFloat() - getTextWidth()
            Direction.UP -> translationY = height.toFloat()
            Direction.DOWN -> translationY = -height.toFloat() - getTextHeight()
        }
    }

    private fun getTextWidth(): Float {
        return paint.measureText(text.toString())
    }

    private fun getTextHeight(): Float {
        return textSize
    }

    fun getCurrentLoop(): Int {
        return currentLoop
    }

    fun stopAnimation() {
        animator?.cancel()
        translationX = 0f
        translationY = 0f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}