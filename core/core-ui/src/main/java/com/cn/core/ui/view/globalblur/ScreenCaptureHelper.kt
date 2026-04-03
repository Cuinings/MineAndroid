package com.cn.core.ui.view.globalblur

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import androidx.core.graphics.createBitmap
import java.nio.ByteBuffer

class ScreenCaptureHelper(private val context: Context) {
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics()
    
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var screenDensity: Int = 0
    
    private var captureCallback: ((Bitmap?) -> Unit)? = null
    private var captureRegion: Rect? = null
    private var isCapturing = false
    
    init {
        initDisplayMetrics()
    }
    
    private fun initDisplayMetrics() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
            screenDensity = context.resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
        }
    }
    
    fun initMediaProjection(resultCode: Int, data: android.content.Intent): Boolean {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        return mediaProjection != null
    }
    
    @SuppressLint("WrongConstant")
    fun startCapture(
        region: Rect? = null,
        callback: (Bitmap?) -> Unit
    ) {
        if (mediaProjection == null) {
            callback(null)
            return
        }
        
        captureCallback = callback
        captureRegion = region
        
        if (imageReader == null) {
            imageReader = ImageReader.newInstance(
                screenWidth, screenHeight,
                android.graphics.PixelFormat.RGBA_8888,
                2
            )
            
            imageReader?.setOnImageAvailableListener(
                { reader ->
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        processImage(image)
                        image.close()
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
        
        if (virtualDisplay == null) {
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                Handler(Looper.getMainLooper())
            )
        }
        
        isCapturing = true
    }
    
    private fun processImage(image: Image) {
        if (!isCapturing) return
        
        try {
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            val bitmap = createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight
            )
            
            bitmap.copyPixelsFromBuffer(buffer)
            
            val finalBitmap = if (captureRegion != null) {
                val region = captureRegion!!
                val croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    region.left.coerceAtLeast(0),
                    region.top.coerceAtLeast(0),
                    region.width().coerceAtMost(bitmap.width - region.left),
                    region.height().coerceAtMost(bitmap.height - region.top)
                )
                bitmap.recycle()
                croppedBitmap
            } else {
                bitmap
            }
            
            captureCallback?.invoke(finalBitmap)
            
        } catch (e: Exception) {
            e.printStackTrace()
            captureCallback?.invoke(null)
        }
    }
    
    fun captureSingleFrame(
        region: Rect? = null,
        callback: (Bitmap?) -> Unit
    ) {
        if (mediaProjection == null) {
            callback(null)
            return
        }
        
        startCapture(region) { bitmap ->
            stopCapture()
            callback(bitmap)
        }
    }
    
    fun stopCapture() {
        isCapturing = false
        captureCallback = null
        captureRegion = null
    }
    
    fun release() {
        stopCapture()
        
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
    }
    
    fun getScreenWidth(): Int = screenWidth
    
    fun getScreenHeight(): Int = screenHeight
    
    fun getScreenDensity(): Int = screenDensity
}
