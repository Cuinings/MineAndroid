package com.cn.board.home.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cn.board.home.entity.AppInfoEntity
import com.cn.board.home.room.converter.EmAppTypeConverter
import com.cn.board.home.room.dao.AppInfoDao
import com.cn.core.ui.application.ApplicationContextExt.context

@Database(
    entities = [AppInfoEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(EmAppTypeConverter::class)
abstract class AppDataBase: RoomDatabase() {

    abstract fun appInfoDao(): AppInfoDao

    companion object {

        private const val DATABASE_NAME = "launcher_db"

        /**
         * Phase 4: 使用 WAL 日志模式（默认），避免 TRUNCATE 模式在崩溃时丢数据。
         * 实现安全的 Migration 替代 fallbackToDestructiveMigration。
         * 当前版本 1 → 无需迁移；升级时在此添加 Migration 对象。
         */
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Room.databaseBuilder(context, AppDataBase::class.java, DATABASE_NAME)
                // WAL is the default and safer than TRUNCATE:
                // - Better concurrent read/write performance
                // - No data loss on crash
                // .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // default, explicit for clarity
                .addMigrations(*ALL_MIGRATIONS)
                .build()
        }

        /**
         * 迁移列表。
         * 当前版本 1，无历史迁移。
         * 版本升级时在此添加 Migration(from, to) 对象。
         *
         * 示例: 1 → 2 迁移
         * val MIGRATION_1_2 = object : Migration(1, 2) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("ALTER TABLE app_table ADD COLUMN new_column TEXT")
         *     }
         * }
         */
        private val ALL_MIGRATIONS: Array<Migration> = emptyArray()
    }
}
