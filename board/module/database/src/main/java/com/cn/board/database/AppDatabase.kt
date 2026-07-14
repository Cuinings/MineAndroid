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
    version = 3,
    exportSchema = true
)
@TypeConverters(CommonNodeTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun commonNodeDao(): CommonNodeDao

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

        // 从版本2到版本3的迁移：新增 common_node 表
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS common_node (
                        id TEXT NOT NULL PRIMARY KEY,
                        nodeType TEXT NOT NULL,
                        resourceType INTEGER NOT NULL,
                        parentId TEXT,
                        deptId TEXT,
                        name TEXT NOT NULL,
                        userId TEXT,
                        lastUpdated INTEGER NOT NULL DEFAULT 0
                    )
                """)
                // 创建常用索引
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_common_node_nodeType ON common_node(nodeType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_common_node_resourceType ON common_node(resourceType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_common_node_parentId ON common_node(parentId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_common_node_deptId ON common_node(deptId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_common_node_userId ON common_node(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_common_node_type_nodetype ON common_node(resourceType, nodeType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_common_node_type_dept_nodetype ON common_node(resourceType, deptId, nodeType)")
            }
        }
    }
}