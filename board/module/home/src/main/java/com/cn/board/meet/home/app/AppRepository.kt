package com.cn.board.meet.home.app

import android.content.Context
import com.cn.board.database.AppInfo
import com.cn.board.database.DatabaseManager

/**
 * 应用数据仓库（统一入口）
 *
 * 合并原 [com.cn.board.meet.home.app.AppInfoRepository]（同步桩方法，供 App 流聚合等
 * 同步场景使用）与原 AppRepository（基于 Room DAO 的协程实现，供 ViewModel 使用）。
 */
class AppRepository(private val context: Context) {

    private val appDao by lazy { DatabaseManager.getAppDao(context) }

    // ------------------------------------------------------------------
    // 原 AppInfoRepository 方法（同步桩，保持原行为不变）
    // ------------------------------------------------------------------

    suspend fun query(): MutableList<AppInfo> {
        return appDao.getAllApps()
    }

    suspend fun queryById(id: Int): MutableList<AppInfo> {
        return appDao.getAppById(id)
    }

    suspend fun queryByPackageName(packageName: String): MutableList<AppInfo> {
        return appDao.getAppByPackageName(packageName)
    }

    fun queryByMain(main: Int): MutableList<AppInfo> {
        return mutableListOf()
    }

    fun queryOfflineMain(value: Int): MutableList<AppInfo> = mutableListOf()

    fun insert(app: AppInfo) {
    }

    fun insert(vararg app: AppInfo) {
    }

    fun insert(list: ArrayList<AppInfo>) {
    }

    fun update(app: AppInfo?) {
    }

    fun update(vararg app: AppInfo) {
    }

    fun delete() {
    }

    fun deleteById(id: Int) {
    }

    fun deleteByPackageName(packageName: String) {
    }
}