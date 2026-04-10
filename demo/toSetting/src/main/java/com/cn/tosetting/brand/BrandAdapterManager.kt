package com.cn.tosetting.brand

import android.content.Context
import android.os.Build

/**
 * 品牌适配器管理器
 * 
 * 负责管理所有品牌适配器，检测当前设备品牌，
 * 并提供统一的设置跳转接口。
 * 
 * ## 功能
 * - 自动检测设备品牌
 * - 动态注册/注销品牌适配器
 * - 查询品牌支持的功能
 * - 提供统一的设置跳转入口
 * 
 * ## 使用示例
 * ```kotlin
 * // 获取当前品牌名称
 * val brandName = BrandAdapterManager.getCurrentBrand(context)
 * 
 * // 检查功能支持
 * if (BrandAdapterManager.isFeatureSupported(context, BrandFeature.AUTO_START)) {
 *     BrandAdapterManager.openAutoStartSettings(context)
 * }
 * 
 * // 动态注册新品牌
 * BrandAdapterManager.register(CustomAdapter())
 * ```
 */
object BrandAdapterManager {

    private val adapters: MutableList<BrandAdapter> = mutableListOf()

    private var currentAdapter: BrandAdapter? = null

    init {
        registerDefaultAdapters()
    }

    /**
     * 注册默认的品牌适配器
     */
    private fun registerDefaultAdapters() {
        register(XiaomiAdapter)
        register(HuaweiAdapter)
        register(OppoAdapter)
        register(VivoAdapter)
        register(SamsungAdapter)
        register(MeizuAdapter)
    }

    /**
     * 注册品牌适配器
     * 
     * @param adapter 要注册的适配器
     */
    fun register(adapter: BrandAdapter) {
        adapters.add(adapter)
    }

    /**
     * 注销品牌适配器
     * 
     * @param adapter 要注销的适配器
     */
    fun unregister(adapter: BrandAdapter) {
        adapters.remove(adapter)
    }

    /**
     * 清除所有已注册的适配器
     */
    fun clearAdapters() {
        adapters.clear()
    }

    /**
     * 检测当前设备品牌
     * 
     * 遍历所有已注册的适配器，返回第一个匹配的适配器。
     * 如果没有匹配的，返回默认适配器。
     * 
     * @param context 上下文
     * @return 匹配的品牌适配器
     */
    fun detectBrand(context: Context): BrandAdapter {
        if (currentAdapter != null) {
            return currentAdapter!!
        }

        val manufacturer = Build.MANUFACTURER
        val brand = Build.BRAND

        val adapter = adapters.find { it.match(manufacturer, brand) } ?: DefaultAdapter
        currentAdapter = adapter
        return adapter
    }

    /**
     * 获取当前品牌名称
     * 
     * @param context 上下文
     * @return 品牌名称字符串
     */
    fun getCurrentBrand(context: Context): String {
        return detectBrand(context).brandName
    }

    /**
     * 获取当前品牌支持的功能集合
     * 
     * @param context 上下文
     * @return 支持的功能集合
     */
    fun getSupportedFeatures(context: Context): Set<BrandFeature> {
        return detectBrand(context).supportedFeatures
    }

    /**
     * 检查当前品牌是否支持指定功能
     * 
     * @param context 上下文
     * @param feature 要检查的功能
     * @return 如果支持返回 true，否则返回 false
     */
    fun isFeatureSupported(context: Context, feature: BrandFeature): Boolean {
        return detectBrand(context).supportedFeatures.contains(feature)
    }

    /**
     * 打开权限设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openPermissionSettings(context: Context): Boolean {
        return detectBrand(context).openPermissionSettings(context)
    }

    /**
     * 打开自启动管理页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openAutoStartSettings(context: Context): Boolean {
        return detectBrand(context).openAutoStartSettings(context)
    }

    /**
     * 打开电池优化设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return detectBrand(context).openBatteryOptimizationSettings(context)
    }

    /**
     * 打开悬浮窗权限设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openFloatingWindowSettings(context: Context): Boolean {
        return detectBrand(context).openFloatingWindowSettings(context)
    }

    /**
     * 打开后台运行设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openBackgroundSettings(context: Context): Boolean {
        return detectBrand(context).openBackgroundSettings(context)
    }

    /**
     * 重置品牌检测缓存
     * 
     * 清除已缓存的适配器，下次调用时会重新检测品牌
     */
    fun resetCache() {
        currentAdapter = null
    }
}
