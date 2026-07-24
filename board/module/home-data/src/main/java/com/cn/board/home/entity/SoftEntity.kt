package com.cn.board.home.entity

import android.graphics.Bitmap

/**
 * 软件实体数据类。
 *
 * Phase 1: 从 presentation 层解耦，移除对 State / AppInfoData 的直接依赖。
 * isShowSelect 的计算逻辑将在 Phase 2 通过 UseCase 提供。
 */
data class SoftEntity(
    val appInfo: AppInfoEntity? = null
): Comparable<SoftEntity> {

    var isSelect: Boolean = false
    var deleting = false
    var isEdit: Boolean = false
    var span: Int = 1
    var bitmap: Bitmap? = null

    /**
     * Phase 2 TODO: 该字段原先通过 State.isOpenAPS / appInfoData.mainAppListSize 动态计算，
     * 重构后由 ManageHomeAppsUseCase 在 ViewModel 层主动赋值。
     */
    var isShowSelect: Boolean = false

    override fun compareTo(other: SoftEntity): Int =
        if (other.appInfo?.main == 0 && this.appInfo?.main == 0) {
            if (this.appInfo?.id!! > other.appInfo?.id!!) 1
            else if (this.appInfo?.id!! == other.appInfo?.id!!) 0
            else -1
        } else {
            if (this.appInfo?.mainIndex!! > other.appInfo?.mainIndex!!) 1
            else if (this.appInfo?.mainIndex!! == other.appInfo?.mainIndex!!) 0
            else -1
        }

    fun copy(): SoftEntity {
        return SoftEntity(appInfo)
    }
}
