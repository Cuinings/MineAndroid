package com.cn.board.meet.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * @author: cn
 * @time: 2026/4/29 10:55
 * @history
 * @description:
 */
class PreciseTwoRectsGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 用于离屏绘制
    private var offscreenBitmap: Bitmap? = null
    private var offscreenCanvas: Canvas? = null

    // 矩形区域
    private val outerRect = RectF()
    private val innerRect = RectF()
    private val gradientRect = RectF()

    // 配置属性
    var borderGap: Float = 20f
        set(value) {
            field = value.coerceAtLeast(1f)
            updateRects()
            invalidate()
        }

    var startColor: Int = Color.parseColor("#FFFF0000") // 红色
    var endColor: Int = Color.parseColor("#FF00FF00")   // 绿色

    var outerStrokeWidth: Float = 4f
    var innerStrokeWidth: Float = 4f

    init {
        paint.style = Paint.Style.FILL

        outerPaint.style = Paint.Style.STROKE
        outerPaint.color = Color.RED
        outerPaint.strokeWidth = outerStrokeWidth

        innerPaint.style = Paint.Style.STROKE
        innerPaint.color = Color.GREEN
        innerPaint.strokeWidth = innerStrokeWidth

        // 启用硬件加速
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w <= 0 || h <= 0) return

        // 释放之前的Bitmap
        offscreenBitmap?.recycle()

        // 创建新的离屏Bitmap
        offscreenBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        offscreenCanvas = Canvas(offscreenBitmap!!)

        updateRects()
    }

    private fun updateRects() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // 计算矩形
        val halfOuterStroke = outerStrokeWidth / 2
        val halfInnerStroke = innerStrokeWidth / 2

        // 外矩形
        outerRect.set(
            halfOuterStroke, halfOuterStroke,
            viewWidth - halfOuterStroke, viewHeight - halfOuterStroke
        )

        // 内矩形
        innerRect.set(
            halfOuterStroke + borderGap + halfInnerStroke,
            halfOuterStroke + borderGap + halfInnerStroke,
            viewWidth - halfOuterStroke - borderGap - halfInnerStroke,
            viewHeight - halfOuterStroke - borderGap - halfInnerStroke
        )

        // 渐变矩形（正好在两个边框之间）
        gradientRect.set(
            halfOuterStroke + outerStrokeWidth,
            halfOuterStroke + outerStrokeWidth,
            viewWidth - halfOuterStroke - outerStrokeWidth,
            viewHeight - halfOuterStroke - outerStrokeWidth
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (offscreenCanvas == null) return

        val offscreen = offscreenCanvas ?: return

        // 清空离屏画布
        offscreen.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // 1. 先绘制渐变背景
        val gradient = LinearGradient(
            outerRect.left, outerRect.top,
            outerRect.right, outerRect.bottom,
            startColor, endColor,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        offscreen.drawRect(gradientRect, paint)
        paint.shader = null

        // 2. 保存图层
        val layer = offscreen.saveLayer(
            0f, 0f, width.toFloat(), height.toFloat(), null
        )

        // 3. 绘制内矩形并清除内部区域
        offscreen.drawRect(innerRect, paint.apply {
            color = Color.TRANSPARENT
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        })
        paint.xfermode = null

        // 恢复图层
        offscreen.restoreToCount(layer)

        // 4. 绘制外框和内框
        offscreen.drawRect(outerRect, outerPaint)
        offscreen.drawRect(innerRect, innerPaint)

        // 5. 将离屏内容绘制到主画布
        canvas.drawBitmap(offscreenBitmap!!, 0f, 0f, null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        offscreenBitmap?.recycle()
    }
}