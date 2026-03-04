package com.cn.core.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * 权限工具类
 */
object PermissionUtil {

    /**
     * 检查单个权限是否已授予
     */
    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查多个权限是否已授予
     */
    fun checkPermissions(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (!checkPermission(context, permission)) {
                return false
            }
        }
        return true
    }

    /**
     * 请求单个权限
     */
    fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    /**
     * 请求多个权限
     */
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * 请求单个权限（Fragment）
     */
    fun requestPermission(fragment: Fragment, permission: String, requestCode: Int) {
        fragment.requestPermissions(arrayOf(permission), requestCode)
    }

    /**
     * 请求多个权限（Fragment）
     */
    fun requestPermissions(fragment: Fragment, permissions: Array<String>, requestCode: Int) {
        fragment.requestPermissions(permissions, requestCode)
    }

    /**
     * 检查是否需要显示权限请求说明
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * 检查是否需要显示多个权限请求说明
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (shouldShowRequestPermissionRationale(activity, permission)) {
                return true
            }
        }
        return false
    }

    /**
     * 检查是否需要显示权限请求说明（Fragment）
     */
    fun shouldShowRequestPermissionRationale(fragment: Fragment, permission: String): Boolean {
        return fragment.shouldShowRequestPermissionRationale(permission)
    }

    /**
     * 检查是否需要显示多个权限请求说明（Fragment）
     */
    fun shouldShowRequestPermissionRationale(fragment: Fragment, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (shouldShowRequestPermissionRationale(fragment, permission)) {
                return true
            }
        }
        return false
    }

    /**
     * 处理权限请求结果
     */
    fun onRequestPermissionsResult(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * 获取权限名称
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            android.Manifest.permission.READ_CALENDAR -> "读取日历"
            android.Manifest.permission.WRITE_CALENDAR -> "写入日历"
            android.Manifest.permission.CAMERA -> "相机"
            android.Manifest.permission.READ_CONTACTS -> "读取联系人"
            android.Manifest.permission.WRITE_CONTACTS -> "写入联系人"
            android.Manifest.permission.GET_ACCOUNTS -> "获取账户"
            android.Manifest.permission.ACCESS_FINE_LOCATION -> "精确位置"
            android.Manifest.permission.ACCESS_COARSE_LOCATION -> "粗略位置"
            android.Manifest.permission.RECORD_AUDIO -> "录音"
            android.Manifest.permission.READ_PHONE_STATE -> "读取电话状态"
            android.Manifest.permission.CALL_PHONE -> "拨打电话"
            android.Manifest.permission.READ_CALL_LOG -> "读取通话记录"
            android.Manifest.permission.WRITE_CALL_LOG -> "写入通话记录"
            android.Manifest.permission.ADD_VOICEMAIL -> "添加语音邮件"
            android.Manifest.permission.USE_SIP -> "使用SIP"
            android.Manifest.permission.PROCESS_OUTGOING_CALLS -> "处理拨出电话"
            android.Manifest.permission.BODY_SENSORS -> "身体传感器"
            android.Manifest.permission.SEND_SMS -> "发送短信"
            android.Manifest.permission.RECEIVE_SMS -> "接收短信"
            android.Manifest.permission.READ_SMS -> "读取短信"
            android.Manifest.permission.RECEIVE_WAP_PUSH -> "接收WAP推送"
            android.Manifest.permission.RECEIVE_MMS -> "接收彩信"
            android.Manifest.permission.READ_EXTERNAL_STORAGE -> "读取存储"
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> "写入存储"
            android.Manifest.permission.ACCESS_NETWORK_STATE -> "访问网络状态"
            android.Manifest.permission.INTERNET -> "访问互联网"
            android.Manifest.permission.ACCESS_WIFI_STATE -> "访问WiFi状态"
            android.Manifest.permission.CHANGE_WIFI_STATE -> "更改WiFi状态"
            android.Manifest.permission.BLUETOOTH -> "蓝牙"
            android.Manifest.permission.BLUETOOTH_ADMIN -> "蓝牙管理"
            android.Manifest.permission.WAKE_LOCK -> "唤醒锁"
            android.Manifest.permission.VIBRATE -> "振动"
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED -> "接收开机完成"
            else -> permission
        }
    }

    /**
     * 检查是否有悬浮窗权限
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 检查是否有安装未知应用权限
     */
    fun hasInstallPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * 检查是否有通知权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 检查是否有存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) ||
            checkPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) ||
            checkPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            checkPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) &&
            checkPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /**
     * 检查是否有相机权限
     */
    fun hasCameraPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.CAMERA)
    }

    /**
     * 检查是否有录音权限
     */
    fun hasRecordAudioPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.RECORD_AUDIO)
    }

    /**
     * 检查是否有位置权限
     */
    fun hasLocationPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ||
        checkPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /**
     * 检查是否有精确位置权限
     */
    fun hasFineLocationPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * 检查是否有粗略位置权限
     */
    fun hasCoarseLocationPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /**
     * 检查是否有电话权限
     */
    fun hasPhonePermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.READ_PHONE_STATE) &&
        checkPermission(context, android.Manifest.permission.CALL_PHONE)
    }

    /**
     * 检查是否有短信权限
     */
    fun hasSmsPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.SEND_SMS) &&
        checkPermission(context, android.Manifest.permission.RECEIVE_SMS) &&
        checkPermission(context, android.Manifest.permission.READ_SMS)
    }

    /**
     * 检查是否有联系人权限
     */
    fun hasContactsPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.READ_CONTACTS) &&
        checkPermission(context, android.Manifest.permission.WRITE_CONTACTS)
    }

    /**
     * 检查是否有日历权限
     */
    fun hasCalendarPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.READ_CALENDAR) &&
        checkPermission(context, android.Manifest.permission.WRITE_CALENDAR)
    }

    /**
     * 跳转到应用设置页面
     */
    fun openAppSettings(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 跳转到悬浮窗权限设置页面
     */
    fun openOverlayPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.packageName)
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * 跳转到安装未知应用权限设置页面
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + context.packageName)
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * 跳转到通知权限设置页面
     */
    fun openNotificationPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent()
            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * 跳转到应用信息页面
     */
    fun openAppInfo(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 跳转到WiFi设置页面
     */
    fun openWifiSettings(context: Context) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 跳转到蓝牙设置页面
     */
    fun openBluetoothSettings(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 跳转到位置设置页面
     */
    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 跳转到移动网络设置页面
     */
    fun openMobileDataSettings(context: Context) {
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 检查是否是系统应用
     */
    fun isSystemApp(context: Context): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查是否是调试应用
     */
    fun isDebuggable(context: Context): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查外部存储是否可读
     */
    fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in setOf(
            Environment.MEDIA_MOUNTED,
            Environment.MEDIA_MOUNTED_READ_ONLY
        )
    }

    /**
     * 检查外部存储是否可写
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * 获取被拒绝的权限列表
     */
    fun getDeniedPermissions(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter { !checkPermission(context, it) }
    }

    /**
     * 获取永久拒绝的权限列表
     */
    fun getPermanentlyDeniedPermissions(activity: Activity, permissions: Array<String>): List<String> {
        return permissions.filter { permission ->
            !checkPermission(activity, permission) &&
            !shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    /**
     * 检查是否有任何权限被拒绝
     */
    fun hasAnyPermissionDenied(context: Context, permissions: Array<String>): Boolean {
        return getDeniedPermissions(context, permissions).isNotEmpty()
    }

    /**
     * 检查是否有任何权限被永久拒绝
     */
    fun hasAnyPermissionPermanentlyDenied(activity: Activity, permissions: Array<String>): Boolean {
        return getPermanentlyDeniedPermissions(activity, permissions).isNotEmpty()
    }

    /**
     * 检查是否有安装应用权限（系统权限）
     */
    fun hasInstallPackagesPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.INSTALL_PACKAGES)
    }

    /**
     * 检查是否有删除应用权限（系统权限）
     */
    fun hasDeletePackagesPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.DELETE_PACKAGES)
    }

    /**
     * 检查是否有清除应用缓存权限（系统权限）
     */
    fun hasClearAppCachePermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.CLEAR_APP_CACHE)
    }

    /**
     * 检查是否有重启设备权限（系统权限）
     */
    fun hasRebootPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.REBOOT)
    }

    /**
     * 检查是否有修改系统设置权限（系统权限）
     */
    fun hasWriteSettingsPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.WRITE_SETTINGS)
    }

    /**
     * 检查是否有修改安全设置权限（系统权限）
     */
    fun hasWriteSecureSettingsPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.WRITE_SECURE_SETTINGS)
    }

    /**
     * 检查是否有系统级权限
     */
    fun hasSystemPermission(context: Context, permission: String): Boolean {
        return checkPermission(context, permission)
    }

    /**
     * 检查是否有多个系统权限
     */
    fun hasSystemPermissions(context: Context, permissions: Array<String>): Boolean {
        return checkPermissions(context, permissions)
    }

    /**
     * 检查设备是否已root
     */
    fun isDeviceRooted(): Boolean {
        return try {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )
            
            for (path in paths) {
                if (java.io.File(path).exists()) {
                    return true
                }
            }
            
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查应用是否有root权限
     */
    fun hasRootPermission(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = process.outputStream
            outputStream.write("exit\n".toByteArray())
            outputStream.flush()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查应用是否有root权限（通过执行命令）
     */
    fun hasRootPermission(command: String = "echo test"): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查是否是特权应用（位于priv-app目录）
     */
    fun isPrivilegedApp(context: Context): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                (appInfo.flags and 0x80) != 0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查是否有系统级应用安装权限
     */
    fun isSystemAppInstaller(context: Context): Boolean {
        return hasInstallPackagesPermission(context) && isSystemApp(context)
    }

    /**
     * 获取应用类型
     */
    fun getAppType(context: Context): String {
        return when {
            isPrivilegedApp(context) -> "特权应用"
            isSystemApp(context) -> "系统应用"
            isDebuggable(context) -> "调试应用"
            else -> "用户应用"
        }
    }
}