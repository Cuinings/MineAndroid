package com.cn.core.ui.view.globalblur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View

class GlobalBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val blurRenderer = BlurRenderer(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var blurredBitmap: Bitmap? = null
    private var isUpdating = false
    private var autoUpdate = false
    private var updateInterval: Long = 100L
    
    private var screenCaptureHelper: ScreenCaptureHelper? = null
    private var captureRegion: Rect? = null
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (autoUpdate && visibility == VISIBLE) {
                updateBlur()
                mainHandler.postDelayed(this, updateInterval)
            }
        }
    }
    
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
    
    fun setBlurRadius(radius: Float) {
        blurRenderer.setBlurRadius(radius)
        invalidate()
    }
    
    fun setScaleFactor(factor: Float) {
        blurRenderer.setScaleFactor(factor)
        invalidate()
    }
    
    fun setCornerRadius(radius: Float) {
        blurRenderer.setCornerRadius(radius)
        invalidate()
    }
    
    fun setOverlayColor(color: Int) {
        blurRenderer.setOverlayColor(color)
        invalidate()
    }
    
    fun setScreenCaptureHelper(helper: ScreenCaptureHelper) {
        screenCaptureHelper = helper
    }
    
    fun setCaptureRegion(region: Rect?) {
        captureRegion = region
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
        
        val helper = screenCaptureHelper ?: return
        
        isUpdating = true
        
        val region = captureRegion ?: Rect(0, 0, width, height)
        
        helper.captureSingleFrame(region) { bitmap ->
            mainHandler.post {
                if (bitmap != null) {
                    updateBlurredBitmap(bitmap)
                }
                isUpdating = false
            }
        }
    }
    
    fun updateBlurredBitmap(sourceBitmap: Bitmap) {
        if (width <= 0 || height <= 0) {
            sourceBitmap.recycle()
            return
        }
        
        val newBlurredBitmap = blurRenderer.renderBlur(
            sourceBitmap, width, height
        )
        
        sourceBitmap.recycle()
        
        blurredBitmap?.recycle()
        blurredBitmap = newBlurredBitmap
        
        invalidate()
    }
    
    fun setBlurredBitmap(bitmap: Bitmap) {
        blurredBitmap?.recycle()
        blurredBitmap = bitmap
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val bitmap = blurredBitmap ?: return
        
        blurRenderer.drawBlurredBitmap(
            canvas,
            bitmap,
            0f, 0f, width.toFloat(), height.toFloat()
        )
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
        
        blurRenderer.release()
    }
    
    companion object {
        const val DEFAULT_UPDATE_INTERVAL = 100L
        const val DEFAULT_BLUR_RADIUS = 15f
        const val DEFAULT_SCALE_FACTOR = 0.25f
    }
}
