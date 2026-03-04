package com.cn.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import java.io.File

/**
 * App操作工具类
 */
object AppUtil {

    /**
     * 检测app是否安装
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            val packageManager = context.packageManager
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 获取app名称
     */
    fun getAppName(context: Context, packageName: String): String? {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.applicationInfo?.let {
                packageManager.getApplicationLabel(it).toString()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * 获取app图标
     */
    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.applicationInfo?.let {
                packageManager.getApplicationIcon(it)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * 获取app版本名称
     */
    fun getAppVersionName(context: Context, packageName: String): String? {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * 获取app版本号
     */
    fun getAppVersionCode(context: Context, packageName: String): Int {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    /**
     * 获取当前app的包名
     */
    fun getCurrentPackageName(context: Context): String {
        return context.packageName
    }

    /**
     * 获取当前app的名称
     */
    fun getCurrentAppName(context: Context): String {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = context.applicationInfo
            packageManager.getApplicationLabel(applicationInfo)?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 获取当前app的版本名称
     */
    fun getCurrentAppVersionName(context: Context): String {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 获取当前app的版本号
     */
    fun getCurrentAppVersionCode(context: Context): Int {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 检查app是否为系统app
     */
    fun isSystemApp(context: Context, packageName: String): Boolean {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.applicationInfo?.let {
                (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } ?: false
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 获取已安装的所有app列表
     */
    fun getInstalledApps(context: Context, includeSystemApps: Boolean = false): List<PackageInfo> {
        val packageManager = context.packageManager
        val allApps = packageManager.getInstalledPackages(0)
        return if (includeSystemApps) {
            allApps
        } else {
            allApps.filter { 
                it.applicationInfo?.let {appInfo ->
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                } ?: false
            }
        }
    }

    /**
     * 启动app
     */
    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 启动app并传递参数
     */
    fun launchApp(context: Context, packageName: String, extras: Map<String, Any>): Boolean {
        return try {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                extras.forEach { (key, value) ->
                    when (value) {
                        is String -> intent.putExtra(key, value)
                        is Int -> intent.putExtra(key, value)
                        is Long -> intent.putExtra(key, value)
                        is Float -> intent.putExtra(key, value)
                        is Double -> intent.putExtra(key, value)
                        is Boolean -> intent.putExtra(key, value)
                    }
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 启动app的指定Activity
     */
    fun launchActivity(context: Context, packageName: String, activityName: String): Boolean {
        return try {
            val intent = Intent()
            intent.setClassName(packageName, activityName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 启动app的指定Activity并传递参数
     */
    fun launchActivity(context: Context, packageName: String, activityName: String, extras: Map<String, Any>): Boolean {
        return try {
            val intent = Intent()
            intent.setClassName(packageName, activityName)
            extras.forEach { (key, value) ->
                when (value) {
                    is String -> intent.putExtra(key, value)
                    is Int -> intent.putExtra(key, value)
                    is Long -> intent.putExtra(key, value)
                    is Float -> intent.putExtra(key, value)
                    is Double -> intent.putExtra(key, value)
                    is Boolean -> intent.putExtra(key, value)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 通过隐式Intent启动app
     */
    fun launchAppByAction(context: Context, action: String, uri: Uri? = null): Boolean {
        return try {
            val intent = Intent(action)
            uri?.let { intent.data = it }
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 打开应用详情页面（用于权限设置等）
     */
    fun openAppDetails(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent()
            intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 卸载app
     */
    fun uninstallApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 安装app
     */
    fun installApp(context: Context, apkFilePath: String): Boolean {
        return try {
            val apkFile = File(apkFilePath)
            if (!apkFile.exists()) {
                return false
            }
            
            val intent = Intent(Intent.ACTION_VIEW)
            val apkUri = Uri.fromFile(apkFile)
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 安装app（使用FileProvider，适用于Android 7.0+）
     */
    fun installApp(context: Context, apkUri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 静默安装app（需要root权限）
     */
    fun installAppSilentlyWithRoot(apkFilePath: String): Boolean {
        return try {
            val apkFile = File(apkFilePath)
            if (!apkFile.exists()) {
                return false
            }
            
            val command = "pm install -r \"$apkFilePath\""
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 静默安装app（需要系统权限）
     * 需要在AndroidManifest.xml中声明权限：
     * <uses-permission android:name="android.permission.INSTALL_PACKAGES" />
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.INSTALL_PACKAGES)
    fun installAppSilently(context: Context, apkFilePath: String): Boolean {
        return try {
            val apkFile = File(apkFilePath)
            if (!apkFile.exists()) {
                return false
            }
            
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                return false
            }
            
            val packageManager = context.packageManager
            val installParams = android.content.pm.PackageInstaller.SessionParams(
                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            
            val sessionId = packageManager.getPackageInstaller().createSession(installParams)
            val session = packageManager.getPackageInstaller().openSession(sessionId)
            
            val apkInputStream = apkFile.inputStream()
            val sessionOutputStream = session.openWrite("app", 0, apkFile.length())
            
            val buffer = ByteArray(65536)
            var bytesRead = 0
            while (apkInputStream.read(buffer).also { bytesRead = it } != -1) {
                sessionOutputStream.write(buffer, 0, bytesRead)
            }
            
            session.fsync(sessionOutputStream)
            apkInputStream.close()
            sessionOutputStream.close()
            
            val intentSender = android.app.PendingIntent.getBroadcast(
                context, 0, Intent("com.example.INSTALL_COMPLETE"),
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                }
            ).intentSender
            
            session.commit(intentSender)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 静默卸载app（需要root权限）
     */
    fun uninstallAppSilentlyWithRoot(packageName: String): Boolean {
        return try {
            val command = "pm uninstall \"$packageName\""
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 静默卸载app（需要系统权限）
     * 需要在AndroidManifest.xml中声明权限：
     * <uses-permission android:name="android.permission.DELETE_PACKAGES" />
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.DELETE_PACKAGES)
    fun uninstallAppSilently(context: Context, packageName: String): Boolean {
        return try {
            val packageManager = context.packageManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val packageInstaller = packageManager.getPackageInstaller()
                val intentSender = android.app.PendingIntent.getBroadcast(
                    context, 0, Intent("com.example.UNINSTALL_COMPLETE"),
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    }
                ).intentSender
                packageInstaller.uninstall(packageName, intentSender)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
