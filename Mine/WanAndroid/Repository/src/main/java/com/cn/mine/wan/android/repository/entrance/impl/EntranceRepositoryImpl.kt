package com.cn.mine.wan.android.repository.entrance.impl

import com.cn.mine.wan.android.entity.EntranceEntity
import com.cn.mine.wan.android.entity.EntranceType
import com.cn.mine.wan.android.repository.R
import com.cn.mine.wan.android.repository.entrance.EntranceRepository

/**
 * @Author: CuiNing
 * @Time: 2025/6/15 19:29
 * @Description:
 */
class EntranceRepositoryImpl: EntranceRepository {
    override suspend fun entrance(): Result<MutableList<EntranceEntity>> = Result.success(arrayListOf<EntranceEntity>().apply {
        add(EntranceEntity(name = R.string.str_entrance_article_recommendation, type = EntranceType.ARTICLE_RECOMMENDATION))
        add(EntranceEntity(name = R.string.str_entrance_harmony, type = EntranceType.HARMONY))
        add(EntranceEntity(name = R.string.str_entrance_mine, type = EntranceType.MINE))
    })
}