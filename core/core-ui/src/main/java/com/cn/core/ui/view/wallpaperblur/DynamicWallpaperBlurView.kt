package com.cn.core.ui.view.wallpaperblur

import android.app.Activity
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
import android.view.View
import android.view.ViewTreeObserver

class DynamicWallpaperBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var blurHelper: DynamicWallpaperBlurHelper? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var blurredBitmap: Bitmap? = null
    private var isUpdating = false
    private var autoUpdate = false
    private var updateInterval: Long = 100L
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    
    private var cornerRadius: Float = 0f
    private var overlayColor: Int = Color.TRANSPARENT
    private var blurRadius: Float = DEFAULT_BLUR_RADIUS
    private var scaleFactor: Float = DEFAULT_SCALE_FACTOR
    
    private var isInitialized = false
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (autoUpdate && visibility == VISIBLE && isInitialized) {
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
                if (isInitialized) {
                    updateBlur()
                }
            }
        })
    }
    
    fun initialize(activity: Activity, resultCode: Int, data: android.content.Intent): Boolean {
        blurHelper = DynamicWallpaperBlurHelper(context)
        
        val serviceStarted = blurHelper?.startProjectionService(activity, resultCode, data) ?: false
        
        if (serviceStarted) {
            setBlurRadius(blurRadius)
            setScaleFactor(scaleFactor)
            
            isInitialized = true
            return true
        }
        
        return false
    }
    
    fun setBlurRadius(radius: Float) {
        blurRadius = radius.coerceIn(0f, 25f)
        blurHelper?.setBlurRadius(blurRadius)
        if (isInitialized) {
            updateBlur()
        }
    }
    
    fun setScaleFactor(factor: Float) {
        scaleFactor = factor.coerceIn(0.1f, 1f)
        blurHelper?.setScaleFactor(scaleFactor)
        if (isInitialized) {
            updateBlur()
        }
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
        
        if (enabled && isInitialized) {
            startAutoUpdate()
        } else {
            stopAutoUpdate()
        }
    }
    
    fun updateBlur() {
        if (!isInitialized || isUpdating || width <= 0 || height <= 0) return
        
        val helper = blurHelper ?: return
        
        isUpdating = true
        
        helper.captureScreen { screenBitmap ->
            mainHandler.post {
                screenBitmap?.let { bitmap ->
                    val newBlurredBitmap = helper.getBlurredRegion(this, bitmap)
                    
                    newBlurredBitmap?.let { blurred ->
                        blurredBitmap?.recycle()
                        blurredBitmap = blurred
                        invalidate()
                    }
                    
                    bitmap.recycle()
                }
                
                isUpdating = false
            }
        }
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
        dstRect: RectF
    ) {
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
    }
    
    private fun drawRoundedBlur(
        canvas: Canvas,
        bitmap: Bitmap,
        srcRect: Rect,
        dstRect: RectF
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
        
        if (visibility == VISIBLE && autoUpdate && isInitialized) {
            startAutoUpdate()
        } else {
            stopAutoUpdate()
        }
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE && autoUpdate && isInitialized) {
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
        
        blurHelper?.release()
        blurHelper = null
        
        isInitialized = false
    }
    
    fun isReady(): Boolean = isInitialized && blurHelper?.isProjectionReady() == true
    
    companion object {
        const val DEFAULT_BLUR_RADIUS = 15f
        const val DEFAULT_SCALE_FACTOR = 0.25f
        const val DEFAULT_UPDATE_INTERVAL = 100L
    }
}
