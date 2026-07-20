package com.cn.board.meet.home.app

import android.graphics.Bitmap
import androidx.recyclerview.widget.DiffUtil
import com.cn.board.database.AppInfo

data class SoftEntity(
    val appInfo: AppInfo? = null,
    var isSelect: Boolean = false,
    var deleting: Boolean = false,
    var isEdit: Boolean = false,
    var span: Int = 1,
    var bitmap: Bitmap? = null,
    var isShowSelect: Boolean = false,
    /** 占位空槽标记：AppAggregatorMainListLayout 不足 6 项时用来补齐卡片 */
    var isPlaceholder: Boolean = false,
) {
    companion object {
        /**
         * SoftEntity 的 Diff 规则，供 BaseBinderAdapter.setDiffNewData 做增量刷新。
         *
         * - areItemsTheSame：用「包名 + 入口类」判定是否为同一个 App，跨次刷新能稳定识别为同项；
         * - areContentsTheSame：比较展示相关字段，刻意排除 bitmap（Bitmap 无稳定 equals，
         *   且为运行时异步加载，不应触发无谓的内容变更/重绑）。
         */
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SoftEntity>() {
            override fun areItemsTheSame(oldItem: SoftEntity, newItem: SoftEntity): Boolean {
                // 占位空槽与真实 App 必须视为不同项；占位项之间可互换，用同一身份即可。
                if (oldItem.isPlaceholder != newItem.isPlaceholder) return false
                if (oldItem.isPlaceholder) return true
                return oldItem.appInfo?.packageName == newItem.appInfo?.packageName
                    && oldItem.appInfo?.clazz == newItem.appInfo?.clazz
            }

            override fun areContentsTheSame(oldItem: SoftEntity, newItem: SoftEntity): Boolean {
                if (oldItem.isPlaceholder != newItem.isPlaceholder) return false
                if (oldItem.isPlaceholder) return true
                // 仅比较“会展示”的字段，故意不比较 appInfo 整体：
                // appInfo.orderIndex 会在拖拽持久化时被原地改写，若纳入比较会导致拖拽后
                // 多余的内容重绑/闪烁，故 orderIndex 不比较。
                // bitmap 也不比较（异步加载、且已在 init 阶段同步载入，不应触发无谓重绑）。
                // 注意：main/offlineMain/mainIndex/offlineMainIndex 决定「选择角标」是否显示，
                // 属于展示字段，必须纳入比较，否则管理模式下点击切换标记后角标不会刷新。
                return oldItem.appInfo?.name == newItem.appInfo?.name
                    && oldItem.appInfo?.main == newItem.appInfo?.main
                    && oldItem.appInfo?.offlineMain == newItem.appInfo?.offlineMain
                    && oldItem.appInfo?.mainIndex == newItem.appInfo?.mainIndex
                    && oldItem.appInfo?.offlineMainIndex == newItem.appInfo?.offlineMainIndex
                    && oldItem.isSelect == newItem.isSelect
                    && oldItem.deleting == newItem.deleting
                    && oldItem.isEdit == newItem.isEdit
                    && oldItem.isShowSelect == newItem.isShowSelect
                    && oldItem.span == newItem.span
            }
        }
    }
}