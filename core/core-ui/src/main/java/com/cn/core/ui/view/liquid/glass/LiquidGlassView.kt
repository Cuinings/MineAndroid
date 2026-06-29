package com.cn.core.ui.view.liquid.glass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * @author: cn
 * @time: 2026/6/1 10:51
 * @history
 * @description:
 */
class LiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerRadius = 0f
    private var glassEffect: RenderEffect? = null

    init {
        setWillNotDraw(false)
        bgPaint.color = Color.argb(20, 255, 255, 255)
        boardPaint.style = Paint.Style.STROKE
        boardPaint.strokeWidth = 1f
        boardPaint.color = Color.argb(60, 255, 255, 255)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blur = RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
            val tint = RenderEffect.createColorFilterEffect(
                PorterDuffColorFilter(
                    Color.argb(30, 255, 255, 255),
                    PorterDuff.Mode.SRC_OVER
                )
            )
            glassEffect = RenderEffect.createChainEffect(blur, tint)
            setRenderEffect(glassEffect)
        }
    }

    fun setDynamicQuality(isScrolling: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isScrolling) {
                setRenderEffect(null)
            } else {
                setRenderEffect(glassEffect)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(
            0f, 0f,
            width.toFloat(), height.toFloat(),
            cornerRadius, cornerRadius,
            bgPaint
        )
        super.onDraw(canvas)

    }

}