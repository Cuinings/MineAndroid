package com.cn.board.meet.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

/**
 * @author: cn
 * @time: 2026/4/9 13:44
 * @history
 * @description:
 */
class WallpaperCropper(private val context: Context) {

    companion object {
        // 壁纸缩放模式
        const val FIT_CENTER = 0
        const val CENTER_CROP = 1
        const val CENTER_INSIDE = 2
        const val FIT_XY = 3
    }

    // 缓存机制
    private data class CropKey(
        val viewLeft: Int,
        val viewTop: Int,
        val viewWidth: Int,
        val viewHeight: Int,
        val scaleMode: Int
    )

    // 使用LruCache来管理缓存，自动处理内存限制
    private val cropCache = object : LruCache<CropKey, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: CropKey, value: Bitmap): Int {
            // 返回Bitmap的内存大小（以KB为单位）
            return value.byteCount / 1024
        }
        
        override fun entryRemoved(evicted: Boolean, key: CropKey?, oldValue: Bitmap?, newValue: Bitmap?) {
            // 当缓存项被移除时，回收Bitmap资源
            oldValue?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
        }
    }
    private var lastWallpaperHash: Int? = null
    private var lastScreenSize: Pair<Int, Int>? = null

    /**
     * 从壁纸中裁剪View对应的区域
     */
    fun cropWallpaperForView(
        wallpaperBitmap: Bitmap,
        viewRect: Rect,
        scaleMode: Int = CENTER_CROP
    ): Bitmap? {
        if (wallpaperBitmap.isRecycled) return null
        
        try {
            // 检查缓存是否有效
            val screenRect = getScreenRect()
            val currentScreenSize = Pair(screenRect.width(), screenRect.height())
            val wallpaperHash = wallpaperBitmap.hashCode()
            
            // 如果壁纸或屏幕尺寸变化，清理缓存
            if (wallpaperHash != lastWallpaperHash || currentScreenSize != lastScreenSize) {
                clearCache()
                lastWallpaperHash = wallpaperHash
                lastScreenSize = currentScreenSize
            }
            
            // 生成缓存键（使用更精确的坐标，以10像素为单位，减少缓存项数量）
            val cropKey = CropKey(
                (viewRect.left / 10) * 10,
                (viewRect.top / 10) * 10,
                viewRect.width(),
                viewRect.height(),
                scaleMode
            )
            
            // 检查缓存
            cropCache[cropKey]?.let { cachedBitmap ->
                if (!cachedBitmap.isRecycled) {
                    return cachedBitmap
                }
            }
            
            // 3. 计算壁纸在屏幕上的显示区域
            val wallpaperDisplayRect = calculateWallpaperDisplayRect(
                wallpaperBitmap,
                screenRect,
                scaleMode
            )

            // 4. 将View坐标映射到壁纸Bitmap坐标
            val cropRect = mapViewRectToWallpaperRect(
                viewRect,
                wallpaperDisplayRect,
                wallpaperBitmap
            )

            // 5. 执行裁剪
            val result = cropBitmap(wallpaperBitmap, cropRect)
            
            // 缓存结果
            result?.let {
                cropCache.put(cropKey, it)
            }
            
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        // LruCache的entryRemoved方法会自动回收Bitmap资源
        cropCache.evictAll()
    }

    /**
     * 获取View在屏幕中的位置（包括状态栏）
     */
    private fun getViewScreenRect(view: View): Rect {
        val location = IntArray(2)
        view.getLocationOnScreen(location)

        return Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )
    }

    /**
     * 获取屏幕尺寸（不包括虚拟导航栏）
     */
    private fun getScreenRect(): Rect {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size) // 获取实际屏幕尺寸

        return Rect(0, 0, size.x, size.y)
    }

    /**
     * 获取屏幕尺寸（包括状态栏）
     */
    private fun getScreenRectWithStatusBar(): Rect {
        val resources = context.resources
        val displayMetrics = resources.displayMetrics
        val statusBarHeight = getStatusBarHeight()

        return Rect(
            0,
            statusBarHeight,
            displayMetrics.widthPixels,
            displayMetrics.heightPixels
        )
    }

    /**
     * 获取状态栏高度
     */
    @SuppressLint("InternalInsetResource")
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * 计算壁纸在屏幕上的显示区域
     */
    private fun calculateWallpaperDisplayRect(
        wallpaper: Bitmap,
        screenRect: Rect,
        scaleMode: Int
    ): RectF {
        val screenWidth = screenRect.width().toFloat()
        val screenHeight = screenRect.height().toFloat()
        val wallpaperWidth = wallpaper.width.toFloat()
        val wallpaperHeight = wallpaper.height.toFloat()

        return when (scaleMode) {
            FIT_CENTER -> {
                // 保持宽高比，居中显示
                val scale = minOf(
                    screenWidth / wallpaperWidth,
                    screenHeight / wallpaperHeight
                )
                val scaledWidth = wallpaperWidth * scale
                val scaledHeight = wallpaperHeight * scale
                val left = (screenWidth - scaledWidth) / 2
                val top = (screenHeight - scaledHeight) / 2

                RectF(left, top, left + scaledWidth, top + scaledHeight)
            }

            CENTER_CROP -> {
                // 保持宽高比，填充屏幕
                val scale = maxOf(
                    screenWidth / wallpaperWidth,
                    screenHeight / wallpaperHeight
                )
                val scaledWidth = wallpaperWidth * scale
                val scaledHeight = wallpaperHeight * scale
                val left = (screenWidth - scaledWidth) / 2
                val top = (screenHeight - scaledHeight) / 2

                RectF(left, top, left + scaledWidth, top + scaledHeight)
            }

            CENTER_INSIDE -> {
                // 保持宽高比，完整显示
                val scale = minOf(
                    screenWidth / wallpaperWidth,
                    screenHeight / wallpaperHeight
                ).coerceAtMost(1f)
                val scaledWidth = wallpaperWidth * scale
                val scaledHeight = wallpaperHeight * scale
                val left = (screenWidth - scaledWidth) / 2
                val top = (screenHeight - scaledHeight) / 2

                RectF(left, top, left + scaledWidth, top + scaledHeight)
            }

            FIT_XY -> {
                // 拉伸填满屏幕
                RectF(0f, 0f, screenWidth, screenHeight)
            }

            else -> RectF(0f, 0f, screenWidth, screenHeight)
        }
    }

    /**
     * 将View坐标映射到壁纸Bitmap坐标
     */
    private fun mapViewRectToWallpaperRect(
        viewRect: Rect,
        wallpaperDisplayRect: RectF,
        wallpaper: Bitmap
    ): Rect {
        // 边界检查
        if (wallpaperDisplayRect.width() <= 0f || wallpaperDisplayRect.height() <= 0f) {
            return Rect(0, 0, wallpaper.width, wallpaper.height)
        }

        // 计算缩放比例
        val scaleX = wallpaper.width / wallpaperDisplayRect.width()
        val scaleY = wallpaper.height / wallpaperDisplayRect.height()
        
        // 计算偏移量
        val offsetX = -wallpaperDisplayRect.left
        val offsetY = -wallpaperDisplayRect.top

        // 映射坐标 - 使用分别的缩放比例以保持正确的比例
        val left = ((viewRect.left + offsetX) * scaleX).roundToInt()
        val top = ((viewRect.top + offsetY) * scaleY).roundToInt()
        val right = ((viewRect.right + offsetX) * scaleX).roundToInt()
        val bottom = ((viewRect.bottom + offsetY) * scaleY).roundToInt()

        // 确保坐标在Bitmap范围内
        return Rect(
            left.coerceIn(0, wallpaper.width - 1),
            top.coerceIn(0, wallpaper.height - 1),
            right.coerceIn(left + 1, wallpaper.width),
            bottom.coerceIn(top + 1, wallpaper.height)
        )
    }

    /**
     * 执行裁剪
     */
    private fun cropBitmap(source: Bitmap, cropRect: Rect): Bitmap? {
        if (cropRect.width() <= 0 || cropRect.height() <= 0 || source.isRecycled) {
            return null
        }

        return try {
            val result = Bitmap.createBitmap(source, cropRect.left, cropRect.top,
                cropRect.width(), cropRect.height())
            // 检查裁剪结果是否有效
            if (result == null || result.isRecycled) {
                null
            } else {
                result
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 高级功能：获取View在屏幕中的可见区域
     */
    fun getVisibleViewRect(view: View, parentActivity: Activity? = null): Rect {
        val screenRect = getScreenRect()
        val viewRect = getViewScreenRect(view)

        // 考虑状态栏
        val statusBarHeight = getStatusBarHeight()
        screenRect.top = statusBarHeight

        // 考虑虚拟导航栏
        val navigationBarHeight = getNavigationBarHeight()
        screenRect.bottom -= navigationBarHeight

        // 如果View的父Activity不为空，考虑Activity的窗口位置
        parentActivity?.let { activity ->
            val window = activity.window
            val decorView = window.decorView

            val windowRect = Rect()
            decorView.getWindowVisibleDisplayFrame(windowRect)
            screenRect.top = windowRect.top
            screenRect.bottom = windowRect.bottom
        }

        // 计算View在屏幕中的可见部分
        val visibleRect = Rect()
        visibleRect.left = viewRect.left.coerceAtLeast(screenRect.left)
        visibleRect.top = viewRect.top.coerceAtLeast(screenRect.top)
        visibleRect.right = viewRect.right.coerceAtMost(screenRect.right)
        visibleRect.bottom = viewRect.bottom.coerceAtMost(screenRect.bottom)

        return visibleRect
    }

    /**
     * 获取导航栏高度
     */
    private fun getNavigationBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier(
            "navigation_bar_height", "dimen", "android"
        )
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * 扩展功能：为裁剪区域添加边框
     */
    fun addBorderToCroppedBitmap(
        bitmap: Bitmap,
        borderWidth: Int = 2,
        borderColor: Int = Color.WHITE
    ): Bitmap {
        val borderedBitmap = Bitmap.createBitmap(
            bitmap.width + borderWidth * 2,
            bitmap.height + borderWidth * 2,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(borderedBitmap)
        val paint = Paint().apply {
            color = borderColor
            style = Paint.Style.FILL
        }

        // 绘制边框
        canvas.drawRect(
            0f, 0f,
            borderedBitmap.width.toFloat(), borderedBitmap.height.toFloat(),
            paint
        )

        // 绘制原图
        canvas.drawBitmap(
            bitmap,
            borderWidth.toFloat(),
            borderWidth.toFloat(),
            null
        )

        return borderedBitmap
    }

    /**
     * 扩展功能：裁剪圆角图片
     */
    fun cropRoundedBitmap(source: Bitmap, cropRect: Rect, cornerRadius: Float): Bitmap {
        val croppedBitmap = Bitmap.createBitmap(
            source, cropRect.left, cropRect.top,
            cropRect.width(), cropRect.height()
        )

        val roundedBitmap = Bitmap.createBitmap(
            croppedBitmap.width,
            croppedBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(roundedBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val rect = RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(croppedBitmap, 0f, 0f, paint)

        croppedBitmap.recycle()
        return roundedBitmap
    }
}