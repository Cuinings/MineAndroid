package com.cn.board.home.domain.usecase

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.cn.board.home.entity.AppInfoEntity
import com.cn.board.home.entity.EmAppType
import com.cn.board.home.room.repsoitory.AppInfoRepository
import com.cn.board.home.util.PackageName.PACKAGE_WELCOME_SIGN
import com.cn.board.home.util.PackageName.PACKAGE_WPS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 扫描已安装应用并同步到数据库。
 *
 * 替代 [SoftSynHandler] 的核心逻辑。
 * Phase 2: 将数据库操作集中在 repository 中，使用 IO 线程执行包扫描。
 */
class SyncAppDatabaseUseCase(
    private val repository: AppInfoRepository,
    private val packageManager: PackageManager,
    private val defaultAppList: List<AppInfoEntity>,
    private val filterPackages: Set<String>,
) {

    /**
     * 执行应用同步：检查默认应用 + 扫描已安装包 + 写入数据库。
     *
     * @return 同步后的完整应用列表
     */
    suspend fun execute(): List<AppInfoEntity> = withContext(Dispatchers.IO) {
        checkDefaultApps()
        scanInstalledPackages()
        repository.query().toList()
    }

    private suspend fun checkDefaultApps() {
        defaultAppList.forEach { defaultApp ->
            val existing = repository.queryByPackageName(defaultApp.packageName)
            val hasMatch = existing.any {
                it.packageName == defaultApp.packageName && it.clazz == defaultApp.clazz
            }
            if (!hasMatch) {
                repository.insert(defaultApp)
            }
        }
    }

    private suspend fun scanInstalledPackages() {
        val packages = safeGetInstalledPackages()
        packages.forEach { pkgInfo ->
            if (!isSystemApp(pkgInfo) && pkgInfo.packageName !in filterPackages) {
                val existing = repository.queryByPackageName(pkgInfo.packageName)
                if (existing.isEmpty()) {
                    val entity = createEntityFromPackage(pkgInfo)
                    repository.insert(entity)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun safeGetInstalledPackages(): List<PackageInfo> {
        return packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
    }

    private fun isSystemApp(pkgInfo: PackageInfo): Boolean {
        if (pkgInfo.packageName == PACKAGE_WPS || pkgInfo.packageName == PACKAGE_WELCOME_SIGN) {
            return false
        }
        if (pkgInfo.applicationInfo?.sourceDir?.contains("/skyuser/respack/android/app/") == true) {
            return false
        }
        return (pkgInfo.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) ?: 0) > 0
    }

    private fun createEntityFromPackage(pkgInfo: PackageInfo): AppInfoEntity {
        return AppInfoEntity().apply {
            packageName = pkgInfo.packageName
            allowDelete = if (pkgInfo.packageName == PACKAGE_WPS) {
                false
            } else {
                pkgInfo.applicationInfo?.sourceDir?.contains("/skyuser/respack/android/app/") != true
            }
        }
    }
}
