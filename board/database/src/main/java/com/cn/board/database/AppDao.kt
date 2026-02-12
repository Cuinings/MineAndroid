package com.cn.board.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * @author: cn
 * @time: 2026/2/10 10:00
 * @history
 * @description: Room DAO接口
 */
@Dao
interface AppDao {
    @Insert
    suspend fun insertApp(appInfo: AppInfo)

    @Insert
    suspend fun insertApps(apps: List<AppInfo>)

    @Update
    suspend fun updateApp(appInfo: AppInfo)

    @Query("DELETE FROM app_info WHERE id = :id")
    suspend fun deleteAppById(id: Int)

    @Query("DELETE FROM app_info")
    suspend fun deleteAllApps()

    @Query("SELECT * FROM app_info ORDER BY sortOrder ASC")
    suspend fun getAllApps(): List<AppInfo>

    @Query("SELECT * FROM app_info WHERE isSystemApp = :isSystemApp ORDER BY sortOrder ASC")
    suspend fun getAppsByType(isSystemApp: Boolean): List<AppInfo>

//    @Query("SELECT * FROM app_info WHERE sortOrder LIKE :namePattern ORDER BY name ASC")
//    suspend fun searchApps(namePattern: String): List<AppInfo>

    @Query("SELECT * FROM app_info WHERE packageName LIKE :packageNamePattern ORDER BY sortOrder ASC")
    suspend fun searchAppsByPackage(packageNamePattern: String): List<AppInfo>

    @Query("SELECT * FROM app_info ORDER BY lastUsedTime DESC LIMIT :limit")
    suspend fun getRecentApps(limit: Int): List<AppInfo>

    @Query("SELECT * FROM app_info ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsedApps(limit: Int): List<AppInfo>

    @Query("UPDATE app_info SET lastUsedTime = :lastUsedTime, usageCount = usageCount + 1 WHERE id = :id")
    suspend fun updateAppUsage(id: Int, lastUsedTime: Long)

    @Query("SELECT * FROM app_info WHERE id = :id")
    suspend fun getAppById(id: Int): AppInfo?

    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): AppInfo?
    
    /**
     * 更新应用排序顺序
     * @param packageName 包名
     * @param sortOrder 排序顺序
     */
    @Query("UPDATE app_info SET sortOrder = :sortOrder WHERE packageName = :packageName")
    suspend fun updateAppSortOrder(packageName: String, sortOrder: Int)
}
