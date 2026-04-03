package com.cn.core.ui.view.globalblur

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast

class GlobalBlurHelper(private val activity: Activity) {
    
    private var blurService: GlobalBlurService? = null
    private var isServiceBound = false
    private var isProjectionReady = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as GlobalBlurService.LocalBinder
            blurService = binder.getService()
            isServiceBound = true
            
            if (!isProjectionReady) {
                requestMediaProjection()
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            blurService = null
            isServiceBound = false
        }
    }
    
    fun initialize() {
        if (!checkOverlayPermission()) {
            requestOverlayPermission()
            return
        }
        
        startAndBindService()
    }
    
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(activity)
        } else {
            true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }
    
    private fun startAndBindService() {
        val intent = Intent(activity, GlobalBlurService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(intent)
        } else {
            activity.startService(intent)
        }
        
        activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun requestMediaProjection() {
        val projectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        activity.startActivityForResult(intent, REQUEST_CODE_MEDIA_PROJECTION)
    }
    
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                if (checkOverlayPermission()) {
                    startAndBindService()
                } else {
                    Toast.makeText(activity, "需要悬浮窗权限才能使用全局模糊效果", Toast.LENGTH_SHORT).show()
                }
            }
            
            REQUEST_CODE_MEDIA_PROJECTION -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    blurService?.initMediaProjection(resultCode, data)
                    isProjectionReady = true
                    Toast.makeText(activity, "全局模糊服务已就绪", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "需要屏幕录制权限才能使用全局模糊效果", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    fun createBlurWindow(
        id: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        blurRadius: Float = 15f,
        cornerRadius: Float = 0f,
        overlayColor: Int = Color.TRANSPARENT,
        autoUpdate: Boolean = false,
        updateInterval: Long = 100L
    ): Boolean {
        return blurService?.createBlurWindow(
            id, x, y, width, height, blurRadius, cornerRadius, overlayColor, autoUpdate, updateInterval
        ) ?: false
    }
    
    fun updateBlurWindowPosition(id: String, x: Int, y: Int): Boolean {
        return blurService?.updateBlurWindowPosition(id, x, y) ?: false
    }
    
    fun updateBlurWindowSize(id: String, width: Int, height: Int): Boolean {
        return blurService?.updateBlurWindowSize(id, width, height) ?: false
    }
    
    fun updateBlurRadius(id: String, radius: Float): Boolean {
        return blurService?.updateBlurRadius(id, radius) ?: false
    }
    
    fun updateCornerRadius(id: String, radius: Float): Boolean {
        return blurService?.updateCornerRadius(id, radius) ?: false
    }
    
    fun updateOverlayColor(id: String, color: Int): Boolean {
        return blurService?.updateOverlayColor(id, color) ?: false
    }
    
    fun showBlurWindow(id: String): Boolean {
        return blurService?.showBlurWindow(id) ?: false
    }
    
    fun hideBlurWindow(id: String): Boolean {
        return blurService?.hideBlurWindow(id) ?: false
    }
    
    fun removeBlurWindow(id: String): Boolean {
        return blurService?.removeBlurWindow(id) ?: false
    }
    
    fun removeAllBlurWindows() {
        blurService?.removeAllBlurWindows()
    }
    
    fun isReady(): Boolean {
        return isServiceBound && isProjectionReady
    }
    
    fun release() {
        removeAllBlurWindows()
        
        if (isServiceBound) {
            activity.unbindService(serviceConnection)
            isServiceBound = false
        }
        
        val intent = Intent(activity, GlobalBlurService::class.java)
        activity.stopService(intent)
    }
    
    companion object {
        const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
        const val REQUEST_CODE_MEDIA_PROJECTION = 1002
    }
}
