package com.cn.board.meet.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import com.cn.board.database.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import kotlin.collections.Map

/**
 * @author: cn
 * @time: 2026/2/10 11:00
 * @history
 * @description: 应用工具类，用于获取系统应用信息
 */
object AppUtil {
    
    // 缓存大小
    private const val CACHE_SIZE = 100
    
    // 应用图标缓存
    private val iconCache = object : LinkedHashMap<String, Drawable>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Drawable>): Boolean {
            return size > CACHE_SIZE
        }
    }
    
    // 应用名称缓存
    private val nameCache = object : LinkedHashMap<String, String>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>): Boolean {
            return size > CACHE_SIZE
        }
    }
    
    // 应用信息缓存
    private val appInfoCache = object : LinkedHashMap<String, AppInfo>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, AppInfo>): Boolean {
            return size > CACHE_SIZE
        }
    }
    
    // 应用列表缓存
    private var appList: List<AppInfo> = emptyList()
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        iconCache.clear()
        nameCache.clear()
        appInfoCache.clear()
    }

    /**
     * 获取系统中所有已安装的应用
     * @param context 上下文
     * @return 应用信息列表
     */
    @SuppressLint("QueryPermissionsNeeded")
    suspend fun getAllInstalledApps(context: Context): List<AppInfo> {
        val appContext = context.applicationContext // 使用applicationContext
        val apps = withContext(Dispatchers.IO) {
            val packageManager = appContext.packageManager
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
                        // 缓存应用信息
                        appInfoCache[pkg.packageName] = appInfo
                    }
                }
            }

            // 按照sortOrder排序
            apps.sortBy { it.sortOrder }
            apps // 返回排序后的列表
        }
        
        // 更新appList缓存
        appList = apps
        
        return apps
    }
    
    /**
     * 更新应用排序顺序
     * @param sortedList 排序后的应用列表
     */
    fun updateAppSortOrder(sortedList: List<AppInfo>) {
        // 更新每个应用的sortOrder
        val updatedList = sortedList.mapIndexed { index, appInfo ->
            val updatedAppInfo = appInfo.copy(sortOrder = index)
            // 更新缓存
            appInfoCache[appInfo.packageName] = updatedAppInfo
            updatedAppInfo
        }
        
        // 更新appList
        appList = updatedList
        Log.d("AppUtil", "更新应用排序顺序，应用数量: ${updatedList.size}")
    }

    /**
     * 获取系统应用
     * @param context 上下文
     * @return 系统应用信息列表
     */
    suspend fun getSystemApps(context: Context): List<AppInfo> {
        return getAllInstalledApps(context.applicationContext).filter { it.isSystemApp }
    }

    /**
     * 获取用户应用
     * @param context 上下文
     * @return 用户应用信息列表
     */
    suspend fun getUserApps(context: Context): List<AppInfo> {
        return getAllInstalledApps(context.applicationContext).filter { !it.isSystemApp }
    }

    /**
     * 根据包名获取应用信息
     * @param context 上下文
     * @param packageName 包名
     * @return 应用信息，若未找到则返回null
     */
    suspend fun getAppByPackageName(context: Context, packageName: String): AppInfo? {
        val appContext = context.applicationContext
        return withContext(Dispatchers.IO) {
            val packageManager = appContext.packageManager
            try {
                val pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                createAppUtilInfo(pkg, packageManager)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    /**
     * 创建应用信息对象
     * @param pkg 包信息
     * @param packageManager 包管理器
     * @return 应用信息对象
     */
    private fun createAppUtilInfo(pkg: PackageInfo, packageManager: PackageManager): AppInfo {
        val appInfo = pkg.applicationInfo
        val isSystemApp = isSystemApp(appInfo)
        val packageName = pkg.packageName

        return AppInfo(
            packageName = packageName,
            isSystemApp = isSystemApp
        )
    }

    /**
     * 获取应用图标
     * @param context 上下文
     * @param packageName 包名
     * @return 应用图标
     */
    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return getAppIconInternal(context.applicationContext, packageName)
    }

    /**
     * 获取应用名称
     * @param context 上下文
     * @param packageName 包名
     * @return 应用名称
     */
    fun getAppName(context: Context, packageName: String): String {
        return getAppNameInternal(context.applicationContext, packageName)
    }

    /**
     * 异步获取应用图标
     * @param context 上下文
     * @param packageName 包名
     * @return 应用图标
     */
    suspend fun getAppIconAsync(context: Context, packageName: String): Drawable? {
        return withContext(Dispatchers.IO) {
            getAppIconInternal(context.applicationContext, packageName)
        }
    }

    /**
     * 异步获取应用名称
     * @param context 上下文
     * @param packageName 包名
     * @return 应用名称
     */
    suspend fun getAppNameAsync(context: Context, packageName: String): String {
        return withContext(Dispatchers.IO) {
            getAppNameInternal(context.applicationContext, packageName)
        }
    }

    /**
     * 内部方法：获取应用图标
     * @param context 上下文
     * @param packageName 包名
     * @return 应用图标
     */
    private fun getAppIconInternal(context: Context, packageName: String): Drawable? {
        // 首先从缓存中获取
        synchronized(iconCache) {
            if (iconCache.containsKey(packageName)) {
                Log.d("AppUtil", "从缓存获取应用图标: $packageName")
                return iconCache[packageName]
            }
        }
        
        // 缓存中没有，从PackageManager中获取
        val packageManager = context.packageManager
        try {
            Log.d("AppUtil", "从PackageManager获取应用图标: $packageName")
            val pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            val icon = pkg?.applicationInfo?.loadIcon(packageManager)?: packageManager.defaultActivityIcon
            
            // 存入缓存
            synchronized(iconCache) {
                iconCache[packageName] = icon
                Log.d("AppUtil", "应用图标存入缓存: $packageName")
            }
            
            return icon
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppUtil", "应用包名未找到: $packageName", e)
            return null
        } catch (e: Exception) {
            Log.e("AppUtil", "获取应用图标失败: $packageName", e)
            return null
        }
    }

    /**
     * 内部方法：获取应用名称
     * @param context 上下文
     * @param packageName 包名
     * @return 应用名称
     */
    private fun getAppNameInternal(context: Context, packageName: String): String {
        // 首先从缓存中获取
        synchronized(nameCache) {
            if (nameCache.containsKey(packageName)) {
                Log.d("AppUtil", "从缓存获取应用名称: $packageName")
                return nameCache[packageName].toString()
            }
        }
        
        // 缓存中没有，从PackageManager中获取
        val packageManager = context.packageManager
        try {
            Log.d("AppUtil", "从PackageManager获取应用名称: $packageName")
            val pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            val appName = pkg?.applicationInfo?.loadLabel(packageManager)?.toString()?:"Unknown"
            
            // 存入缓存
            synchronized(nameCache) {
                nameCache[packageName] = appName
                Log.d("AppUtil", "应用名称存入缓存: $packageName -> $appName")
            }
            
            return appName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppUtil", "应用包名未找到: $packageName", e)
            return "Unknown"
        } catch (e: Exception) {
            Log.e("AppUtil", "获取应用名称失败: $packageName", e)
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

    /**
     * 通过包名启动应用
     * @param context 上下文
     * @param packageName 包名
     * @return 是否启动成功
     */
    fun launchAppByPackageName(context: Context, packageName: String): Boolean {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                // 添加FLAG_ACTIVITY_NEW_TASK以确保在非Activity上下文也能启动
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(launchIntent)
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
