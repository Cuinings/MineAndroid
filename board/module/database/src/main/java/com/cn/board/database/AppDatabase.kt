package com.cn.board.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * @author: cn
 * @time: 2026/2/10 10:00
 * @history
 * @description: Room数据库配置
 */
@Database(entities = [AppInfo::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        
        // 从版本1到版本2的迁移
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 开始事务
                database.beginTransaction()
                try {
                    // 检查现有列
                    val columns = mutableListOf<String>()
                    var cursor = database.query("PRAGMA table_info(app_info)")
                    try {
                        if (cursor.moveToFirst()) {
                            do {
                                val columnNameIndex = cursor.getColumnIndexOrThrow("name")
                                if (!cursor.isNull(columnNameIndex)) {
                                    columns.add(cursor.getString(columnNameIndex))
                                }
                            } while (cursor.moveToNext())
                        }
                    } finally {
                        cursor.close()
                    }
                    
                    // 添加必需的列（如果不存在）
                    if (!columns.contains("packageName")) {
                        database.execSQL("ALTER TABLE app_info ADD COLUMN packageName TEXT DEFAULT ''")
                    }
                    
                    if (!columns.contains("lastUsedTime")) {
                        database.execSQL("ALTER TABLE app_info ADD COLUMN lastUsedTime INTEGER DEFAULT 0")
                    }

                    if (!columns.contains("isSystemApp")) {
                        database.execSQL("ALTER TABLE app_info ADD COLUMN isSystemApp INTEGER DEFAULT 0")
                    }
                    
                    if (!columns.contains("usageCount")) {
                        database.execSQL("ALTER TABLE app_info ADD COLUMN usageCount INTEGER DEFAULT 0")
                    }
                    
                    if (!columns.contains("sortOrder")) {
                        database.execSQL("ALTER TABLE app_info ADD COLUMN sortOrder INTEGER DEFAULT 0")
                    }
                    
                    if (!columns.contains("appFlag")) {
                        database.execSQL("ALTER TABLE app_info ADD COLUMN appFlag INTEGER DEFAULT 0")
                    }
                    
                    // 事务成功
                    database.setTransactionSuccessful()
                } catch (e: Exception) {
                    // 记录异常
                    e.printStackTrace()
                    throw e
                } finally {
                    // 结束事务
                    database.endTransaction()
                }
            }
        }
    }
}