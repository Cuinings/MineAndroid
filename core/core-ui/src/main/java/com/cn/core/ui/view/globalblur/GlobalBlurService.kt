package com.cn.core.ui.view.globalblur

import android.app.Service
import android.content.Intent
import android.graphics.Rect
import android.os.Binder
import android.os.IBinder
import android.os.ResultReceiver

class GlobalBlurService : Service() {
    
    private val binder = LocalBinder()
    private lateinit var windowManager: GlobalBlurWindowManager
    private lateinit var screenCaptureHelper: ScreenCaptureHelper
    
    private var isInitialized = false
    private var isProjectionReady = false
    
    inner class LocalBinder : Binder() {
        fun getService(): GlobalBlurService = this@GlobalBlurService
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = GlobalBlurWindowManager(this)
        screenCaptureHelper = ScreenCaptureHelper(this)
        isInitialized = true
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeAllBlurWindows()
        screenCaptureHelper.release()
    }
    
    fun initMediaProjection(resultCode: Int, data: Intent): Boolean {
        val success = screenCaptureHelper.initMediaProjection(resultCode, data)
        isProjectionReady = success
        return success
    }
    
    fun createBlurWindow(
        id: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        blurRadius: Float = GlobalBlurView.DEFAULT_BLUR_RADIUS,
        cornerRadius: Float = 0f,
        overlayColor: Int = android.graphics.Color.TRANSPARENT,
        autoUpdate: Boolean = false,
        updateInterval: Long = GlobalBlurView.DEFAULT_UPDATE_INTERVAL
    ): Boolean {
        val success = windowManager.createBlurWindow(
            id, x, y, width, height, blurRadius, cornerRadius, overlayColor
        )
        
        if (success && autoUpdate) {
            setupAutoUpdate(id, x, y, width, height, updateInterval)
        }
        
        return success
    }
    
    private fun setupAutoUpdate(
        id: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        updateInterval: Long
    ) {
        if (!isProjectionReady) return
        
        val region = Rect(x, y, x + width, y + height)
        
        windowManager.updateBlurWindow(id, x, y, width, height, region, updateInterval)
    }
    
    fun updateBlurWindowPosition(id: String, x: Int, y: Int): Boolean {
        return windowManager.updateBlurWindowPosition(id, x, y)
    }
    
    fun updateBlurWindowSize(id: String, width: Int, height: Int): Boolean {
        return windowManager.updateBlurWindowSize(id, width, height)
    }
    
    fun updateBlurRadius(id: String, radius: Float): Boolean {
        return windowManager.updateBlurRadius(id, radius)
    }
    
    fun updateCornerRadius(id: String, radius: Float): Boolean {
        return windowManager.updateCornerRadius(id, radius)
    }
    
    fun updateOverlayColor(id: String, color: Int): Boolean {
        return windowManager.updateOverlayColor(id, color)
    }
    
    fun showBlurWindow(id: String): Boolean {
        return windowManager.showBlurWindow(id)
    }
    
    fun hideBlurWindow(id: String): Boolean {
        return windowManager.hideBlurWindow(id)
    }
    
    fun removeBlurWindow(id: String): Boolean {
        return windowManager.removeBlurWindow(id)
    }
    
    fun removeAllBlurWindows() {
        windowManager.removeAllBlurWindows()
    }
    
    fun getBlurWindowIds(): Set<String> {
        return windowManager.getBlurWindowIds()
    }
    
    fun hasBlurWindow(id: String): Boolean {
        return windowManager.hasBlurWindow(id)
    }
    
    fun isServiceInitialized(): Boolean = isInitialized
    
    fun isProjectionInitialized(): Boolean = isProjectionReady
    
    fun getScreenCaptureHelper(): ScreenCaptureHelper = screenCaptureHelper
    
    companion object {
        const val ACTION_START = "com.cn.core.ui.view.globalblur.START"
        const val ACTION_STOP = "com.cn.core.ui.view.globalblur.STOP"
        
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_WINDOW_ID = "window_id"
        const val EXTRA_X = "x"
        const val EXTRA_Y = "y"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_BLUR_RADIUS = "blur_radius"
        const val EXTRA_CORNER_RADIUS = "corner_radius"
        const val EXTRA_OVERLAY_COLOR = "overlay_color"
        const val EXTRA_AUTO_UPDATE = "auto_update"
        const val EXTRA_UPDATE_INTERVAL = "update_interval"
    }
}
