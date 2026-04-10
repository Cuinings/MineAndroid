package com.cn.tosetting.brand

import android.content.Context

/**
 * 手机品牌适配器接口
 * 
 * 用于定义不同手机品牌的设置页面跳转逻辑。
 * 每个品牌可以实现此接口来提供品牌特定的设置跳转功能。
 * 
 * ## 使用示例
 * ```kotlin
 * object NewBrandAdapter : BrandAdapter {
 *     override val brandName = "新品牌"
 *     override val manufacturerKeywords = listOf("newbrand")
 *     override val brandKeywords = listOf("newbrand")
 *     
 *     override fun openAutoStartSettings(context: Context): Boolean {
 *         // 实现跳转逻辑
 *     }
 * }
 * ```
 * 
 * @see BrandAdapterManager
 * @see BrandFeature
 */
interface BrandAdapter {
    
    /**
     * 品牌名称
     * 用于显示和日志记录
     */
    val brandName: String
    
    /**
     * 制造商关键词列表
     * 用于匹配 Build.MANUFACTURER
     */
    val manufacturerKeywords: List<String>
    
    /**
     * 品牌关键词列表
     * 用于匹配 Build.BRAND
     */
    val brandKeywords: List<String>

    /**
     * 检测当前设备是否匹配此品牌
     * 
     * @param manufacturer 设备制造商 (Build.MANUFACTURER)
     * @param brand 设备品牌 (Build.BRAND)
     * @return 如果匹配返回 true，否则返回 false
     */
    fun match(manufacturer: String, brand: String): Boolean {
        val manufacturerLower = manufacturer.lowercase()
        val brandLower = brand.lowercase()
        return manufacturerKeywords.any { manufacturerLower.contains(it) } ||
                brandKeywords.any { brandLower.contains(it) }
    }

    /**
     * 打开权限设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openPermissionSettings(context: Context): Boolean = false
    
    /**
     * 打开自启动管理页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openAutoStartSettings(context: Context): Boolean = false
    
    /**
     * 打开电池优化设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean = false
    
    /**
     * 打开悬浮窗权限设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openFloatingWindowSettings(context: Context): Boolean = false
    
    /**
     * 打开后台运行设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openBackgroundSettings(context: Context): Boolean = false

    /**
     * 此品牌支持的功能集合
     */
    val supportedFeatures: Set<BrandFeature>
        get() = emptySet()
}

/**
 * 品牌功能枚举
 * 
 * 定义品牌适配器可以支持的功能类型
 */
enum class BrandFeature {
    /** 权限设置 */
    PERMISSION_SETTINGS,
    
    /** 自启动管理 */
    AUTO_START,
    
    /** 电池优化 */
    BATTERY_OPTIMIZATION,
    
    /** 悬浮窗权限 */
    FLOATING_WINDOW,
    
    /** 后台运行设置 */
    BACKGROUND_SETTINGS
}
