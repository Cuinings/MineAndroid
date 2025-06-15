package com.cn.mine.wan.android.entity

import androidx.annotation.StringRes

/**
 * @Author: CuiNing
 * @Time: 2025/6/10 11:54
 * @Description:
 */
data class EntranceEntity(
    @StringRes val name: Int,
    val type: EntranceType
)

enum class EntranceType {
    ARTICLE_RECOMMENDATION,
    HARMONY,
    MINE,
}
