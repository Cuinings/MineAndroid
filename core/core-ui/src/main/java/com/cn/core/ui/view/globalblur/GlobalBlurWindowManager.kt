package com.cn.core.ui.view.globalblur

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class GlobalBlurWindowManager(private val context: Context) {
    
    private val windowManager: WindowManager = 
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    private val blurWindows = mutableMapOf<String, BlurWindowInfo>()
    
    data class BlurWindowInfo(
        val view: View,
        val params: WindowManager.LayoutParams,
        val blurView: GlobalBlurView
    )
    
    fun createBlurWindow(
        id: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        blurRadius: Float = 15f,
        cornerRadius: Float = 0f,
        overlayColor: Int = android.graphics.Color.TRANSPARENT
    ): Boolean {
        if (blurWindows.containsKey(id)) {
            return false
        }
        
        val blurView = GlobalBlurView(context).apply {
            setBlurRadius(blurRadius)
            setCornerRadius(cornerRadius)
            setOverlayColor(overlayColor)
        }
        
        val container = FrameLayout(context).apply {
            addView(blurView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }
        
        val params = createWindowParams(x, y, width, height)
        
        try {
            windowManager.addView(container, params)
            blurWindows[id] = BlurWindowInfo(container, params, blurView)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    @SuppressLint("WrongConstant")
    private fun createWindowParams(
        x: Int, 
        y: Int, 
        width: Int, 
        height: Int
    ): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            this.x = x
            this.y = y
        }
    }
    
    fun updateBlurWindowPosition(id: String, x: Int, y: Int): Boolean {
        val windowInfo = blurWindows[id] ?: return false
        
        windowInfo.params.x = x
        windowInfo.params.y = y
        
        try {
            windowManager.updateViewLayout(windowInfo.view, windowInfo.params)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    fun updateBlurWindowSize(id: String, width: Int, height: Int): Boolean {
        val windowInfo = blurWindows[id] ?: return false
        
        windowInfo.params.width = width
        windowInfo.params.height = height
        
        try {
            windowManager.updateViewLayout(windowInfo.view, windowInfo.params)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    fun updateBlurWindow(
        id: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        captureRegion: Rect?,
        updateInterval: Long
    ): Boolean {
        val windowInfo = blurWindows[id] ?: return false
        
        windowInfo.params.x = x
        windowInfo.params.y = y
        windowInfo.params.width = width
        windowInfo.params.height = height
        
        try {
            windowManager.updateViewLayout(windowInfo.view, windowInfo.params)
            
            windowInfo.blurView.setCaptureRegion(captureRegion)
            windowInfo.blurView.setAutoUpdate(true, updateInterval)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    fun updateBlurRadius(id: String, radius: Float): Boolean {
        val windowInfo = blurWindows[id] ?: return false
        windowInfo.blurView.setBlurRadius(radius)
        return true
    }
    
    fun updateCornerRadius(id: String, radius: Float): Boolean {
        val windowInfo = blurWindows[id] ?: return false
        windowInfo.blurView.setCornerRadius(radius)
        return true
    }
    
    fun updateOverlayColor(id: String, color: Int): Boolean {
        val windowInfo = blurWindows[id] ?: return false
        windowInfo.blurView.setOverlayColor(color)
        return true
    }
    
    fun showBlurWindow(id: String): Boolean {
        val windowInfo = blurWindows[id] ?: return false
        windowInfo.view.visibility = View.VISIBLE
        return true
    }
    
    fun hideBlurWindow(id: String): Boolean {
        val windowInfo = blurWindows[id] ?: return false
        windowInfo.view.visibility = View.INVISIBLE
        return true
    }
    
    fun removeBlurWindow(id: String): Boolean {
        val windowInfo = blurWindows[id] ?: return false
        
        try {
            windowManager.removeView(windowInfo.view)
            windowInfo.blurView.release()
            blurWindows.remove(id)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    fun removeAllBlurWindows() {
        blurWindows.keys.toList().forEach { id ->
            removeBlurWindow(id)
        }
    }
    
    fun getBlurWindowIds(): Set<String> {
        return blurWindows.keys.toSet()
    }
    
    fun hasBlurWindow(id: String): Boolean {
        return blurWindows.containsKey(id)
    }
}
