package com.cn.board.home.entity.diff

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.cn.board.home.entity.SoftEntity

class SoftDiffCallBack: DiffUtil.ItemCallback<SoftEntity>() {

    override fun areItemsTheSame(oldItem: SoftEntity, newItem: SoftEntity): Boolean {
        return oldItem.appInfo?.packageName == newItem.appInfo?.packageName && oldItem.appInfo?.clazz == newItem.appInfo?.clazz
    }

    override fun areContentsTheSame(oldItem: SoftEntity, newItem: SoftEntity): Boolean {
        return oldItem.appInfo?.main == newItem.appInfo?.main
                && oldItem.appInfo?.offlineMain == newItem.appInfo?.offlineMain
                && oldItem.appInfo?.allowDelete == newItem.appInfo?.allowDelete
                && oldItem.appInfo?.appType == newItem.appInfo?.appType
                && oldItem.isSelect == newItem.isSelect
                && oldItem.isEdit == newItem.isEdit
    }

    override fun getChangePayload(oldItem: SoftEntity, newItem: SoftEntity): Any? {
        val diff = Bundle()
        if (oldItem.appInfo?.main != newItem.appInfo?.main) diff.putInt("main", newItem.appInfo?.main?:0)
        if (oldItem.appInfo?.offlineMain != newItem.appInfo?.offlineMain) diff.putInt("offlineMain", newItem.appInfo?.offlineMain?:0)
        if (oldItem.appInfo?.packageName != newItem.appInfo?.packageName) diff.putString("packageName", newItem.appInfo?.packageName?:"")
        if (oldItem.appInfo?.clazz != newItem.appInfo?.clazz) diff.putString("clazz", newItem.appInfo?.clazz?:"")
        if (oldItem.appInfo?.appType != newItem.appInfo?.appType) diff.putInt("appType", newItem.appInfo?.appType?.ordinal?:4)
        if (oldItem.isSelect != newItem.isSelect) diff.putBoolean("isSelect", newItem.isSelect)
        if (oldItem.isEdit != newItem.isEdit) diff.putBoolean("isEdit", newItem.isEdit)
        return if (diff.size() > 0) diff else super.getChangePayload(oldItem, newItem)
    }

}
