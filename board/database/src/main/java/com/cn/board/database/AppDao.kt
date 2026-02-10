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

    @Query("SELECT * FROM app_info ORDER BY name ASC")
    suspend fun getAllApps(): List<AppInfo>

    @Query("SELECT * FROM app_info WHERE isSystemApp = :isSystemApp ORDER BY name ASC")
    suspend fun getAppsByType(isSystemApp: Boolean): List<AppInfo>

    @Query("SELECT * FROM app_info WHERE name LIKE :namePattern ORDER BY name ASC")
    suspend fun searchApps(namePattern: String): List<AppInfo>

    @Query("SELECT * FROM app_info WHERE packageName LIKE :packageNamePattern ORDER BY name ASC")
    suspend fun searchAppsByPackage(packageNamePattern: String): List<AppInfo>

    @Query("SELECT * FROM app_info ORDER BY lastUsedTime DESC LIMIT :limit")
    suspend fun getRecentApps(limit: Int): List<AppInfo>

    @Query("SELECT * FROM app_info ORDER BY lastUsedTime DESC LIMIT :limit")
    suspend fun getMostUsedApps(limit: Int): List<AppInfo>

    @Query("UPDATE app_info SET lastUsedTime = :lastUsedTime WHERE id = :id")
    suspend fun updateAppUsage(id: Int, lastUsedTime: Long)

    @Query("SELECT * FROM app_info WHERE id = :id")
    suspend fun getAppById(id: Int): AppInfo?

    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): AppInfo?
}
