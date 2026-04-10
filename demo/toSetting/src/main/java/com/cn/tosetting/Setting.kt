package com.cn.tosetting

import android.content.Context

/**
 * 设置项密封类
 * 
 * 定义所有可跳转的系统设置项，采用密封类设计确保类型安全和编译时检查。
 * 
 * ## 设计特点
 * - 类型安全：编译时检查所有设置项
 * - 分类管理：支持按类别筛选设置项
 * - 品牌标记：标识需要品牌适配的设置项
 * - 懒加载：所有设置项列表使用懒加载
 * 
 * ## 使用示例
 * ```kotlin
 * // 打开单个设置
 * openSetting(Setting.PermissionSettings)
 * 
 * // 获取所有设置项
 * val all = Setting.all
 * 
 * // 按分类获取
 * val network = Setting.byCategory(SettingCategory.NETWORK)
 * 
 * // 获取品牌差异化设置
 * val brandSpecific = Setting.brandSpecific()
 * ```
 * 
 * @see SettingCategory
 * @see SettingHelper
 */
sealed class Setting(
    val title: String,
    val description: String,
    val iconResId: Int,
    val category: SettingCategory = SettingCategory.APP,
    val requiresBrandSpecific: Boolean = false
) {
    /**
     * 执行设置跳转
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    abstract fun execute(context: Context): Boolean

    /**
     * 应用设置
     * 
     * 打开当前应用的详情设置页面，包含应用权限、存储、通知等设置。
     */
    object AppSettings : Setting(
        title = "应用设置",
        description = "打开应用详情设置页面",
        iconResId = R.drawable.ic_app_settings,
        category = SettingCategory.APP
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openAppSettings(context)
    }

    /**
     * 权限设置
     * 
     * 打开应用权限设置页面，不同品牌手机有不同的权限管理界面。
     * 支持：小米、华为、OPPO、VIVO、魅族等主流品牌。
     */
    object PermissionSettings : Setting(
        title = "权限设置",
        description = "打开应用权限设置（区分手机品牌）",
        iconResId = R.drawable.ic_permission,
        category = SettingCategory.APP,
        requiresBrandSpecific = true
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openPermissionSettings(context)
    }

    /**
     * 通知设置
     * 
     * 打开应用通知设置页面，可以配置通知权限和通知渠道。
     * Android 8.0+ 支持通知渠道管理。
     */
    object NotificationSettings : Setting(
        title = "通知设置",
        description = "打开应用通知设置",
        iconResId = R.drawable.ic_notification,
        category = SettingCategory.APP
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openNotificationSettings(context)
    }

    /**
     * 自动启动设置
     * 
     * 打开自启动管理页面，允许应用在开机时自动启动。
     * 不同品牌手机有不同的自启动管理界面。
     */
    object AutoStartSettings : Setting(
        title = "自动启动",
        description = "打开自启动管理（区分手机品牌）",
        iconResId = R.drawable.ic_auto_start,
        category = SettingCategory.PERMISSION,
        requiresBrandSpecific = true
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openAutoStartSettings(context)
    }

    /**
     * 电池优化设置
     * 
     * 打开电池优化设置页面，配置应用的省电策略。
     * 不同品牌手机有不同的电池管理界面。
     */
    object BatteryOptimization : Setting(
        title = "电池优化",
        description = "打开电池优化设置（区分手机品牌）",
        iconResId = R.drawable.ic_battery,
        category = SettingCategory.PERMISSION,
        requiresBrandSpecific = true
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openBatteryOptimizationSettings(context)
    }

    /**
     * 悬浮窗权限设置
     * 
     * 打开悬浮窗权限设置页面，允许应用显示在其他应用上层。
     * 不同品牌手机有不同的悬浮窗管理界面。
     */
    object FloatingWindow : Setting(
        title = "悬浮窗权限",
        description = "打开悬浮窗权限设置（区分手机品牌）",
        iconResId = R.drawable.ic_floating_window,
        category = SettingCategory.PERMISSION,
        requiresBrandSpecific = true
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openFloatingWindowSettings(context)
    }

    /**
     * 后台运行设置
     * 
     * 打开后台运行设置页面，允许应用在后台运行。
     * 不同品牌手机有不同的后台管理界面。
     */
    object BackgroundSettings : Setting(
        title = "后台运行",
        description = "打开后台运行设置（区分手机品牌）",
        iconResId = R.drawable.ic_background,
        category = SettingCategory.PERMISSION,
        requiresBrandSpecific = true
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openBackgroundSettings(context)
    }

    /**
     * 忽略电池优化请求
     * 
     * 请求将应用加入电池优化白名单。
     * Android 6.0+ 支持。
     */
    object IgnoreBatteryOptimization : Setting(
        title = "忽略电池优化",
        description = "请求忽略电池优化（Android 6.0+）",
        iconResId = R.drawable.ic_battery_optimization,
        category = SettingCategory.PERMISSION
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openIgnoreBatteryOptimization(context)
    }

    /**
     * 安装未知应用权限
     * 
     * 打开安装未知应用权限设置页面。
     * Android 8.0+ 需要此权限才能安装APK。
     */
    object InstallUnknownApps : Setting(
        title = "安装未知应用",
        description = "打开安装未知应用权限设置（Android 8.0+）",
        iconResId = R.drawable.ic_install_unknown,
        category = SettingCategory.PERMISSION
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openInstallUnknownAppsSettings(context)
    }

    /**
     * 画中画权限设置
     * 
     * 打开画中画权限设置页面。
     * Android 8.0+ 支持画中画模式。
     */
    object PictureInPicture : Setting(
        title = "画中画权限",
        description = "打开画中画权限设置（Android 8.0+）",
        iconResId = R.drawable.ic_picture_in_picture,
        category = SettingCategory.PERMISSION
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openPictureInPictureSettings(context)
    }

    /**
     * WiFi设置
     */
    object WifiSettings : Setting(
        title = "WiFi设置",
        description = "打开WiFi设置页面",
        iconResId = R.drawable.ic_wifi,
        category = SettingCategory.NETWORK
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openWifiSettings(context)
    }

    /**
     * 蓝牙设置
     */
    object BluetoothSettings : Setting(
        title = "蓝牙设置",
        description = "打开蓝牙设置页面",
        iconResId = R.drawable.ic_bluetooth,
        category = SettingCategory.NETWORK
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openBluetoothSettings(context)
    }

    /**
     * 位置服务设置
     */
    object LocationSettings : Setting(
        title = "位置设置",
        description = "打开位置服务设置",
        iconResId = R.drawable.ic_location,
        category = SettingCategory.NETWORK
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openLocationSettings(context)
    }

    /**
     * 移动数据设置
     */
    object MobileDataSettings : Setting(
        title = "移动数据",
        description = "打开移动数据设置",
        iconResId = R.drawable.ic_mobile_data,
        category = SettingCategory.NETWORK
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openDataRoamingSettings(context)
    }

    /**
     * 网络运营商设置
     */
    object NetworkSettings : Setting(
        title = "网络设置",
        description = "打开网络运营商设置",
        iconResId = R.drawable.ic_network,
        category = SettingCategory.NETWORK
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openNetworkSettings(context)
    }

    /**
     * 飞行模式设置
     */
    object AirplaneModeSettings : Setting(
        title = "飞行模式",
        description = "打开飞行模式设置",
        iconResId = R.drawable.ic_airplane,
        category = SettingCategory.NETWORK
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openAirplaneModeSettings(context)
    }

    /**
     * 流量使用设置
     */
    object DataUsageSettings : Setting(
        title = "流量使用",
        description = "打开流量使用设置",
        iconResId = R.drawable.ic_data_usage,
        category = SettingCategory.NETWORK
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openDataUsageSettings(context)
    }

    /**
     * VPN设置
     */
    object VpnSettings : Setting(
        title = "VPN设置",
        description = "打开VPN设置",
        iconResId = R.drawable.ic_vpn,
        category = SettingCategory.NETWORK
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openVpnSettings(context)
    }

    /**
     * NFC设置
     */
    object NfcSettings : Setting(
        title = "NFC设置",
        description = "打开NFC设置",
        iconResId = R.drawable.ic_nfc,
        category = SettingCategory.NETWORK
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openNfcSettings(context)
    }

    /**
     * 显示设置
     */
    object DisplaySettings : Setting(
        title = "显示设置",
        description = "打开显示设置页面",
        iconResId = R.drawable.ic_display,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openDisplaySettings(context)
    }

    /**
     * 声音设置
     */
    object SoundSettings : Setting(
        title = "声音设置",
        description = "打开声音设置页面",
        iconResId = R.drawable.ic_sound,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openSoundSettings(context)
    }

    /**
     * 存储设置
     */
    object StorageSettings : Setting(
        title = "存储设置",
        description = "打开存储设置页面",
        iconResId = R.drawable.ic_storage,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openStorageSettings(context)
    }

    /**
     * 内存设置
     */
    object MemorySettings : Setting(
        title = "内存设置",
        description = "打开内存设置页面",
        iconResId = R.drawable.ic_memory,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openMemorySettings(context)
    }

    /**
     * 电池设置
     */
    object BatterySettings : Setting(
        title = "电池设置",
        description = "打开电池设置页面",
        iconResId = R.drawable.ic_battery,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openBatterySettings(context)
    }

    /**
     * 安全设置
     */
    object SecuritySettings : Setting(
        title = "安全设置",
        description = "打开安全设置页面",
        iconResId = R.drawable.ic_security,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openSecuritySettings(context)
    }

    /**
     * 隐私设置
     */
    object PrivacySettings : Setting(
        title = "隐私设置",
        description = "打开隐私设置页面",
        iconResId = R.drawable.ic_privacy,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openPrivacySettings(context)
    }

    /**
     * 语言设置
     */
    object LanguageSettings : Setting(
        title = "语言设置",
        description = "打开语言设置页面",
        iconResId = R.drawable.ic_language,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openLanguageSettings(context)
    }

    /**
     * 日期时间设置
     */
    object DateSettings : Setting(
        title = "日期时间",
        description = "打开日期时间设置",
        iconResId = R.drawable.ic_date,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openDateSettings(context)
    }

    /**
     * 无障碍设置
     */
    object AccessibilitySettings : Setting(
        title = "无障碍",
        description = "打开无障碍设置",
        iconResId = R.drawable.ic_accessibility,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openAccessibilitySettings(context)
    }

    /**
     * 开发者选项
     */
    object DeveloperSettings : Setting(
        title = "开发者选项",
        description = "打开开发者选项",
        iconResId = R.drawable.ic_developer,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openDeveloperSettings(context)
    }

    /**
     * 关于手机
     */
    object AboutPhoneSettings : Setting(
        title = "关于手机",
        description = "打开关于手机页面",
        iconResId = R.drawable.ic_about_phone,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openAboutPhoneSettings(context)
    }

    /**
     * 用户设置
     */
    object UserSettings : Setting(
        title = "用户设置",
        description = "打开用户设置页面",
        iconResId = R.drawable.ic_user,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openUserSettings(context)
    }

    /**
     * 打印设置
     */
    object PrintSettings : Setting(
        title = "打印设置",
        description = "打开打印设置页面",
        iconResId = R.drawable.ic_print,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openPrintSettings(context)
    }

    object SystemSettings : Setting(
        title = "系统设置",
        description = "打开系统设置主页",
        iconResId = R.drawable.ic_system_settings,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openSystemSettings(context)
    }

    /**
     * 原生相册
     */
    object GallerySettings : Setting(
        title = "原生相册",
        description = "打开系统图库应用",
        iconResId = R.drawable.ic_gallery,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openGallerySettings(context)
    }

    /**
     * 系统文件管理
     */
    object FileManagerSettings : Setting(
        title = "文件管理",
        description = "打开系统文件管理器",
        iconResId = R.drawable.ic_storage,
        category = SettingCategory.SYSTEM
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openFileManagerSettings(context)
    }

    /**
     * 应用管理
     */
    object AllAppsSettings : Setting(
        title = "应用管理",
        description = "打开所有应用列表",
        iconResId = R.drawable.ic_all_apps,
        category = SettingCategory.APP
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openAllAppsSettings(context)
    }

    /**
     * 默认应用设置
     */
    object DefaultAppsSettings : Setting(
        title = "默认应用",
        description = "打开默认应用设置（Android 7.0+）",
        iconResId = R.drawable.ic_default_apps,
        category = SettingCategory.APP
    ) {
        override fun execute(context: Context): Boolean = SettingHelper.openDefaultAppsSettings(context)
    }

    companion object {
        /**
         * 所有设置项列表
         * 
         * 使用懒加载初始化，避免重复创建。
         */
        val all: List<Setting> by lazy {
            listOf(
                SystemSettings,
                AppSettings,
                AllAppsSettings,
                DefaultAppsSettings,
                PermissionSettings,
                NotificationSettings,
                AutoStartSettings,
                BatteryOptimization,
                IgnoreBatteryOptimization,
                FloatingWindow,
                BackgroundSettings,
                InstallUnknownApps,
                PictureInPicture,
                WifiSettings,
                BluetoothSettings,
                LocationSettings,
                MobileDataSettings,
                NetworkSettings,
                AirplaneModeSettings,
                DataUsageSettings,
                VpnSettings,
                NfcSettings,
                DisplaySettings,
                SoundSettings,
                StorageSettings,
                MemorySettings,
                BatterySettings,
                SecuritySettings,
                PrivacySettings,
                LanguageSettings,
                DateSettings,
                AccessibilitySettings,
                DeveloperSettings,
                AboutPhoneSettings,
                UserSettings,
                PrintSettings,
                GallerySettings,
                FileManagerSettings,
            )
        }

        /**
         * 按分类获取设置项
         * 
         * @param category 设置分类
         * @return 该分类下的所有设置项列表
         */
        fun byCategory(category: SettingCategory): List<Setting> {
            return all.filter { it.category == category }
        }

        /**
         * 获取需要品牌适配的设置项
         * 
         * @return 需要品牌适配的设置项列表
         */
        fun brandSpecific(): List<Setting> {
            return all.filter { it.requiresBrandSpecific }
        }
    }
}

/**
 * 设置分类枚举
 * 
 * 将设置项分为四大类，便于管理和展示。
 */
enum class SettingCategory(val displayName: String) {
    /** 应用相关设置 */
    APP("应用相关"),
    
    /** 权限管理设置 */
    PERMISSION("权限管理"),
    
    /** 网络设置 */
    NETWORK("网络设置"),
    
    /** 系统设置 */
    SYSTEM("系统设置")
}
