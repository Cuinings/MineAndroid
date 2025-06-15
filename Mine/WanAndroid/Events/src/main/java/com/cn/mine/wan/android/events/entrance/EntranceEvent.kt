package com.cn.mine.wan.android.events.entrance

import com.cn.mine.wan.android.entity.EntranceEntity
import com.cn.mine.wan.android.events.EventParam
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.IEvent
import com.cn.mine.wan.android.repository.entrance.EntranceRepository
import com.cn.mine.wan.android.repository.entrance.impl.EntranceRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * @Author: CuiNing
 * @Time: 2025/6/14 10:56
 * @Description:
 */
class EntranceEvent: IEvent<EntranceEvent.EntranceParam, EventResult<MutableList<EntranceEntity>>> {

    data class EntranceParam(val value: Int): EventParam()

    private val entranceRepository: EntranceRepository = EntranceRepositoryImpl()

    override fun execute(param: EntranceParam?): Flow<EventResult<MutableList<EntranceEntity>>> =
        flow {
            val result = entranceRepository.entrance()
            emit(EventResult.Success(result.getOrNull()))
        }
}