package com.cn.tosetting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import com.cn.tosetting.brand.BrandAdapterManager
import com.cn.tosetting.brand.BrandFeature

/**
 * 设置跳转工具类
 * 
 * 提供各种系统设置页面的跳转功能，支持Android标准API和品牌适配。
 * 
 * ## 功能分类
 * - 通用设置跳转：使用Android标准Settings API
 * - 品牌适配跳转：针对不同手机品牌的定制设置页面
 * - 权限相关跳转：应用权限、通知、悬浮窗等
 * 
 * ## 使用示例
 * ```kotlin
 * // 打开应用设置
 * SettingHelper.openAppSettings(context)
 * 
 * // 打开权限设置（自动适配品牌）
 * SettingHelper.openPermissionSettings(context)
 * 
 * // 检查功能支持
 * if (SettingHelper.isFeatureSupported(context, BrandFeature.AUTO_START)) {
 *     SettingHelper.openAutoStartSettings(context)
 * }
 * ```
 * 
 * @see BrandAdapterManager
 * @see BrandFeature
 */
object SettingHelper {

    /**
     * 获取当前手机品牌名称
     * 
     * @return 品牌名称字符串
     */
    @Deprecated("")
    fun getPhoneBrand(): String {
        return ""
//        return BrandAdapterManager.getCurrentBrand(android.os.Process.myUserHandle().toString().let { "default" })
    }

    /**
     * 获取当前手机品牌名称
     * 
     * @param context 上下文
     * @return 品牌名称字符串
     */
    fun getPhoneBrand(context: Context): String {
        return BrandAdapterManager.getCurrentBrand(context)
    }

    /**
     * 检查当前设备是否支持指定功能
     * 
     * @param context 上下文
     * @param feature 要检查的功能
     * @return 如果支持返回 true，否则返回 false
     */
    fun isFeatureSupported(context: Context, feature: BrandFeature): Boolean {
        return BrandAdapterManager.isFeatureSupported(context, feature)
    }

