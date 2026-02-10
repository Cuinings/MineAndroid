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
@Database(entities = [AppInfo::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        // 从版本1到版本2的迁移
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 检查是否存在packageName列
                val cursor = database.query("PRAGMA table_info(app_info)")
                val columns = mutableListOf<String>()
                if (cursor.moveToFirst()) {
                    do {
                        columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    } while (cursor.moveToNext())
                }
                cursor.close()
                
                // 添加packageName列（如果不存在）
                if (!columns.contains("packageName")) {
                    database.execSQL("ALTER TABLE app_info ADD COLUMN packageName TEXT DEFAULT ''")
                }
                
                // 添加lastUsedTime列（如果不存在）
                if (!columns.contains("lastUsedTime")) {
                    database.execSQL("ALTER TABLE app_info ADD COLUMN lastUsedTime INTEGER DEFAULT 0")
                }
                
                // 添加usageCount列（如果不存在）
                if (!columns.contains("usageCount")) {
                    database.execSQL("ALTER TABLE app_info ADD COLUMN usageCount INTEGER DEFAULT 0")
                }
            }
        }
    }
}
