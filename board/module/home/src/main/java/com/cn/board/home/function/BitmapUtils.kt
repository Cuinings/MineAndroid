package com.cn.board.home.function

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap

/**
 * Bitmap 工具方法。
 *
 * Phase 3: 抽取 SoftSynHandler / MainAppInitHandler / AppInfoData 中
 * 三处重复的 Drawable→Bitmap 转换逻辑。
 */
object BitmapUtils {

    /**
     * 从应用图标 Drawable 创建 Bitmap。
     */
    fun createAppIconBitmap(icon: Drawable): Bitmap {
        val config = if (icon.opacity != PixelFormat.OPAQUE) {
            Bitmap.Config.ARGB_8888
        } else {
            Bitmap.Config.RGB_565
        }
        return createBitmap(icon.intrinsicWidth, icon.intrinsicHeight, config).apply {
            val canvas = Canvas(this)
            icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            icon.draw(canvas)
        }
    }

    /**
     * 从包名加载应用图标 Bitmap。
     */
    fun loadAppIconBitmap(context: Context, packageName: String): Bitmap? {
        return try {
            val icon = context.packageManager.getApplicationIcon(packageName)
            createAppIconBitmap(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