    /**
     * 打开应用详情设置页面
     * 
     * 打开当前应用的详情设置页面，包含应用权限、存储、通知等设置。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openAppSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            openSettingsSafely(context, Settings.ACTION_APPLICATION_SETTINGS)
        }
    }

    /**
     * 打开WiFi设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openWifiSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_WIFI_SETTINGS)
    }

    /**
     * 打开蓝牙设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openBluetoothSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_BLUETOOTH_SETTINGS)
    }

    /**
     * 打开位置服务设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openLocationSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }

    /**
     * 打开数据漫游设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openDataRoamingSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_DATA_ROAMING_SETTINGS)
    }

    /**
     * 打开网络运营商设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openNetworkSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
    }

    /**
     * 打开显示设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openDisplaySettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_DISPLAY_SETTINGS)
    }

    /**
     * 打开声音设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openSoundSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_SOUND_SETTINGS)
    }

    /**
     * 打开存储设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openStorageSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
    }

    /**
     * 打开电池设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openBatterySettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)
    }

    /**
     * 打开安全设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openSecuritySettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_SECURITY_SETTINGS)
    }

    /**
     * 打开语言设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openLanguageSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_LOCALE_SETTINGS)
    }

    /**
     * 打开日期时间设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openDateSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_DATE_SETTINGS)
    }

    /**
     * 打开无障碍设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openAccessibilitySettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    /**
     * 打开开发者选项页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openDeveloperSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
    }

    /**
     * 打开所有应用列表页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openAllAppsSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_APPLICATION_SETTINGS)
    }

    /**
     * 打开默认应用设置页面
     * 
     * Android 7.0+ 支持此功能。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openDefaultAppsSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            openSettingsSafely(context, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        } else {
            openAppSettings(context)
        }
    }

    /**
     * 打开关于手机页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openAboutPhoneSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_DEVICE_INFO_SETTINGS)
    }

    /**
     * 打开隐私设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openPrivacySettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_PRIVACY_SETTINGS)
    }

    /**
     * 打开飞行模式设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openAirplaneModeSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
    }

    /**
     * 打开流量使用设置页面
     * 
     * Android 9.0+ 支持此功能。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openDataUsageSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            openSettingsSafely(context, Settings.ACTION_DATA_USAGE_SETTINGS)
        } else {
            openSettingsSafely(context, Settings.ACTION_SETTINGS)
        }
    }

    /**
     * 打开内存设置页面
     * 
     * Android 9.0+ 支持此功能。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openMemorySettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            openSettingsSafely(context, Settings.ACTION_MEMORY_CARD_SETTINGS)
        } else {
            openStorageSettings(context)
        }
    }

    /**
     * 打开NFC设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openNfcSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_NFC_SETTINGS)
    }

    /**
     * 打开VPN设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openVpnSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_VPN_SETTINGS)
    }

    /**
     * 打开打印设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openPrintSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_PRINT_SETTINGS)
    }

    /**
     * 打开用户设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openUserSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_SETTINGS)
    }

    /**
     * 打开系统设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openSystemSettings(context: Context): Boolean {
        return openSettingsSafely(context, Settings.ACTION_SETTINGS)
    }

    fun openNotificationSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                openAppSettings(context)
            }
        } else {
            openAppSettings(context)
        }
    }

    /**
     * 打开权限设置页面
     * 
     * 根据不同手机品牌自动适配跳转到对应的权限管理页面。
     * 支持：小米、华为、OPPO、VIVO、魅族等主流品牌。
     * 如果品牌适配失败，则跳转到应用详情页面。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openPermissionSettings(context: Context): Boolean {
        val result = BrandAdapterManager.openPermissionSettings(context)
        return if (result) true else openAppSettings(context)
    }

    /**
     * 打开自启动管理页面
     * 
     * 根据不同手机品牌自动适配跳转到对应的自启动管理页面。
     * 支持：小米、华为、OPPO、VIVO、魅族等主流品牌。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val result = BrandAdapterManager.openAutoStartSettings(context)
        return if (result) true else {
            Toast.makeText(context, "当前设备不支持自动启动管理", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * 打开电池优化设置页面
     * 
     * 根据不同手机品牌自动适配跳转到对应的电池管理页面。
     * 支持：小米、华为、OPPO、VIVO、三星等主流品牌。
     * 如果品牌适配失败，则跳转到Android标准电池优化设置页面。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        val result = BrandAdapterManager.openBatteryOptimizationSettings(context)
        return if (result) true else openSettingsSafely(context, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    /**
     * 打开悬浮窗权限设置页面
     * 
     * 根据不同手机品牌自动适配跳转到对应的悬浮窗管理页面。
     * 支持：小米、华为、OPPO、VIVO、三星等主流品牌。
     * 如果品牌适配失败，则跳转到Android标准悬浮窗权限设置页面。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openFloatingWindowSettings(context: Context): Boolean {
        val result = BrandAdapterManager.openFloatingWindowSettings(context)
        return if (result) true else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                openSettingsSafely(context, Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            } else {
                openAppSettings(context)
            }
        }
    }

    /**
     * 打开后台运行设置页面
     * 
     * 根据不同手机品牌自动适配跳转到对应的后台管理页面。
     * 支持：小米、华为、OPPO、VIVO等主流品牌。
     * 如果品牌适配失败，则跳转到应用详情页面。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openBackgroundSettings(context: Context): Boolean {
        val result = BrandAdapterManager.openBackgroundSettings(context)
        return if (result) true else openAppSettings(context)
    }

    /**
     * 请求忽略电池优化
     * 
     * 打开请求忽略电池优化对话框，请求将应用加入电池优化白名单。
     * Android 6.0+ 支持此功能。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openIgnoreBatteryOptimization(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                openSettingsSafely(context, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }
        } else {
            openAppSettings(context)
        }
    }

    /**
     * 打开安装未知应用权限设置页面
     * 
     * Android 8.0+ 需要此权限才能安装APK。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openInstallUnknownAppsSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                openSecuritySettings(context)
            }
        } else {
            openSecuritySettings(context)
        }
    }

    /**
     * 打开画中画权限设置页面
     * 
     * Android 8.0+ 支持画中画模式。
     * 由于Android没有专门的画中画设置页面，此方法跳转到应用详情页面。
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openPictureInPictureSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                openAppSettings(context)
            }
        } else {
            openAppSettings(context)
        }
    }

    /**
     * 打开原生相册
     * 
     * 打开系统图库应用，支持多种跳转方式：
     * 1. 优先尝试打开系统相册应用
     * 2. 尝试使用 MediaStore Intent 打开图库
     * 3. 最后尝试打开图片查看器
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openGallerySettings(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                Toast.makeText(context, "无法打开相册", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    /**
     * 打开系统文件管理器
     * 
     * 打开系统文件管理应用，支持多种跳转方式：
     * 1. 优先尝试使用 ACTION_GET_CONTENT 打开文件选择器
     * 2. 尝试使用 ACTION_OPEN_DOCUMENT 打开文件管理器
     * 3. 尝试打开存储设置页面
     * 
     * @param context 上下文
     * @return 如果跳转成功返回 true，否则返回 false
     */
    fun openFileManagerSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                openStorageSettings(context)
            }
        }
    }

    /**
     * 安全打开设置页面
     * 
     * 尝试打开指定的设置页面，如果失败则尝试打开系统设置主页。
     * 
     * @param context 上下文
     * @param action Settings Action常量
     * @return 如果跳转成功返回 true，否则返回 false
     */
    private fun openSettingsSafely(context: Context, action: String): Boolean {
        return try {
            val intent = Intent(action)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                Toast.makeText(context, "无法打开设置页面", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }
}
