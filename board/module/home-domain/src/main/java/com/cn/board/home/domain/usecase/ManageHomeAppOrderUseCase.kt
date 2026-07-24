package com.cn.board.home.domain.usecase

import com.cn.board.home.domain.state.SessionStateHolder
import com.cn.board.home.entity.SoftEntity
import com.cn.board.home.room.repsoitory.AppInfoRepository

/**
 * 管理首页应用排序和选择。
 *
 * Phase 2: 替代 [HomeActivityViewModel.updateHomeAppList] 中的核心逻辑。
 */
class ManageHomeAppOrderUseCase(
    private val repository: AppInfoRepository,
) {

    companion object {
        private const val MAX_HOME_APPS = 6
        const val INDEX_UNSET = 10000
    }

    /**
     * 将应用添加到首页或从首页移除。
     *
     * @param entity 目标应用实体
     * @param currentHomeApps 当前首页应用列表
     * @return 更新后的首页应用列表
     */
    suspend fun toggleHomeApp(
        entity: SoftEntity,
        currentHomeApps: List<SoftEntity>,
    ): List<SoftEntity> {
        val appInfo = entity.appInfo ?: return currentHomeApps
        val isOnline = SessionStateHolder.isOpenAPS ||
                SessionStateHolder.commandDispatcherModel ||
                SessionStateHolder.mcuModel

        if (isOnline) {
            appInfo.main = if (appInfo.main == 0 && currentHomeApps.size < MAX_HOME_APPS) 1 else 0
        } else {
            appInfo.offlineMain = if (appInfo.offlineMain == 0 && currentHomeApps.size < MAX_HOME_APPS) 1 else 0
        }

        repository.update(appInfo)
        return currentHomeApps // Phase 3: return refreshed list from DB
    }

    /**
     * 将应用从首页移除。
     *
     * @param entity 要移除的应用
     */
    suspend fun removeFromHome(entity: SoftEntity) {
        entity.appInfo?.let { info ->
            if (SessionStateHolder.isOpenAPS) {
                info.main = 0
            } else {
                info.offlineMain = 0
            }
            repository.update(info)
        }
    }

    /**
     * 重新排列首页应用索引。
     *
     * @param reordered 重排后的应用列表
     */
    suspend fun reorderHomeApps(reordered: List<SoftEntity>) {
        reordered.forEachIndexed { index, entity ->
            entity.appInfo?.let { info ->
                info.mainIndex = index
                repository.update(info)
            }
        }
    }
}
