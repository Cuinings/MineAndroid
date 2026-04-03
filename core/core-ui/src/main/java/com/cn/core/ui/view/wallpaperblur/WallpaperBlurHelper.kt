package com.cn.core.ui.view.wallpaperblur

import android.app.WallpaperManager
import android.content.Context
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
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class WallpaperBlurHelper(private val context: Context) {
    
    private val wallpaperManager = WallpaperManager.getInstance(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var renderScript: RenderScript? = null
    private var blurScript: ScriptIntrinsicBlur? = null
    
    private var blurRadius: Float = 15f
    private var scaleFactor: Float = 0.25f
    private var cornerRadius: Float = 0f
    private var overlayColor: Int = Color.TRANSPARENT
    
    private var wallpaperBitmap: Bitmap? = null
    private var lastBlurredBitmap: Bitmap? = null
    
    private var isUpdating = false
    private var autoUpdate = false
    private var updateInterval: Long = 100L
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (autoUpdate) {
                updateWallpaper()
                mainHandler.postDelayed(this, updateInterval)
            }
        }
    }
    
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
    
    fun setAutoUpdate(enabled: Boolean, interval: Long = 100L) {
        autoUpdate = enabled
        updateInterval = interval
        
        if (enabled) {
            startAutoUpdate()
        } else {
            stopAutoUpdate()
        }
    }
    
    fun captureWallpaper(): Bitmap? {
        return try {
            val wallpaperDrawable: Drawable? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.drawable
            } else {
                wallpaperManager.fastDrawable
            }
            
            when (wallpaperDrawable) {
                is BitmapDrawable -> {
                    wallpaperDrawable.bitmap?.copy(Bitmap.Config.ARGB_8888, true)
                }
                else -> {
                    val width = wallpaperDrawable?.intrinsicWidth ?: 1080
                    val height = wallpaperDrawable?.intrinsicHeight ?: 1920
                    
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
    
    fun updateWallpaper() {
        if (isUpdating) return
        isUpdating = true
        
        wallpaperBitmap?.recycle()
        wallpaperBitmap = captureWallpaper()
        
        isUpdating = false
    }
    
    fun getBlurredRegion(
        view: View,
        regionLeft: Int = 0,
        regionTop: Int = 0,
        regionRight: Int = view.width,
        regionBottom: Int = view.height
    ): Bitmap? {
        val wallpaper = wallpaperBitmap ?: captureWallpaper() ?: return null
        
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        
        val viewLeft = location[0] + regionLeft
        val viewTop = location[1] + regionTop
        val viewRight = location[0] + regionRight
        val viewBottom = location[1] + regionBottom
        
        val scaleX = wallpaper.width.toFloat() / screenWidth
        val scaleY = wallpaper.height.toFloat() / screenHeight
        
        val srcLeft = (viewLeft * scaleX).toInt().coerceAtLeast(0)
        val srcTop = (viewTop * scaleY).toInt().coerceAtLeast(0)
        val srcRight = (viewRight * scaleX).toInt().coerceAtMost(wallpaper.width)
        val srcBottom = (viewBottom * scaleY).toInt().coerceAtMost(wallpaper.height)
        
        if (srcRight <= srcLeft || srcBottom <= srcTop) {
            return null
        }
        
        val regionWidth = srcRight - srcLeft
        val regionHeight = srcBottom - srcTop
        
        val regionBitmap = Bitmap.createBitmap(wallpaper, srcLeft, srcTop, regionWidth, regionHeight)
        
        val blurredBitmap = applyBlur(regionBitmap)
        regionBitmap.recycle()
        
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
    
    fun drawBlurredRegion(
        canvas: Canvas,
        view: View,
        regionLeft: Int = 0,
        regionTop: Int = 0,
        regionRight: Int = view.width,
        regionBottom: Int = view.height
    ) {
        val blurredBitmap = getBlurredRegion(view, regionLeft, regionTop, regionRight, regionBottom)
            ?: return
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        
        val dstRect = RectF(0f, 0f, view.width.toFloat(), view.height.toFloat())
        
        if (cornerRadius > 0) {
            val path = Path().apply {
                addRoundRect(dstRect, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(path)
        }
        
        val srcRect = Rect(0, 0, blurredBitmap.width, blurredBitmap.height)
        canvas.drawBitmap(blurredBitmap, srcRect, dstRect, paint)
        
        if (cornerRadius > 0) {
            canvas.restore()
        }
        
        if (overlayColor != Color.TRANSPARENT) {
            if (cornerRadius > 0) {
                val path = Path().apply {
                    addRoundRect(dstRect, cornerRadius, cornerRadius, Path.Direction.CW)
                }
                canvas.drawPath(path, Paint().apply { color = overlayColor })
            } else {
                canvas.drawRect(dstRect, Paint().apply { color = overlayColor })
            }
        }
        
        blurredBitmap.recycle()
    }
    
    fun setupViewBlur(view: View) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                updateWallpaper()
                view.invalidate()
            }
        })
        
        view.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            if (right - left > 0 && bottom - top > 0) {
                view.invalidate()
            }
        }
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
        
        wallpaperBitmap?.recycle()
        wallpaperBitmap = null
        
        lastBlurredBitmap?.recycle()
        lastBlurredBitmap = null
        
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
