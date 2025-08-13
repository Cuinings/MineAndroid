package com.cn.mine.wan.android.events.entrance

import android.util.Log
import com.cn.mine.wan.android.entity.EntranceEntity
import com.cn.mine.wan.android.events.EventParam
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.IEvent
import com.cn.mine.wan.android.events.IEventWithParam
import com.cn.mine.wan.android.repository.entrance.EntranceRepository
import com.cn.mine.wan.android.repository.entrance.impl.EntranceRepositoryImpl
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/6/14 10:56
 * @Description:
 */
class EntranceEvent @Inject constructor(): IEvent<EventResult<MutableList<EntranceEntity>>> {

    private val entranceRepository: EntranceRepository = EntranceRepositoryImpl()

    override fun execute(): Flow<EventResult<MutableList<EntranceEntity>>> =
        callbackFlow {
            val result = entranceRepository.entrance().apply {
                Log.i("EntranceEvent", "execute: $this")
            }
            trySendBlocking(EventResult.Success(result.getOrNull()))
            awaitClose()
        }
}