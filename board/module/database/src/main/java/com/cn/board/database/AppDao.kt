package com.cn.board.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApp(appInfo: AppInfo)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApps(apps: List<AppInfo>)

    @Update
    suspend fun updateApp(appInfo: AppInfo)

    /** 按主键 id 更新展示顺序 orderIndex（拖拽重排只改此列，不碰主键，无碰撞风险） */
    @Query("UPDATE app_info SET orderIndex = :value WHERE id = :id")
    suspend fun updateOrderIndex(id: Int, value: Int)

    @Query("DELETE FROM app_info WHERE id = :id")
    suspend fun deleteAppById(id: Int)

    @Query("DELETE FROM app_info")
    suspend fun deleteAllApps()

    @Query("SELECT * FROM app_info")
    suspend fun getAllApps(): MutableList<AppInfo>
    @Query("SELECT * FROM app_info WHERE main = (:main) ORDER by mainIndex")
    suspend fun getAppsByMain(main: Int): MutableList<AppInfo>
    @Query("SELECT * FROM app_info WHERE main = (:main) ORDER by offlineMainIndex")
    suspend fun getAppsByOfflineMain(main: Int): MutableList<AppInfo>

    @Query("SELECT * FROM app_info WHERE offlineMain = :id")
    suspend fun getAppById(id: Int): MutableList<AppInfo>

    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): MutableList<AppInfo>

}
