package com.cn.board.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import java.io.File

object WallpaperConfigStore {

    const val ACTION_CONFIG_CHANGED = "com.cn.board.wallpaper.CONFIG_CHANGED"

    private const val PREFS_NAME = "board_wallpaper_config"
    private const val KEY_VIDEO_PATH = "video_path"

    private const val TAG = "WallpaperConfigStore"

    interface Listener {
        fun onConfigChanged(config: WallpaperConfig)
    }

    private val listeners = mutableListOf<Listener>()

    fun load(context: Context): WallpaperConfig {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val videoPath = sp.getString(KEY_VIDEO_PATH, "/storage/emulated/0/Download/wallpaper_dy_1.mp4")
        return WallpaperConfig(videoPath = videoPath)
    }

    fun save(context: Context, config: WallpaperConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_VIDEO_PATH, config.videoPath)
            .apply()
        val intent = Intent(ACTION_CONFIG_CHANGED).setPackage(context.packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.sendBroadcast(intent, null)
        } else {
            context.sendBroadcast(intent)
        }
    }

    fun createReceiver(): BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_CONFIG_CHANGED && context != null) {
                val config = load(context)
                synchronized(listeners) { listeners.forEach { it.onConfigChanged(config) } }
            }
        }
    }

    fun subscribe(listener: Listener) { synchronized(listeners) { listeners.add(listener) } }
    fun unsubscribe(listener: Listener) { synchronized(listeners) { listeners.remove(listener) } }

    fun copyToInternal(context: Context, externalPath: String): String {
        val src = File(externalPath)
        if (!src.exists() || !src.canRead()) {
            Log.w(TAG, "copyToInternal: source not readable: $externalPath")
            return externalPath
        }
        val dir = File(context.filesDir, "wallpaper_media")
        if (!dir.exists()) dir.mkdirs()
        val dst = File(dir, src.name)
        return try {
            if (!dst.exists() || dst.length() != src.length()) {
                src.copyTo(dst, overwrite = true)
                Log.i(TAG, "copyToInternal: $externalPath → ${dst.absolutePath}")
            }
            dst.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "copyToInternal failed: $externalPath", e)
            externalPath
        }
    }
}
