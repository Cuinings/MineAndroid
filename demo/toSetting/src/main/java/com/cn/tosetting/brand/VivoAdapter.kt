package com.cn.tosetting.brand

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * VIVO品牌适配器
 * 
 * 支持VIVO、iQOO等设备的设置页面跳转。
 * 
 * ## 支持的功能
 * - 权限设置：打开FuntouchOS权限管理页面
 * - 自启动管理：打开FuntouchOS自启动管理页面
 * - 电池优化：打开FuntouchOS省电管理页面
 * - 悬浮窗权限：打开FuntouchOS悬浮窗管理页面
 * - 后台运行：打开FuntouchOS后台管理页面
 * 
 * ## 兼容性
 * - FuntouchOS 9.0+
 * - OriginOS 1.0+
 * - Android 7.0+
 */
object VivoAdapter : BrandAdapter {
    override val brandName = "VIVO"
    override val manufacturerKeywords = listOf("vivo")
    override val brandKeywords = listOf("vivo", "iqoo")

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
     * 尝试打开FuntouchOS权限管理器，如果失败则尝试iQOO备用Activity。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openPermissionSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("packagename", context.packageName)
                setClassName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("packagename", context.packageName)
                    setClassName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.safeguard.SoftPermissionDetailActivity"
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
     * 打开FuntouchOS后台启动管理页面，用于配置应用的自启动权限。
     * 支持VIVO和iQOO两个版本。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openAutoStartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                    )
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                try {
                    val intent = Intent().apply {
                        component = ComponentName(
                            "com.iqoo.secure",
                            "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
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
     * 打开FuntouchOS省电管理页面，用于配置应用的电池优化策略。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.vivo.abe",
                    "com.vivo.applicationabstractservice.MainPowerSavingActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.iqoo.powersaving",
                        "com.iqoo.powersaving.PowerSavingManagerActivity"
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
     * 打开iQOO悬浮窗管理页面，用于配置应用的悬浮窗权限。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openFloatingWindowSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.FloatWindowManager"
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
     * 打开FuntouchOS后台启动管理页面，用于配置应用的后台运行权限。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openBackgroundSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
