package com.cn.sample.test.child.app

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone

/**
 * @Author: CuiNing
 * @Time: 2025/12/2 15:27
 * @Description:
 */
open class ColumnMajorGridLayout: ViewGroup {

    var columnCount: Int = 1
        set(value) {
            if (value < 1) throw IllegalArgumentException("columnCount must be >= 1")
            field = value
            requestLayout()
        }

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    @SuppressLint("NewApi", "CustomViewStyleable", "UseKtx")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val childWidth = (widthSize / columnCount).coerceAtLeast(0)

        var totalHeight = 0
        var maxChildWidth = 0

        // 测量子视图
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isGone) continue

            val childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

            child.measure(childWidthSpec, childHeightSpec)
            totalHeight += child.measuredHeight
            maxChildWidth = maxOf(maxChildWidth, child.measuredWidth)
        }

        // 父容器宽度：尊重 EXACTLY（如 match_parent），否则用内容宽
        val finalWidth = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> widthSize
            else -> (maxChildWidth * columnCount).coerceAtMost(widthSize)
        }

        // 高度：根据子视图总高 + padding
        val finalHeight = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            else -> totalHeight + paddingTop + paddingBottom
        }

        setMeasuredDimension(finalWidth, finalHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val paddingLeft = this.paddingLeft
        val paddingTop = this.paddingTop
        val contentWidth = right - left - paddingLeft - paddingRight
        val cellWidth = contentWidth / columnCount

        val visibleChildren = (0 until childCount).mapNotNull {
            val child = getChildAt(it)
            if (child.visibility != View.GONE) child else null
        }

        if (visibleChildren.isEmpty()) return

        val rowCount = (visibleChildren.size + columnCount - 1) / columnCount // ceil division

        var index = 0
        for (col in 0 until columnCount) {
            for (row in 0 until rowCount) {
                if (index >= visibleChildren.size) break

                val child = visibleChildren[index]
                val cellHeight = child.measuredHeight

                val childLeft = paddingLeft + col * cellWidth
                val childRight = childLeft + cellWidth

                // 计算当前列中所有前面子项的高度和（用于定位 top）
                var childTop = paddingTop
                for (r in 0 until row) {
                    val prevIndex = r * columnCount + col
                    if (prevIndex < visibleChildren.size) {
                        childTop += visibleChildren[prevIndex].measuredHeight
                    }
                }
                val childBottom = childTop + cellHeight

                child.layout(childLeft, childTop, childRight, childBottom)
                index++
            }
        }
    }
}