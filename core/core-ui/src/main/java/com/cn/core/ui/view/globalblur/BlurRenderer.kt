package com.cn.core.ui.view.globalblur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.core.graphics.scale

class BlurRenderer(private val context: Context) {
    
    private var renderScript: RenderScript? = null
    private var blurScript: ScriptIntrinsicBlur? = null
    private var blurInput: Allocation? = null
    private var blurOutput: Allocation? = null
    
    private var blurRadius: Float = 15f
    private var scaleFactor: Float = 0.25f
    private var cornerRadius: Float = 0f
    private var overlayColor: Int = Color.TRANSPARENT
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    
    private val rectF = RectF()
    private var roundedPath: Path? = null
    
    private var lastBlurredBitmap: Bitmap? = null
    
    init {
        initRenderScript()
    }
    
    private fun initRenderScript() {
        try {
            renderScript = RenderScript.create(context)
            blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun setBlurRadius(radius: Float) {
        blurRadius = radius.coerceIn(0f, 25f)
    }
    
    fun setScaleFactor(factor: Float) {
        scaleFactor = factor.coerceIn(0.1f, 1f)
    }
    
    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
    }
    
    fun setOverlayColor(color: Int) {
        overlayColor = color
    }
    
    fun renderBlur(sourceBitmap: Bitmap?, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (sourceBitmap == null || targetWidth <= 0 || targetHeight <= 0) {
            return null
        }
        
        val scaledWidth = (sourceBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val scaledHeight = (sourceBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
        
        val scaledBitmap = sourceBitmap.scale(scaledWidth, scaledHeight, false)
        
        val blurredBitmap = applyBlur(scaledBitmap)
        scaledBitmap.recycle()
        
        lastBlurredBitmap?.recycle()
        lastBlurredBitmap = blurredBitmap
        
        return blurredBitmap
    }
    
    private fun applyBlur(bitmap: Bitmap): Bitmap {
        if (blurRadius <= 0f) {
            return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }
        
        if (renderScript == null || blurScript == null) {
            return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }
        
        return try {
            val input = Allocation.createFromBitmap(
                renderScript,
                bitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            val output = Allocation.createTyped(renderScript!!, input.type)
            
            blurScript?.setRadius(blurRadius)
            blurScript?.setInput(input)
            blurScript?.forEach(output)
            
            val result = Bitmap.createBitmap(
                bitmap.width, 
                bitmap.height, 
                bitmap.config ?: Bitmap.Config.ARGB_8888
            )
            output.copyTo(result)
            
            input.destroy()
            output.destroy()
            
            result
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }
    }
    
    fun drawBlurredBitmap(
        canvas: Canvas,
        bitmap: Bitmap?,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        if (bitmap == null || bitmap.isRecycled) return
        
        if (cornerRadius > 0) {
            drawRoundedBlur(canvas, bitmap, left, top, right, bottom)
        } else {
            drawNormalBlur(canvas, bitmap, left, top, right, bottom)
        }
        
        if (overlayColor != Color.TRANSPARENT) {
            rectF.set(left, top, right, bottom)
            if (cornerRadius > 0) {
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, 
                    Paint().apply { color = overlayColor })
            } else {
                canvas.drawRect(rectF, Paint().apply { color = overlayColor })
            }
        }
    }
    
    private fun drawNormalBlur(
        canvas: Canvas,
        bitmap: Bitmap,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = android.graphics.RectF(left, top, right, bottom)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
    }
    
    private fun drawRoundedBlur(
        canvas: Canvas,
        bitmap: Bitmap,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        if (roundedPath == null) {
            roundedPath = Path()
        }
        
        roundedPath?.rewind()
        roundedPath?.addRoundRect(
            left, top, right, bottom,
            cornerRadius, cornerRadius,
            Path.Direction.CW
        )
        
        val saveCount = canvas.save()
        canvas.clipPath(roundedPath!!)
        
        val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = android.graphics.RectF(left, top, right, bottom)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        
        canvas.restoreToCount(saveCount)
    }
    
    fun release() {
        lastBlurredBitmap?.recycle()
        lastBlurredBitmap = null
        
        try {
            blurInput?.destroy()
        } catch (e: Exception) {}
        blurInput = null
        
        try {
            blurOutput?.destroy()
        } catch (e: Exception) {}
        blurOutput = null
        
        try {
            blurScript?.destroy()
        } catch (e: Exception) {}
        blurScript = null
        
        try {
            renderScript?.destroy()
        } catch (e: Exception) {}
        renderScript = null
    }
    
    companion object {
        fun createBlurBitmap(
            context: Context,
            sourceBitmap: Bitmap,
            radius: Float,
            scale: Float = 0.25f
        ): Bitmap? {
            val renderer = BlurRenderer(context)
            renderer.setBlurRadius(radius)
            renderer.setScaleFactor(scale)
            
            val result = renderer.renderBlur(
                sourceBitmap, 
                sourceBitmap.width, 
                sourceBitmap.height
            )
            
            renderer.release()
            return result
        }
    }
}
