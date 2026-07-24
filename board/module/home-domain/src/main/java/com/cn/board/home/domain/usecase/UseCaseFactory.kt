package com.cn.board.home.domain.usecase

import android.content.pm.PackageManager
import com.cn.board.home.entity.AppInfoEntity
import com.cn.board.home.room.repsoitory.AppInfoRepository

/**
 * UseCase 工厂，负责创建和注入依赖。
 *
 * Phase 2: 作为从旧 Handler 链到新 UseCase 模式过渡的桥梁。
 * Phase 4: 替换为 Hilt DI。
 */
object UseCaseFactory {

    private var _syncDb: SyncAppDatabaseUseCase? = null
    private var _buildHome: BuildHomeAppListUseCase? = null
    private var _manageOrder: ManageHomeAppOrderUseCase? = null

    val initialized: Boolean get() = _syncDb != null

    fun init(
        repository: AppInfoRepository,
        packageManager: PackageManager,
        defaultAppList: List<AppInfoEntity>,
        filterPackages: Set<String>,
    ) {
        _syncDb = SyncAppDatabaseUseCase(repository, packageManager, defaultAppList, filterPackages)
        _buildHome = BuildHomeAppListUseCase(repository)
        _manageOrder = ManageHomeAppOrderUseCase(repository)
    }

    val syncAppDatabase: SyncAppDatabaseUseCase
        get() = _syncDb ?: throw IllegalStateException("UseCaseFactory not initialized. Call init() first.")

    val buildHomeAppList: BuildHomeAppListUseCase
        get() = _buildHome ?: throw IllegalStateException("UseCaseFactory not initialized. Call init() first.")

    val manageHomeAppOrder: ManageHomeAppOrderUseCase
        get() = _manageOrder ?: throw IllegalStateException("UseCaseFactory not initialized. Call init() first.")
}
