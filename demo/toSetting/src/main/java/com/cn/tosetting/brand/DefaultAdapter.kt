package com.cn.tosetting.brand

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.os.Build

/**
 * 默认品牌适配器
 * 
 * 作为所有未识别品牌的默认适配器，提供Android标准API支持。
 * 
 * ## 支持的功能
 * - 悬浮窗权限：使用Android标准API打开悬浮窗权限设置（Android 6.0+）
 * 
 * ## 特点
 * - 匹配所有设备（match方法始终返回true）
 * - 仅使用Android标准API，不依赖厂商定制功能
 * - 作为适配器链的兜底方案
 */
object DefaultAdapter : BrandAdapter {
    override val brandName = "Default"
    override val manufacturerKeywords = emptyList<String>()
    override val brandKeywords = emptyList<String>()

    /**
     * 匹配所有设备
     * 
     * 默认适配器始终返回true，确保所有设备都能被匹配到。
     * 
     * @param manufacturer 设备制造商（未使用）
     * @param brand 设备品牌（未使用）
     * @return 始终返回 true
     */
    override fun match(manufacturer: String, brand: String): Boolean = true

    /**
     * 支持的功能集合
     * 
     * 根据Android版本动态返回支持的功能：
     * - Android 6.0+：支持悬浮窗权限设置
     * - Android 6.0以下：不支持任何功能
     */
    override val supportedFeatures: Set<BrandFeature>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setOf(BrandFeature.FLOATING_WINDOW)
        } else {
            emptySet()
        }

    /**
     * 打开悬浮窗权限设置页面
     * 
     * 使用Android标准API打开悬浮窗权限设置页面。
     * 仅支持Android 6.0（API 23）及以上版本。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    override fun openFloatingWindowSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
}
