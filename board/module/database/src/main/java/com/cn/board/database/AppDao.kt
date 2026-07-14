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

    @Query("SELECT * FROM app_info")
    suspend fun getAllApps(): MutableList<AppInfo>

    @Query("SELECT * FROM app_info WHERE id = :id")
    suspend fun getAppById(id: Int): MutableList<AppInfo>

    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): MutableList<AppInfo>

}
