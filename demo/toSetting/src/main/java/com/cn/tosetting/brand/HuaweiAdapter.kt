package com.cn.tosetting.brand

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * 华为品牌适配器
 * 
 * 支持华为、荣耀等设备的设置页面跳转。
 * 
 * ## 支持的功能
 * - 权限设置：打开华为权限管理页面
 * - 自启动管理：打开华为启动管理页面
 * - 电池优化：打开华为省电管理页面
 * - 悬浮窗权限：打开华为悬浮窗管理页面
 * - 后台运行：打开华为受保护应用页面
 * 
 * ## 兼容性
 * - EMUI 8.0+
 * - Magic UI 2.0+
 * - HarmonyOS 2.0+
 */
object HuaweiAdapter : BrandAdapter {
    override val brandName = "华为"
    override val manufacturerKeywords = listOf("huawei")
    override val brandKeywords = listOf("huawei", "honor")

    override val supportedFeatures = setOf(
        BrandFeature.PERMISSION_SETTINGS,
        BrandFeature.AUTO_START,
        BrandFeature.BATTERY_OPTIMIZATION,
        BrandFeature.FLOATING_WINDOW,
        BrandFeature.BACKGROUND_SETTINGS
    )

    /**
     * 打开权限设置页面
     * 
     * 尝试打开华为权限管理器，如果失败则尝试备用Activity。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openPermissionSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("packageName", context.packageName)
                setClassName(
                    "com.huawei.systemmanager",
                    "com.huawei.permissionmanager.ui.MainActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("packageName", context.packageName)
                    setClassName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.addviewmonitor.AddViewMonitorActivity"
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
     * 打开自启动管理页面
     * 
     * 打开华为启动管理器，用于配置应用的自启动权限。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openAutoStartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
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
     * 打开华为省电管理页面，用于配置应用的电池优化策略。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
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
     * 打开华为悬浮窗管理页面，用于配置应用的悬浮窗权限。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openFloatingWindowSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.addviewmonitor.AddViewMonitorActivity"
                )
                putExtra("packageName", context.packageName)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 打开后台运行设置页面
     * 
     * 打开华为受保护应用页面，用于配置应用的后台运行权限。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openBackgroundSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
