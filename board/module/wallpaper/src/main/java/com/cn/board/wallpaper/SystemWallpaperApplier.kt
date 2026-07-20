package com.cn.board.wallpaper

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 系统级壁纸挂载器（与「系统方案」一致的核心）。
 *
 * ## 为什么要它
 * 普通 App 只能拉起 [WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER] 系统 picker，
 * 让用户手动点「应用」。在具备 **system / 特权(priV) 身份** 时，可以直接调用隐藏 API
 * [WallpaperManager.setWallpaperComponent]，把本 Live Wallpaper 设为系统壁纸，
 * 实现「开机默认、无感切换、不依赖用户操作」——这与系统原生壁纸方案的挂载方式一致。
 *
 * ## 权限要求
 * `setWallpaperComponent` 标注 @hide，需 [android.Manifest.permission.SET_WALLPAPER_COMPONENT]
 * （AOSP 定义为 `signature|privileged`）。只要把本模块以 **priv-app** 身份装入
 * `/system/priv-app`（或随 ROM 用平台签名），并在 Manifest 声明该权限即可获得。
 *
 * ## 兼容性
 * 反射调用失败时（非系统构建 / 框架版本差异）自动降级为 picker，保证同一份代码两种构建都能跑。
 */
object SystemWallpaperApplier {

    private const val TAG = "SystemWallpaperApplier"

    /**
     * 把本服务设为系统动态壁纸。
     *
     * @param fallbackToPicker 无 system 权限时是否回退到系统 picker（默认 true）
     * @return true=已编程式挂载成功；false=走了 picker 降级或失败
     */
    @SuppressLint("MissingPermission", "PrivateApi")
    fun applyAsSystemWallpaper(context: Context, fallbackToPicker: Boolean = true): Boolean {
        val component = ComponentName(context, BoardWallpaperService::class.java)
        try {
            val wm = WallpaperManager.getInstance(context)
            // 隐藏 API：boolean setWallpaperComponent(ComponentName)
            val method = WallpaperManager::class.java
                .getMethod("setWallpaperComponent", ComponentName::class.java)
            val result = method.invoke(wm, component)
            val ok = result !is Boolean || result
            Log.i(TAG, "setWallpaperComponent invoked -> $result")
            if (ok) return true
        } catch (e: Exception) {
            // SecurityException(无权限) / NoSuchMethodException(框架差异) / 其他
            Log.w(TAG, "setWallpaperComponent unavailable, fallback to picker", e)
        }
        if (fallbackToPicker) {
            launchPicker(context)
        }
        return false
    }

    /** 无系统权限时回退：引导用户在系统 picker 中选择本 Live Wallpaper */
    private fun launchPicker(context: Context) {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, BoardWallpaperService::class.java),
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
