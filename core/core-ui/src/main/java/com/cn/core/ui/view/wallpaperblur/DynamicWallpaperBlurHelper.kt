package com.cn.core.ui.view.wallpaperblur

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class DynamicWallpaperBlurHelper(private val context: Context) {
    
    private val wallpaperManager = WallpaperManager.getInstance(context)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var renderScript: RenderScript? = null
    private var blurScript: ScriptIntrinsicBlur? = null
    
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    
    private var blurRadius: Float = 15f
    private var scaleFactor: Float = 0.25f
    private var cornerRadius: Float = 0f
    private var overlayColor: Int = Color.TRANSPARENT
    
    private var isServiceBound = false
    
    init {
        initRenderScript()
        initDisplayMetrics()
    }
    
    private fun initRenderScript() {
        try {
            renderScript = RenderScript.create(context)
            blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun initDisplayMetrics() {
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        }
    }
    
    fun startProjectionService(activity: Activity, resultCode: Int, data: Intent): Boolean {
        return try {
            val serviceIntent = Intent(activity, MediaProjectionService::class.java).apply {
                action = MediaProjectionService.ACTION_START
                putExtra(MediaProjectionService.EXTRA_RESULT_CODE, resultCode)
                putExtra(MediaProjectionService.EXTRA_RESULT_DATA, data)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(serviceIntent)
            } else {
                activity.startService(serviceIntent)
            }
            
            isServiceBound = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun stopProjectionService() {
        if (isServiceBound) {
            val serviceIntent = Intent(context, MediaProjectionService::class.java).apply {
                action = MediaProjectionService.ACTION_STOP
            }
            context.startService(serviceIntent)
            isServiceBound = false
        }
    }
    
    fun captureScreen(callback: (Bitmap?) -> Unit) {
        val service = MediaProjectionService.getInstance()
        
        if (service == null || !service.isProjectionReady()) {
            callback(null)
            return
        }
        
        service.captureScreen(callback)
    }
    
    fun getStaticWallpaper(): Bitmap? {
        return try {
            val wallpaperDrawable: Drawable? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.drawable
            } else {
                @Suppress("DEPRECATION")
                wallpaperManager.fastDrawable
            }
            
            when (wallpaperDrawable) {
                is BitmapDrawable -> {
                    wallpaperDrawable.bitmap?.copy(Bitmap.Config.ARGB_8888, true)
                }
                else -> {
                    val width = wallpaperDrawable?.intrinsicWidth ?: screenWidth
                    val height = wallpaperDrawable?.intrinsicHeight ?: screenHeight
                    
                    val bitmap = createBitmap(width, height)
                    val canvas = Canvas(bitmap)
                    wallpaperDrawable?.setBounds(0, 0, width, height)
                    wallpaperDrawable?.draw(canvas)
                    bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getBlurredRegion(
        view: View,
        screenBitmap: Bitmap?,
        regionLeft: Int = 0,
        regionTop: Int = 0,
        regionRight: Int = view.width,
        regionBottom: Int = view.height
    ): Bitmap? {
        val sourceBitmap = screenBitmap ?: return null
        
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        
        val viewLeft = location[0] + regionLeft
        val viewTop = location[1] + regionTop
        val viewRight = location[0] + regionRight
        val viewBottom = location[1] + regionBottom
        
        val scaleX = sourceBitmap.width.toFloat() / screenWidth
        val scaleY = sourceBitmap.height.toFloat() / screenHeight
        
        val srcLeft = (viewLeft * scaleX).toInt().coerceAtLeast(0)
        val srcTop = (viewTop * scaleY).toInt().coerceAtLeast(0)
        val srcRight = (viewRight * scaleX).toInt().coerceAtMost(sourceBitmap.width)
        val srcBottom = (viewBottom * scaleY).toInt().coerceAtMost(sourceBitmap.height)
        
        if (srcRight <= srcLeft || srcBottom <= srcTop) {
            return null
        }
        
        val regionWidth = srcRight - srcLeft
        val regionHeight = srcBottom - srcTop
        
        val regionBitmap = Bitmap.createBitmap(sourceBitmap, srcLeft, srcTop, regionWidth, regionHeight)
        
        val blurredBitmap = applyBlur(regionBitmap)
        regionBitmap.recycle()
        
        return blurredBitmap
    }
    
    private fun applyBlur(bitmap: Bitmap): Bitmap {
        if (blurRadius <= 0f) {
            return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }
        
        if (renderScript == null || blurScript == null) {
            return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }
        
        val scaledWidth = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val scaledHeight = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)
        
        val scaledBitmap = bitmap.scale(scaledWidth, scaledHeight, false)
        
        return try {
            val input = Allocation.createFromBitmap(
                renderScript,
                scaledBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            val output = Allocation.createTyped(renderScript!!, input.type)
            
            blurScript?.setRadius(blurRadius)
            blurScript?.setInput(input)
            blurScript?.forEach(output)
            
            val result = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            output.copyTo(result)
            
            input.destroy()
            output.destroy()
            scaledBitmap.recycle()
            
            result
        } catch (e: Exception) {
            e.printStackTrace()
            scaledBitmap
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
    
    fun getScreenWidth(): Int = screenWidth
    fun getScreenHeight(): Int = screenHeight
    
    fun isProjectionReady(): Boolean {
        val service = MediaProjectionService.getInstance()
        return service != null && service.isProjectionReady()
    }
    
    fun release() {
        stopProjectionService()
        
        try {
            blurScript?.destroy()
        } catch (e: Exception) {}
        blurScript = null
        
        try {
            renderScript?.destroy()
        } catch (e: Exception) {}
        renderScript = null
    }
}
