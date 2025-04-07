package com.cn.sample.hilt.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cn.sample.hilt.room.User
import com.cn.sample.hilt.room.UserDao

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