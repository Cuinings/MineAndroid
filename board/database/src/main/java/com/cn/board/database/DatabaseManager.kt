package com.cn.board.database

import android.content.Context
import androidx.room.Room
import android.util.Log

/**
 * @author: cn
 * @time: 2026/2/10 10:00
 * @history
 * @description: 数据库管理类，负责初始化数据库并提供 DAO 实例
 */
object DatabaseManager {
    private var db: AppDatabase? = null
    private var appDao: AppDao? = null

    /**
     * 初始化数据库
     */
    fun initDatabase(context: Context) {
        if (db == null) {
            Log.d("DatabaseManager", "开始初始化数据库")
            db = Room.databaseBuilder(
                context.applicationContext, // 使用 Application 上下文避免内存泄露
                AppDatabase::class.java,
                "board_database"
            )
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .fallbackToDestructiveMigration(true) // 添加破坏性迁移作为最后的保障
                .build()
            appDao = db?.appDao()
            Log.d("DatabaseManager", "数据库初始化完成")
        }
    }

    /**
     * 获取 AppDao 实例（自动初始化）
     */
    fun getAppDao(context: Context? = null): AppDao {
        if (appDao == null) {
            context?.let {
                Log.d("DatabaseManager", "DAO 未初始化，自动初始化数据库")
                initDatabase(it)
            }
            check(appDao != null) { "Database not initialized. Call initDatabase() first or provide context." }
        }
        return appDao!!
    }

    /**
     * 关闭数据库连接
     */
    fun closeDatabase() {
        Log.d("DatabaseManager", "关闭数据库连接")
        db?.close()
        db = null
        appDao = null
    }

    /**
     * 检查数据库是否已初始化
     */
    fun isInitialized(): Boolean {
        return appDao != null
    }
}