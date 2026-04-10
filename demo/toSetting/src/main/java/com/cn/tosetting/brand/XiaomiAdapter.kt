package com.cn.tosetting.brand

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

/**
 * 小米品牌适配器
 * 
 * 支持小米、红米等设备的设置页面跳转。
 * 
 * ## 支持的功能
 * - 权限设置：打开MIUI权限管理页面
 * - 自启动管理：打开MIUI自启动管理页面
 * - 电池优化：打开神隐模式设置页面
 * - 悬浮窗权限：打开MIUI权限管理页面
 * - 后台运行：打开应用详情页面
 * 
 * ## 兼容性
 * - MIUI 10+
 * - Android 7.0+
 */
object XiaomiAdapter : BrandAdapter {
    override val brandName = "小米"
    override val manufacturerKeywords = listOf("xiaomi")
    override val brandKeywords = listOf("xiaomi", "redmi", "mi")

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
     * 尝试打开MIUI权限编辑器，如果失败则尝试备用Activity。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openPermissionSettings(context: Context): Boolean {
        return try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", context.packageName)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
                    )
                    putExtra("extra_pkgname", context.packageName)
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
     * 打开MIUI安全中心的自启动管理Activity。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openAutoStartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 打开电池优化设置页面
     * 
     * 打开MIUI神隐模式配置页面，用于设置应用的后台运行策略。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
                putExtra("package_name", context.packageName)
                putExtra("package_label", context.getString(context.applicationInfo.labelRes))
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 打开悬浮窗权限设置页面
     * 
     * 通过MIUI权限编辑器打开权限设置页面，
     * 用户可以在其中找到悬浮窗权限选项。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openFloatingWindowSettings(context: Context): Boolean {
        return try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", context.packageName)
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
     * 打开MIUI应用详情页面，用于配置应用的后台运行权限。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openBackgroundSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.appmanager.ApplicationsDetailsActivity"
                )
                putExtra("package_name", context.packageName)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
