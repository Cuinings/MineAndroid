package com.cn.board.meet.home.repository

import android.content.Context
import com.cn.board.database.DatabaseManager
import com.cn.board.database.AppInfo
import com.cn.board.meet.home.AppUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author: cn
 * @time: 2026/2/10 10:00
 * @history
 * @description: 应用数据仓库，统一管理数据访问
 */
class AppRepository(private val context: Context) {

    private val appDao by lazy { DatabaseManager.getAppDao(context) }

    /**
     * 加载应用列表
     */
    suspend fun loadApps(): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            var appList = appDao.getAllApps()
            
            if (appList.isEmpty()) {
                // 数据库为空，从系统获取应用数据
                val systemApps = AppUtil.getAllInstalledApps(context)
                
                // 转换并批量插入，设置初始sortOrder
                val appsToInsert = systemApps.mapIndexed {
                    index, appInfo ->
                    AppInfo(
                        isSystemApp = appInfo.isSystemApp,
                        packageName = appInfo.packageName,
                        lastUsedTime = System.currentTimeMillis(),
                        usageCount = 0,
                        sortOrder = index
                    )
                }
                
                // 批量插入
                appDao.insertApps(appsToInsert)
                appList = appDao.getAllApps()
            }
            
            appList
        }
    }

    /**
     * 搜索应用
     */
    suspend fun searchApps(query: String): List<AppInfo> {
        return withContext(Dispatchers.IO) {
//            appDao.searchApps("%$query%")
            emptyList()
        }
    }

    /**
     * 更新应用使用记录
     */
    suspend fun updateAppUsage(appId: Int) {
        return withContext(Dispatchers.IO) {
            appDao.updateAppUsage(appId, System.currentTimeMillis())
        }
    }

    /**
     * 获取最近使用的应用
     */
    suspend fun getRecentApps(limit: Int = 5): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            appDao.getRecentApps(limit)
        }
    }

    /**
     * 获取最常用的应用
     */
    suspend fun getMostUsedApps(limit: Int = 5): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            appDao.getMostUsedApps(limit)
        }
    }

    /**
     * 根据包名获取应用
     */
    suspend fun getAppByPackageName(packageName: String): AppInfo? {
        return withContext(Dispatchers.IO) {
            appDao.searchAppsByPackage(packageName).firstOrNull()
        }
    }

    /**
     * 获取系统应用
     */
    suspend fun getSystemApps(): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            appDao.getAppsByType(true)
        }
    }

    /**
     * 获取用户应用
     */
    suspend fun getUserApps(): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            appDao.getAppsByType(false)
        }
    }
    
    /**
     * 批量更新应用排序顺序
     * @param sortedList 排序后的应用列表
     */
    suspend fun updateAppSortOrders(sortedList: List<AppInfo>) {
        return withContext(Dispatchers.IO) {
            for ((index, appInfo) in sortedList.withIndex()) {
                appDao.updateAppSortOrder(appInfo.packageName, index)
            }
        }
    }
}
