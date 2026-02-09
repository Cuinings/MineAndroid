package com.cn.other.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class FrostedGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bitmap: Bitmap? = null
    private val rect = Rect()
    
    init {
        setBackgroundColor(Color.TRANSPARENT)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let {
            canvas.drawBitmap(it, null, rect, paint)
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0, 0, w, h)
        createFrostedGlassBitmap()
    }
    
    private fun createFrostedGlassBitmap() {
        if (width <= 0 || height <= 0) return
        
        try {
            val baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val baseCanvas = Canvas(baseBitmap)
            
            val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            blackPaint.color = Color.argb(25, 0, 0, 0) // 10% black
            baseCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), blackPaint)
            
            bitmap = createBlurredBitmap(baseBitmap, 50f)
            baseBitmap.recycle()
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createBlurredBitmap(source: Bitmap, blurPercentage: Float): Bitmap {
        val scaleFactor = 100f / blurPercentage
        val scaledWidth = (source.width / scaleFactor).toInt()
        val scaledHeight = (source.height / scaleFactor).toInt()
        
        var blurred = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        blurred = Bitmap.createScaledBitmap(blurred, source.width, source.height, true)
        return blurred
    }
    
    override fun isOpaque(): Boolean {
        return false
    }
}