package com.cn.tosetting.brand

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * 三星品牌适配器
 * 
 * 支持三星设备的设置页面跳转。
 * 
 * ## 支持的功能
 * - 自启动管理：打开三星智能管理器应用管理页面
 * - 电池优化：打开三星智能管理器电池页面
 * - 悬浮窗权限：打开Android标准悬浮窗权限设置
 * 
 * ## 兼容性
 * - One UI 1.0+
 * - Android 7.0+
 */
object SamsungAdapter : BrandAdapter {
    override val brandName = "三星"
    override val manufacturerKeywords = listOf("samsung")
    override val brandKeywords = listOf("samsung")

    override val supportedFeatures = setOf(
        BrandFeature.AUTO_START,
        BrandFeature.BATTERY_OPTIMIZATION,
        BrandFeature.FLOATING_WINDOW
    )

    /**
     * 打开自启动管理页面
     * 
     * 打开三星智能管理器的应用管理页面，用于配置应用的自启动权限。
     * 支持中国版和国际版两个版本。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openAutoStartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.samsung.android.sm_cn",
                    "com.samsung.android.sm.ui.appmanager.AppManagerActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.sm",
                        "com.samsung.android.sm.ui.appmanager.AppManagerActivity"
                    )
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * 打开电池优化设置页面
     * 
     * 打开三星智能管理器的电池管理页面，用于配置应用的电池优化策略。
     * 支持中国版和国际版两个版本。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.samsung.android.sm_cn",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                    "com.samsung.android.sm",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * 打开悬浮窗权限设置页面
     * 
     * 打开Android标准的悬浮窗权限设置页面。
     * 三星设备使用标准Android API。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openFloatingWindowSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
