package com.cn.board.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

/**
 * 电量/充电状态监控。
 *
 * 用于壁纸渲染器的省电策略：
 * - 视频壁纸：低电量且未充电时，可由渲染器决定是否降帧/暂停（本框架默认保持播放）。
 * - 图片轮播：低电量未充电时自动拉长轮播间隔甚至暂停轮播（静态仍显示）。
 *
 * 通过 `Context.registerReceiver` **动态注册** `ACTION_BATTERY_CHANGED`（粘性广播），
 * 注册瞬间即回调一次当前状态，无需额外初始化。需在 [stop] 时反注册。
 */
class BatteryStateMonitor(private val context: Context) {

    var isCharging: Boolean = false
        private set

    var levelPercent: Int = 100
        private set

    private var callback: ((isCharging: Boolean, levelPercent: Int) -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            intent ?: return
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            levelPercent = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            Log.d(TAG, "battery: charging=$isCharging level=$levelPercent%")
            callback?.invoke(isCharging, levelPercent)
        }
    }

    fun start(callback: (isCharging: Boolean, levelPercent: Int) -> Unit) {
        this.callback = callback
        // 粘性广播注册后会立即收到一次当前状态
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    fun stop() {
        callback = null
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "BatteryStateMonitor"

        /** 低电量阈值（未充电且低于此值触发省电） */
        const val LOW_BATTERY_THRESHOLD = 20
    }
}
