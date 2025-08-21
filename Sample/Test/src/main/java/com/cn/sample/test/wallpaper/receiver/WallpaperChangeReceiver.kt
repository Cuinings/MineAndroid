package com.cn.sample.test.wallpaper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
// * @Author: CuiNing
// * @Time: 2025/8/20 10:00
 * @Description:
 */
// 广播接收器
class WallpaperChangeReceiver(private val action: () -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_WALLPAPER_CHANGED) {
            action.invoke()
            Log.i("WallpaperChangeReceiver", "onReceive: ${Intent.ACTION_WALLPAPER_CHANGED}")
        }
    }
}