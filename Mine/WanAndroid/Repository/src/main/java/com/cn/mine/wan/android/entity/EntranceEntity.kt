package com.cn.mine.wan.android.entity

import androidx.annotation.StringRes

/**
 * @Author: CuiNing
 * @Time: 2025/6/10 11:54
 * @Description:
 */
data class EntranceEntity(
    @StringRes val name: Int = 0,
    val type: EntranceType = EntranceType.ARTICLE_RECOMMENDATION
)

enum class EntranceType {
    ARTICLE_RECOMMENDATION,
    HARMONY,
    MINE,
    LOGIN,
    LOGOUT,
}
