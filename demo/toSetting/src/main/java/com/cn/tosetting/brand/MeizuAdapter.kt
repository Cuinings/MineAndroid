package com.cn.tosetting.brand

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 魅族品牌适配器
 * 
 * 支持魅族设备的设置页面跳转。
 * 
 * ## 支持的功能
 * - 权限设置：打开Flyme安全中心权限管理页面
 * - 自启动管理：打开Flyme安全中心自启动管理页面
 * 
 * ## 兼容性
 * - Flyme 6.0+
 * - Android 6.0+
 */
object MeizuAdapter : BrandAdapter {
    override val brandName = "魅族"
    override val manufacturerKeywords = listOf("meizu")
    override val brandKeywords = listOf("meizu")

    override val supportedFeatures = setOf(
        BrandFeature.PERMISSION_SETTINGS,
        BrandFeature.AUTO_START
    )

    /**
     * 打开权限设置页面
     * 
     * 打开Flyme安全中心的应用安全设置页面，
     * 用户可以在其中配置应用的各项权限。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openPermissionSettings(context: Context): Boolean {
        return try {
            val intent = Intent("com.meizu.safe.security.SHOW_APPSEC").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra("packageName", context.packageName)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 打开自启动管理页面
     * 
     * 打开Flyme安全中心的应用安全设置页面，
     * 用户可以在其中配置应用的自启动权限。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openAutoStartSettings(context: Context): Boolean {
        return try {
            val intent = Intent("com.meizu.safe.security.SHOW_APPSEC").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra("packageName", context.packageName)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
