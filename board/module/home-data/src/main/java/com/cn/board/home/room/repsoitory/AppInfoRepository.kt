package com.cn.board.home.room.repsoitory

import com.cn.board.home.data.state.HomeUiState
import com.cn.board.home.entity.AppInfoEntity
import com.cn.board.home.entity.EmAppType
import com.cn.board.home.entity.SoftEntity
import com.cn.board.home.room.AppDataBase
import com.cn.board.home.room.dao.AppInfoDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

val repository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    AppInfoRepository()
}

class AppInfoRepository {

    private var dao: AppInfoDao = AppDataBase.instance.appInfoDao()

    // === suspend 查询 (保留兼容) ===

    suspend fun query(): MutableList<AppInfoEntity> = dao.query()
    suspend fun queryById(id: Int): MutableList<AppInfoEntity> = dao.queryById(id)
    suspend fun queryByPackageName(packageName: String): MutableList<AppInfoEntity> = dao.queryByPackageName(packageName)
    suspend fun queryByMain(main: Int): MutableList<AppInfoEntity> = dao.queryByMain(main)
    suspend fun queryOfflineMain(value: Int): MutableList<AppInfoEntity> = dao.queryOfflineMain(value)

    // === 写入操作 ===

    suspend fun insert(app: AppInfoEntity) { dao.insert(app) }
    suspend fun insert(vararg app: AppInfoEntity) { dao.insert(*app) }
    suspend fun insert(list: ArrayList<AppInfoEntity>) { dao.insert(list) }
    suspend fun update(app: AppInfoEntity?) { app?.let { dao.update(it) } }
    suspend fun update(vararg app: AppInfoEntity) { dao.update(*app) }
    suspend fun delete() { dao.delete() }
    suspend fun deleteById(id: Int) { dao.deleteById(id) }
    suspend fun deleteByPackageName(packageName: String) { dao.deleteByPackageName(packageName) }

    // === Flow 持续观察 ===

    /**
     * 观察 Home UI 状态。
     * @param isOnlineFlow 在线/离线状态流（由调用方注入，避免跨模块循环依赖）
     */
    fun observeHomeUiState(isOnlineFlow: Flow<Boolean>): Flow<HomeUiState> {
        return dao.observeAll()
            .combine(isOnlineFlow) { entities, isOnline ->
                buildHomeUiState(entities, isOnline)
            }
            .onStart { emit(HomeUiState(isLoading = true)) }
    }

    private fun buildHomeUiState(
        entities: List<AppInfoEntity>,
        isOnline: Boolean,
    ): HomeUiState {
        val allSoft = entities.map { SoftEntity(it) }

        val mainApps = allSoft.filter { entity ->
            if (isOnline) entity.appInfo?.main == 1
            else entity.appInfo?.offlineMain == 1
        }

        val paddedHome = fillHomeListToSix(mainApps)

        return HomeUiState(
            allApps = allSoft,
            homeAppList = paddedHome,
            mainAppList = mainApps,
            isLoading = false,
        )
    }

    companion object {
        private const val MAX_HOME = 6

        val noneEntity = SoftEntity(AppInfoEntity(appType = EmAppType.NONE).apply {
            mainIndex = Int.MAX_VALUE
        })

        fun fillHomeListToSix(list: List<SoftEntity>): List<SoftEntity> {
            val result = ArrayList(list)
            while (result.size < MAX_HOME) result.add(noneEntity)
            return result
        }
    }
}
