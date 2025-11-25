package com.cn.sample.test.child.wallpaper

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import com.cn.sample.test.R

/**
 * @Author: CuiNing
 * @Time: 2025/8/20 9:42
 * @Description:
 */

/**
 * 自定义ViewGroup：动态根据壁纸亮度切换背景色
 * 功能：
 *  1. 支持平滑过渡动画
 *  3. 可配置动画时长
 */
class AdaptiveWallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // 属性配置
    private var animationDuration = 1000L // 默认动画时长
    private var currentBackgroundColor = Color.TRANSPARENT

    init {
        // 解析自定义属性
        context.withStyledAttributes(attrs, R.styleable.AdaptiveWallpaperView) {
            animationDuration = getInt(R.styleable.AdaptiveWallpaperView_animationDuration, 1000).toLong()
        }
    }

    @ColorInt
    private var color: Int = Color.WHITE
        set(value) { value.takeIf { it != field }?.let {
            field = it
            post { animateBackgroundChange(it) }
        } }

    /**
     * 平滑过渡背景色动画
     */
    var animator: ValueAnimator? = null
    private fun animateBackgroundChange(targetColor: Int) {
        animator?.cancel()
        animator = ValueAnimator.ofArgb(currentBackgroundColor, targetColor).apply {
            duration = animationDuration
            addUpdateListener { animation ->
                currentBackgroundColor = animation.animatedValue as Int
                setBackgroundColor(currentBackgroundColor)
            }
            start()
        }
    }

    /**
     * 提供外部更新方法（如壁纸变化时）
     */
    fun refreshColor(@ColorInt color: Int) {
        this@AdaptiveWallpaperView.color = color
    }
}