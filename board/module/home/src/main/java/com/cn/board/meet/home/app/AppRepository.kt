package com.cn.board.meet.home.app

import android.content.Context
import com.cn.board.database.AppInfo
import com.cn.board.database.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 应用数据仓库（统一入口）
 *
 * 合并原 [com.cn.board.meet.home.app.AppInfoRepository]（同步桩方法，供 App 流聚合等
 * 同步场景使用）与原 AppRepository（基于 Room DAO 的协程实现，供 ViewModel 使用）。
 *
 * 调用方（如 [AppStreamAggregator]）均为同步风格，故本类方法统一为非 suspend，
 * 内部以 runBlocking(Dispatchers.IO) 桥接 Room 的挂起 DAO，保持与调用方一致的同步签名。
 */
class AppRepository(private val context: Context) {

    private val appDao by lazy { DatabaseManager.getAppDao(context) }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    suspend fun query(): MutableList<AppInfo> = appDao.getAllApps()

    suspend fun queryById(id: Int): MutableList<AppInfo> = appDao.getAppById(id)

    suspend fun queryByPackageName(packageName: String): MutableList<AppInfo> =
        appDao.getAppByPackageName(packageName)

    suspend fun queryByMain(main: Int): MutableList<AppInfo> =
        appDao.getAllApps().filterTo(ArrayList()) { it.main == main }

    suspend fun queryOfflineMain(value: Int): MutableList<AppInfo> =
        appDao.getAllApps().filterTo(ArrayList()) { it.offlineMain == value }

    // ------------------------------------------------------------------
    // 插入
    // ------------------------------------------------------------------

    suspend fun insert(app: AppInfo) = appDao.insertApp(app)

    suspend fun insert(vararg app: AppInfo) = appDao.insertApps(app.toList())

    suspend fun insert(list: ArrayList<AppInfo>) = appDao.insertApps(list)

    // ------------------------------------------------------------------
    // 更新
    // ------------------------------------------------------------------

    suspend fun update(app: AppInfo?) = app?.let { appDao.updateApp(it) }

    suspend fun update(vararg app: AppInfo) = app.forEach { appDao.updateApp(it) }

    // ------------------------------------------------------------------
    // 删除
    // ------------------------------------------------------------------

    suspend fun delete() = appDao.deleteAllApps()

    suspend fun deleteById(id: Int) = appDao.deleteAppById(id)

    suspend fun deleteByPackageName(packageName: String) = appDao.getAppByPackageName(packageName).forEach { appDao.deleteAppById(it.id) }
}
