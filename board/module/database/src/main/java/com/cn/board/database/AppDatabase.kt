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
    exportSchema = false
)
@TypeConverters(CommonNodeTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun commonNodeDao(): CommonNodeDao

    companion object {
        /** 1 → 2：新增 orderIndex 列（首页展示顺序）。存量行默认 -1，由 AppStreamAggregator.init() 惰性回填 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_info ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT -1")
            }
        }

        /**
         * 2 → 3：迭代过程中 AppInfo 的字段默认值/结构发生变化，导致设备库 identity hash
         * 与当前编译产物不一致（Room 报 "Room cannot verify the data integrity"）。
         *
         * 采用「按列名重建表」方式，使 on-device schema 与当前 entities 完全一致：
         *  - 旧表中存在的列会被原样拷贝（尽可能保留数据）；
         *  - 旧表缺失的列取实体默认值；
         *  - 即便旧表结构完全不同也不会抛异常（只拷贝交集列）。
         *
         * 建表 SQL 与 AppDatabase_Impl 中生成的 CREATE TABLE app_info 逐字一致，
         * 保证迁移后 schema 校验通过。若极端情况下校验仍未过，
         * DatabaseManager 中已配置的 fallbackToDestructiveMigration 会兜底重建。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(app_info)")
                val oldColumns = LinkedHashSet<String>()
                while (cursor.moveToNext()) {
                    oldColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                val targetColumns = listOf(
                    "id", "clazz", "packageName", "versionCode", "versionName",
                    "appType", "name", "allowDelete", "orderIndex", "main",
                    "mainIndex", "offlineMain", "offlineMainIndex"
                )
                // 各列在「旧表缺失 / 旧值为 NULL」时使用的兜底值（与实体默认值一致）
                val fallback = mapOf(
                    "clazz" to "''",
                    "packageName" to "''",
                    "versionCode" to "''",
                    "versionName" to "''",
                    "allowDelete" to "1",
                    "orderIndex" to "-1",
                    "main" to "0",
                    "mainIndex" to "0",
                    "offlineMain" to "0",
                    "offlineMainIndex" to "0"
                )

                db.execSQL(
                    """CREATE TABLE app_info_new (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `clazz` TEXT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `versionCode` TEXT NOT NULL,
                        `versionName` TEXT NOT NULL,
                        `appType` TEXT NOT NULL,
                        `name` TEXT,
                        `allowDelete` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL DEFAULT -1,
                        `main` INTEGER NOT NULL,
                        `mainIndex` INTEGER NOT NULL DEFAULT 0,
                        `offlineMain` INTEGER NOT NULL,
                        `offlineMainIndex` INTEGER NOT NULL DEFAULT 0
                    )"""
                )

                // 为每个目标列构造取值表达式，保证 NOT NULL 列永远拿到非 NULL 值：
                //  - id：主键，必然存在，直接拷贝
                //  - name：可空，旧表没有则写 NULL
                //  - appType：枚举按名存储，强制合法默认值 'Third'（启动时由包扫描重新派生，避免旧值解析崩溃）
                //  - 其余：旧表有则 COALESCE(col, 默认值) 填 NULL，旧表没有则直接用默认值
                val insertCols = targetColumns.joinToString(", ")
                val selectExprs = targetColumns.map { col ->
                    when (col) {
                        "id" -> "id"
                        "name" -> if ("name" in oldColumns) "name" else "NULL"
                        "appType" -> "'Third'"
                        else -> {
                            val def = fallback[col] ?: "''"
                            if (col in oldColumns) "COALESCE(`$col`, $def)" else def
                        }
                    }
                }
                val selectList = selectExprs.joinToString(", ")
                db.execSQL("INSERT INTO app_info_new ($insertCols) SELECT $selectList FROM app_info")

                db.execSQL("DROP TABLE app_info")
                db.execSQL("ALTER TABLE app_info_new RENAME TO app_info")
            }
        }
    }
}