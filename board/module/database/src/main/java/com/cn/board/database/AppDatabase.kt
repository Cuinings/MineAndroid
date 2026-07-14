package com.cn.board.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * @author: cn
 * @time: 2026/2/10 10:00
 * @history
 * @description: Room数据库配置
 */
@Database(
    entities = [AppInfo::class, CommonNode::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(CommonNodeTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun commonNodeDao(): CommonNodeDao

    companion object {

    }
}