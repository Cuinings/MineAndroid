package com.cn.board.home.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cn.board.home.entity.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {

    // === 写入操作 (suspend) ===

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppInfoEntity)
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg apps: AppInfoEntity)
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ArrayList<AppInfoEntity>)
    @Transaction
    @Query("DELETE FROM app_table")
    suspend fun delete()
    @Transaction
    @Query("DELETE FROM app_table WHERE id = (:id)")
    suspend fun deleteById(id: Int)
    @Transaction
    @Query("DELETE FROM app_table WHERE packageName = (:packageName)")
    suspend fun deleteByPackageName(packageName: String)

    @Transaction
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(app: AppInfoEntity)
    @Transaction
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg apps: AppInfoEntity)

    // === 一次性查询 (suspend, 保留兼容) ===

    @Transaction
    @Query("SELECT * FROM app_table ORDER by id")
    suspend fun query(): MutableList<AppInfoEntity>
    @Transaction
    @Query("SELECT * FROM app_table WHERE id = (:id) ORDER by ID")
    suspend fun queryById(id: Int): MutableList<AppInfoEntity>
    @Transaction
    @Query("SELECT * FROM app_table WHERE packageName = (:packageName) ORDER by ID")
    suspend fun queryByPackageName(packageName: String): MutableList<AppInfoEntity>
    @Transaction
    @Query("SELECT * FROM app_table WHERE main = (:main) ORDER by mainIndex")
    suspend fun queryByMain(main: Int): MutableList<AppInfoEntity>
    @Transaction
    @Query("SELECT * FROM app_table WHERE offlineMain = (:value)  ORDER by offlineMainIndex")
    suspend fun queryOfflineMain(value: Int): MutableList<AppInfoEntity>

    // === Flow 持续观察 (新增, Room 自动在写入时重发) ===

    @Query("SELECT * FROM app_table ORDER by id")
    fun observeAll(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_table WHERE main = (:main) ORDER by mainIndex")
    fun observeByMain(main: Int): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_table WHERE offlineMain = (:value) ORDER by offlineMainIndex")
    fun observeByOfflineMain(value: Int): Flow<List<AppInfoEntity>>
}
