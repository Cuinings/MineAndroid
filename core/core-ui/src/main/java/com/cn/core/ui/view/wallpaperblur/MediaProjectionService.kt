package com.cn.core.ui.view.wallpaperblur

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.nio.ByteBuffer

class MediaProjectionService : Service() {
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var screenDensity: Int = 0
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager: WindowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    private var captureCallback: ((Bitmap?) -> Unit)? = null
    private var isCapturing = false
    
    companion object {
        const val CHANNEL_ID = "MediaProjectionServiceChannel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "com.cn.core.ui.view.wallpaperblur.START_PROJECTION"
        const val ACTION_STOP = "com.cn.core.ui.view.wallpaperblur.STOP_PROJECTION"
        
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        
        private var instance: MediaProjectionService? = null
        
        fun getInstance(): MediaProjectionService? = instance
        
        fun isRunning(): Boolean = instance != null
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        initDisplayMetrics()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, android.content.Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                
                if (data != null) {
                    startProjection(resultCode, data)
                }
            }
            ACTION_STOP -> {
                stopProjection()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopProjection()
        instance = null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "动态壁纸模糊服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于捕获屏幕内容实现动态壁纸模糊效果"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("动态壁纸模糊")
            .setContentText("正在运行")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun initDisplayMetrics() {
        val displayMetrics = resources.displayMetrics
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
            screenDensity = displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
        }
    }
    
    private fun startProjection(resultCode: Int, data: android.content.Intent) {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
                as MediaProjectionManager
            
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopProjection()
                }
            }, mainHandler)
            
            startVirtualDisplay()
            
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }
    
    private fun startVirtualDisplay() {
        if (mediaProjection == null) return
        
        try {
            imageReader = ImageReader.newInstance(
                screenWidth, screenHeight,
                PixelFormat.RGBA_8888,
                2
            )
            
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "DynamicWallpaperCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                mainHandler
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun captureScreen(callback: (Bitmap?) -> Unit) {
        if (imageReader == null || isCapturing) {
            callback(null)
            return
        }
        
        isCapturing = true
        captureCallback = callback
        
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val bitmap = processImage(image)
                image.close()
                
                isCapturing = false
                captureCallback?.invoke(bitmap)
                captureCallback = null
            }
        }, mainHandler)
    }
    
    private fun processImage(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            
            bitmap.copyPixelsFromBuffer(buffer)
            
            if (rowPadding > 0) {
                val croppedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, screenWidth, screenHeight
                )
                bitmap.recycle()
                croppedBitmap
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun stopProjection() {
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
    }
    
    fun getScreenWidth(): Int = screenWidth
    fun getScreenHeight(): Int = screenHeight
    fun isProjectionReady(): Boolean = mediaProjection != null
}
