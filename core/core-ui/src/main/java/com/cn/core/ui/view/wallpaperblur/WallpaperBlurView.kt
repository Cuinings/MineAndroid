package com.cn.core.ui.view.wallpaperblur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.graphics.scale
import androidx.core.view.isVisible

class WallpaperBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    companion object {
        val TAG = WallpaperBlurView::class.simpleName
    }
    
    private val wallpaperBlurHelper = WallpaperBlurHelper(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var blurredBitmap: Bitmap? = null
    private var isUpdating = false
        set(value) { value.takeIf { it != field }?.let {
            field = it
            Log.d(TAG, "isUpdating: $field")
        } }
    private var autoUpdate = false
        set(value) { value.takeIf { it != field }?.let {
            field = it
            Log.d(TAG, "autoUpdate: $field")
        } }
    private var updateInterval: Long = 100L
        set(value) { value.takeIf { it != field }?.let {
            field = it
            Log.d(TAG, "updateInterval: $field")
        } }
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    
    private var cornerRadius: Float = 0f
    private var overlayColor: Int = Color.TRANSPARENT
    private var blurRadius: Float = 15f
    private var scaleFactor: Float = 0.25f
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (autoUpdate && isVisible) {
                updateBlur()
                mainHandler.postDelayed(this, updateInterval)
            }
        }
    }
    
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setupViewTreeObserver()
    }
    
    private fun setupViewTreeObserver() {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                updateBlur()
            }
        })
    }
    
    fun setBlurRadius(radius: Float) {
        blurRadius = radius.coerceIn(0f, 25f)
        wallpaperBlurHelper.setBlurRadius(blurRadius)
        updateBlur()
    }
    
    fun setScaleFactor(factor: Float) {
        scaleFactor = factor.coerceIn(0.1f, 1f)
        wallpaperBlurHelper.setScaleFactor(scaleFactor)
        updateBlur()
    }
    
    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        invalidate()
    }
    
    fun setOverlayColor(color: Int) {
        overlayColor = color
        invalidate()
    }
    
    fun setAutoUpdate(enabled: Boolean, interval: Long = 100L) {
        autoUpdate = enabled
        updateInterval = interval
        
        if (enabled) {
            startAutoUpdate()
        } else {
            stopAutoUpdate()
        }
    }
    
    fun updateBlur() {
        if (isUpdating || width <= 0 || height <= 0) return
        
        isUpdating = true
        
        try {
            val newBlurredBitmap = wallpaperBlurHelper.getBlurredRegion(this)
            
            newBlurredBitmap?.let { bitmap ->
                blurredBitmap?.recycle()
                blurredBitmap = bitmap
                invalidate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        isUpdating = false
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val bitmap = blurredBitmap ?: return
        
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        
        if (cornerRadius > 0) {
            drawRoundedBlur(canvas, bitmap, srcRect, dstRect)
        } else {
            drawNormalBlur(canvas, bitmap, srcRect, dstRect)
        }
        
        if (overlayColor != Color.TRANSPARENT) {
            drawOverlay(canvas, dstRect)
        }
    }
    
    private fun drawNormalBlur(
        canvas: Canvas,
        bitmap: Bitmap,
        srcRect: Rect,
        dstRect: RectF,
    ) {
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
    }
    
    private fun drawRoundedBlur(
        canvas: Canvas,
        bitmap: Bitmap,
        srcRect: Rect,
        dstRect: RectF,
    ) {
        val path = Path().apply {
            addRoundRect(dstRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        
        val saveCount = canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        canvas.restoreToCount(saveCount)
    }
    
    private fun drawOverlay(canvas: Canvas, dstRect: RectF) {
        val overlayPaint = Paint().apply { color = overlayColor }
        
        if (cornerRadius > 0) {
            val path = Path().apply {
                addRoundRect(dstRect, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            canvas.drawPath(path, overlayPaint)
        } else {
            canvas.drawRect(dstRect, overlayPaint)
        }
    }
    
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        
        if (visibility == VISIBLE && autoUpdate) {
            startAutoUpdate()
        } else {
            stopAutoUpdate()
        }
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE && autoUpdate) {
            startAutoUpdate()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAutoUpdate()
        release()
    }
    
    private fun startAutoUpdate() {
        mainHandler.removeCallbacks(updateRunnable)
        mainHandler.post(updateRunnable)
    }
    
    private fun stopAutoUpdate() {
        mainHandler.removeCallbacks(updateRunnable)
    }
    
    fun release() {
        stopAutoUpdate()
        
        blurredBitmap?.recycle()
        blurredBitmap = null
        
        wallpaperBlurHelper.release()
    }
}
