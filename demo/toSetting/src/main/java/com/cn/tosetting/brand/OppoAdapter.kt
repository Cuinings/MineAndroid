package com.cn.tosetting.brand

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * OPPO品牌适配器
 * 
 * 支持OPPO、Realme、一加等设备的设置页面跳转。
 * 
 * ## 支持的功能
 * - 权限设置：打开ColorOS权限管理页面
 * - 自启动管理：打开ColorOS自启动管理页面
 * - 电池优化：打开ColorOS电量管理页面
 * - 悬浮窗权限：打开ColorOS悬浮窗管理页面
 * - 后台运行：打开ColorOS自启动管理页面
 * 
 * ## 兼容性
 * - ColorOS 5.0+
 * - Android 7.0+
 */
object OppoAdapter : BrandAdapter {
    override val brandName = "OPPO"
    override val manufacturerKeywords = listOf("oppo")
    override val brandKeywords = listOf("oppo", "realme", "oneplus")

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
     * 尝试打开ColorOS权限管理器，如果失败则尝试备用Activity。
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
                    "com.color.safecenter",
                    "com.color.safecenter.permission.PermissionManagerActivity"
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
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.PermissionManagerActivity"
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
     * 打开ColorOS自启动管理页面，用于配置应用的自启动权限。
     * 支持多个版本的ColorOS。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openAutoStartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.startupapp.StartupAppListActivity"
                    )
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                try {
                    val intent = Intent().apply {
                        component = ComponentName(
                            "com.oppo.safe",
                            "com.oppo.safe.permission.startup.StartupAppListActivity"
                        )
                    }
                    context.startActivity(intent)
                    true
                } catch (e3: Exception) {
                    false
                }
            }
        }
    }

    /**
     * 打开电池优化设置页面
     * 
     * 打开ColorOS电量管理页面，用于配置应用的电池优化策略。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.coloros.oppoguardelf",
                        "com.coloros.powermanager.fuelgaue.PowerSaverModeActivity"
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
     * 打开ColorOS悬浮窗管理页面，用于配置应用的悬浮窗权限。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openFloatingWindowSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.sysfloatwindow.FloatWindowListActivity"
                )
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
     * 打开ColorOS自启动管理页面，用于配置应用的后台运行权限。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openBackgroundSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
