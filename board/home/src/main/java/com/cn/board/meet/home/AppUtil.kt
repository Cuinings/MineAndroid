package com.cn.board.meet.home

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.cn.board.database.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author: cn
 * @time: 2026/2/10 11:00
 * @history
 * @description: 应用工具类，用于获取系统应用信息
 */
object AppUtil {

    /**
     * 获取系统中所有已安装的应用
     * @param context 上下文
     * @return 应用信息列表
     */
    suspend fun getAllInstalledApps(context: Context): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val apps = mutableListOf<AppInfo>()

            // 获取所有已安装的应用包信息
            val packages = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)

            for (pkg in packages) {
                // 检查应用是否已启用
                if (isAppEnabled(pkg.applicationInfo)) {
                    // 检查是否有启动Activity
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkg.packageName)
                    if (launchIntent != null) {
                        val appInfo = createAppUtilInfo(pkg, packageManager)
                        apps.add(appInfo)
                    }
                }
            }

            apps
        }
    }

    /**
     * 获取系统应用
     * @param context 上下文
     * @return 系统应用信息列表
     */
    suspend fun getSystemApps(context: Context): List<AppInfo> {
        return getAllInstalledApps(context).filter { it.isSystemApp }
    }

    /**
     * 获取用户应用
     * @param context 上下文
     * @return 用户应用信息列表
     */
    suspend fun getUserApps(context: Context): List<AppInfo> {
        return getAllInstalledApps(context).filter { !it.isSystemApp }
    }

    /**
     * 根据包名获取应用信息
     * @param context 上下文
     * @param packageName 包名
     * @return 应用信息，若未找到则返回null
     */
    suspend fun getAppByPackageName(context: Context, packageName: String): AppInfo? {
        return withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            try {
                val pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                createAppUtilInfo(pkg, packageManager)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    /**
     * 同步版本：根据包名获取应用信息
     * @param context 上下文
     * @param packageName 包名
     * @return 应用信息，若未找到则返回null
     */
    /*fun getAppByPackageNameSync(context: Context, packageName: String): AppInfo? {
        val packageManager = context.packageManager
        try {
            val pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return createAppUtilInfo(pkg, packageManager)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
    }*/

    /**
     * 创建应用信息对象
     * @param pkg 包信息
     * @param packageManager 包管理器
     * @return 应用信息对象
     */
    private fun createAppUtilInfo(pkg: PackageInfo, packageManager: PackageManager): AppInfo {
        val appInfo = pkg.applicationInfo
//        val appName = appInfo?.loadLabel(packageManager)?.toString() ?: "Unknown"
//        val icon = appInfo?.loadIcon(packageManager) ?: packageManager.defaultActivityIcon
        val isSystemApp = isSystemApp(appInfo)
        val packageName = pkg.packageName

        return AppInfo(
            packageName = packageName,
            isSystemApp = isSystemApp
        )
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        val packageManager = context.packageManager
        try {
            val pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return pkg?.applicationInfo?.loadIcon(packageManager)?: packageManager.defaultActivityIcon
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
    }

    fun getAppName(context: Context, packageName: String): String {
        val packageManager = context.packageManager
        try {
            val pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return pkg?.applicationInfo?.loadLabel(packageManager)?.toString()?:"Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            return "Unknown"
        }
    }

    /**
     * 判断是否为系统应用
     * @param appInfo 应用信息
     * @return 是否为系统应用
     */
    private fun isSystemApp(appInfo: ApplicationInfo?): Boolean {
        return appInfo != null && ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
               (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
    }

    /**
     * 判断应用是否已启用
     * @param appInfo 应用信息
     * @return 是否已启用
     */
    private fun isAppEnabled(appInfo: ApplicationInfo?): Boolean {
        return appInfo != null
    }
}
