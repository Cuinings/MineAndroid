package com.cn.board.home.domain.usecase

import com.cn.board.home.domain.state.SessionStateHolder
import com.cn.board.home.entity.AppInfoEntity
import com.cn.board.home.entity.EmAppType
import com.cn.board.home.entity.SoftEntity
import com.cn.board.home.room.repsoitory.AppInfoRepository
import com.cn.board.home.util.PackageName.PACKAGE_CONFERENCE
import com.cn.board.home.util.ActivityClassName.APP_VOD

/**
 * 构建首页应用列表。
 *
 * Phase 2: 替代 [MainAppInitHandler] 的核心逻辑。
 * 根据登录状态（APS/离线）查询主应用，构建首页 6 格应用列表。
 */
class BuildHomeAppListUseCase(
    private val repository: AppInfoRepository,
) {

    companion object {
        private const val MAX_HOME_APPS = 6

        val addEntity = SoftEntity(AppInfoEntity(appType = EmAppType.Add).apply {
            mainIndex = Int.MAX_VALUE
        })
        val noneEntity = SoftEntity(AppInfoEntity(appType = EmAppType.NONE).apply {
            mainIndex = Int.MAX_VALUE
        })
    }

    /**
     * 构建首页应用列表。
     *
     * @return Pair(homeList, mainAppList) 首页 6 格列表 + 全部主应用列表
     */
    suspend fun execute(): Pair<List<SoftEntity>, List<SoftEntity>> {
        val isOnline = SessionStateHolder.isOpenAPS ||
                SessionStateHolder.commandDispatcherModel ||
                SessionStateHolder.mcuModel

        val mainEntities = if (isOnline) {
            repository.queryByMain(1)
        } else {
            repository.queryOfflineMain(1)
        }

        val mainAppList = mainEntities.map { entity ->
            SoftEntity(entity).also { applyFilter(it) }
        }.toMutableList()

        val homeList = buildHomeList(mainAppList.toList(), isOnline)
        return Pair(homeList, mainAppList)
    }

    private fun buildHomeList(mainApps: List<SoftEntity>, isOnline: Boolean): List<SoftEntity> {
        val home = ArrayList<SoftEntity>()

        mainApps.forEach { entity ->
            if (home.size < MAX_HOME_APPS) {
                entity.appInfo?.let { info ->
                    val shouldAdd = if (info.packageName == PACKAGE_CONFERENCE && info.clazz == APP_VOD) {
                        SessionStateHolder.vrsLoginState
                    } else {
                        true
                    }
                    if (shouldAdd) home.add(entity)
                }
            }
        }

        return home
    }

    private fun applyFilter(entity: SoftEntity) {
        // Placeholder for icon loading and additional filtering logic
        // Phase 3: Move icon loading to a separate IconLoader utility
    }

    /**
     * 用占位实体填充首页列表到 6 格。
     */
    fun fillToSix(list: MutableList<SoftEntity>): List<SoftEntity> {
        val result = ArrayList(list)
        while (result.size < MAX_HOME_APPS) {
            result.add(noneEntity)
        }
        return result
    }
}
