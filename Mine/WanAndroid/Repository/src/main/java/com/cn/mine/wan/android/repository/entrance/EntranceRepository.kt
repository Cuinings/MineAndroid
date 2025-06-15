package com.cn.mine.wan.android.repository.entrance

import com.cn.mine.wan.android.entity.EntranceEntity
import com.cn.mine.wan.android.repository.IRepository

/**
 * @Author: CuiNing
 * @Time: 2025/6/15 19:27
 * @Description:
 */
interface EntranceRepository: IRepository {

    suspend fun entrance(): Result<MutableList<EntranceEntity>>

}