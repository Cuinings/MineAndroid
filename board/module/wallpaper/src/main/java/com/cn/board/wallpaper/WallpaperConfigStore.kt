package com.cn.board.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.util.Log

/**
 * 壁纸配置存储（跨进程）。
 *
 * ## 为什么需要它
 * 壁纸服务运行在独立进程（AndroidManifest 中 `android:process=":wallpaper"`），
 * 而设置入口（[WallpaperSettingsActivity]）在主进程。两个进程之间：
 * - **持久化**：用 `SharedPreferences`（文件原子写）保存最新配置，服务启动/重建时读取。
 * - **实时通知**：`SharedPreferences.OnSharedPreferenceChangeListener` 在 Android 跨进程下
 *   不可靠，因此改用**应用内广播**（`setPackage` 限定本应用）触发服务重新加载。
 *
 * ## 使用
 * ```kotlin
 * // 设置端（主进程）
 * WallpaperConfigStore.save(context, WallpaperConfig.video(path))
 *
 * // 服务进程（:wallpaper）
 * val receiver = WallpaperConfigStore.createReceiver()   // 收到广播后自动 load + 回调
 * context.registerReceiver(receiver, IntentFilter(WallpaperConfigStore.ACTION_CONFIG_CHANGED))
 * ```
 */
object WallpaperConfigStore {

    const val ACTION_CONFIG_CHANGED = "com.cn.board.wallpaper.ACTION_CONFIG_CHANGED"

    private const val PREFS_NAME = "wallpaper_config"
    private const val KEY_TYPE = "type"
    private const val KEY_VIDEO_PATH = "video_path"
    private const val KEY_IMAGE_PATHS = "image_paths" // 逗号分隔
    private const val KEY_CAROUSEL_INTERVAL = "carousel_interval_ms"
    private const val KEY_PARALLAX = "parallax_enabled"
    private const val KEY_FPS_CAP = "fps_cap"

    private const val TAG = "WallpaperConfigStore"

    interface Listener {
        /** 配置发生变化（来自广播或主动 dispatch） */
        fun onConfigChanged(config: WallpaperConfig)
    }

    private val listeners = mutableSetOf<Listener>()

    /** 读取当前持久化配置；无配置时返回 [WallpaperConfig.DEFAULT] */
    fun load(context: Context): WallpaperConfig {
        val sp = prefs(context)
        val type = WallpaperType.fromName(sp.getString(KEY_TYPE, null))
        val videoPath = sp.getString(KEY_VIDEO_PATH, null)
        val imagePaths = sp.getString(KEY_IMAGE_PATHS, null)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val carouselIntervalMs = sp.getLong(KEY_CAROUSEL_INTERVAL, 5000L)
        val parallaxEnabled = sp.getBoolean(KEY_PARALLAX, true)
        val fpsCap = sp.getInt(KEY_FPS_CAP, 0)
        return WallpaperConfig(
            type = type,
            videoPath = videoPath,
            imagePaths = imagePaths,
            carouselIntervalMs = carouselIntervalMs,
            parallaxEnabled = parallaxEnabled,
            fpsCap = fpsCap,
        ).also { Log.d(TAG, "load: $it") }
    }

    /** 保存配置并广播给所有进程（含 :wallpaper） */
    fun save(context: Context, config: WallpaperConfig) {
        Log.d(TAG, "save: $config")
        prefs(context).edit().apply {
            putString(KEY_TYPE, config.type.name)
            putString(KEY_VIDEO_PATH, config.videoPath)
            putString(KEY_IMAGE_PATHS, config.imagePaths.joinToString(","))
            putLong(KEY_CAROUSEL_INTERVAL, config.carouselIntervalMs)
            putBoolean(KEY_PARALLAX, config.parallaxEnabled)
            putInt(KEY_FPS_CAP, config.fpsCap)
            apply()
        }
        // 仅发给本应用，避免泄漏到其他应用
        val intent = Intent(ACTION_CONFIG_CHANGED).setPackage(context.packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.sendBroadcast(intent, null)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.sendBroadcast(intent)
        }
    }

    fun subscribe(listener: Listener) {
        synchronized(listeners) { listeners.add(listener) }
    }

    fun unsubscribe(listener: Listener) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    /** 直接派发配置（同进程内使用，例如服务初始化时） */
    fun dispatch(config: WallpaperConfig) {
        synchronized(listeners) {
            listeners.toList().forEach { it.onConfigChanged(config) }
        }
    }

    /**
     * 创建一个广播接收器：收到配置变更广播后自动 [load] 并派发给订阅者。
     * 调用方负责 register / unregister。
     */
    fun createReceiver(): BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == ACTION_CONFIG_CHANGED) {
                dispatch(load(ctx))
            }
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
