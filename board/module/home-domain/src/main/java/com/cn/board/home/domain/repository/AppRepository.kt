package com.cn.board.home.domain.repository

import com.cn.board.home.entity.AppInfoEntity

/**
 * 应用数据仓库接口。
 *
 * 定义在 Domain 层，由 Data 层的 [AppInfoRepository] 实现。
 * UseCase 依赖此接口而非具体实现，实现依赖倒置。
 */
interface AppRepository {

    suspend fun queryAll(): List<AppInfoEntity>

    suspend fun queryById(id: Int): List<AppInfoEntity>

    suspend fun queryByPackageName(packageName: String): List<AppInfoEntity>

    suspend fun queryByMain(main: Int): List<AppInfoEntity>

    suspend fun queryByOfflineMain(value: Int): List<AppInfoEntity>

    suspend fun insert(app: AppInfoEntity)

    suspend fun insertBatch(apps: List<AppInfoEntity>)

    suspend fun update(app: AppInfoEntity?)

    suspend fun deleteAll()

    suspend fun deleteById(id: Int)

    suspend fun deleteByPackageName(packageName: String)
}
