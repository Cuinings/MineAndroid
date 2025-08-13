package com.cn.mine.wan.android.events.banner

import com.cn.mine.wan.android.entity.BannerEntity
import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.IEvent
import com.cn.mine.wan.android.repository.RepositoryExt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 20:11
 * @Description:
 */
class BannerEvent @Inject constructor(): IEvent<EventResult<CommonEntity<List<BannerEntity>>>> {

    override fun execute(): Flow<EventResult<CommonEntity<List<BannerEntity>>>> = flow {
        val result =  RepositoryExt.apiRepository.banner()
        emit(result.takeIf { it.isSuccess }?.let { EventResult.Success(it.getOrNull()) }?: EventResult.Failed(result.exceptionOrNull()))
    }
}