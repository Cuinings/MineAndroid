package com.cn.core.utils

import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.sqrt

/**
 * 屏幕工具类
 */
object ScreenUtil {

    /**
     * 获取屏幕宽度
     */
    fun getScreenWidth(context: Context): Int {
        return getScreenSize(context).x
    }

    /**
     * 获取屏幕高度
     */
    fun getScreenHeight(context: Context): Int {
        return getScreenSize(context).y
    }

    /**
     * 获取屏幕尺寸
     */
    fun getScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)
        return point
    }

    /**
     * 获取屏幕密度
     */
    fun getScreenDensity(context: Context): Float {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.density
    }

    /**
     * 获取屏幕密度DPI
     */
    fun getScreenDensityDpi(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.densityDpi
    }

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(context: Context): Int {
        var statusBarHeight = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

    /**
     * 获取导航栏高度
     */
    fun getNavigationBarHeight(context: Context): Int {
        var navigationBarHeight = 0
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            navigationBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return navigationBarHeight
    }

    /**
     * 将dp转换为px
     */
    fun dp2px(context: Context, dp: Float): Int {
        val density = getScreenDensity(context)
        return (dp * density + 0.5f).toInt()
    }

    /**
     * 将px转换为dp
     */
    fun px2dp(context: Context, px: Float): Int {
        val density = getScreenDensity(context)
        return (px / density + 0.5f).toInt()
    }

    /**
     * 将sp转换为px
     */
    fun sp2px(context: Context, sp: Float): Int {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return (sp * scaledDensity + 0.5f).toInt()
    }

    /**
     * 将px转换为sp
     */
    fun px2sp(context: Context, px: Float): Int {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return (px / scaledDensity + 0.5f).toInt()
    }

    /**
     * 获取屏幕方向
     */
    fun getScreenOrientation(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        return display.rotation
    }

    /**
     * 检查屏幕是否为横屏
     */
    fun isLandscape(context: Context): Boolean {
        val screenWidth = getScreenWidth(context)
        val screenHeight = getScreenHeight(context)
        return screenWidth > screenHeight
    }

    /**
     * 检查屏幕是否为竖屏
     */
    fun isPortrait(context: Context): Boolean {
        return !isLandscape(context)
    }

    /**
     * 获取屏幕对角线长度（英寸）
     */
    fun getScreenInches(context: Context): Double {
        val screenWidth = getScreenWidth(context)
        val screenHeight = getScreenHeight(context)
        val diagonalPixels = sqrt((screenWidth * screenWidth + screenHeight * screenHeight).toDouble())
        val densityDpi = getScreenDensityDpi(context)
        return diagonalPixels / densityDpi
    }

    /**
     * 获取屏幕分辨率
     */
    fun getScreenResolution(context: Context): String {
        val screenWidth = getScreenWidth(context)
        val screenHeight = getScreenHeight(context)
        return "$screenWidth x $screenHeight"
    }
}