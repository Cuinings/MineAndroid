package com.cn.library.common.icon.battery

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * @Author: CuiNing
 * @Time: 2025/11/24 17:25
 * @Description:
 */
class BatteryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var batteryLevel = 80 // 0-100

    init {
        paint.strokeWidth = 6f // 加宽边框让视觉效果更好
    }

    fun setBatteryLevel(level: Int) {
        batteryLevel = level.coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 8f // 增加padding以适应更圆滑的外观
        val headWidth = width * 0.15f // 稍微增加头的宽度
        val bodyWidth = width - headWidth - padding
        val bodyHeight = height - padding * 2

        // 设置圆角半径为高度的一半来实现圆润的效果
        val cornerRadius = bodyHeight / 2

        // 绘制电池主体
        paint.color = Color.GRAY
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(padding, padding, bodyWidth, height - padding, cornerRadius, cornerRadius, paint)

        // 绘制电池头
        canvas.drawRect(bodyWidth, height * 0.4f, width - padding, height * 0.6f, paint)

        // 绘制电量
        if (batteryLevel > 0) {
            val fillWidth = bodyWidth * batteryLevel / 100
            paint.color = when {
                batteryLevel > 50 -> Color.GREEN
                batteryLevel > 20 -> Color.YELLOW
                else -> Color.RED
            }
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(padding, padding, fillWidth, height - padding, cornerRadius, cornerRadius, paint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 120
        val desiredHeight = 60

        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }
}