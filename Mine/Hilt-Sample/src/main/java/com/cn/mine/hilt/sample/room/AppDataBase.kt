package com.cn.mine.hilt.sample.room

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 13:42
 * @Description:
 */
@Database(
    entities = [User::class],
    version = 1,
    exportSchema = true
)
abstract class AppDataBase: RoomDatabase() {
    abstract fun userDao(): UserDao
}