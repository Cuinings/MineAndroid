package com.cn.mine.wan.android.events.banner

import com.cn.mine.wan.android.data.entity.BannerEntity
import com.cn.mine.wan.android.data.entity.CommonEntity
import com.cn.mine.wan.android.events.EventParam
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.IEvent
import com.cn.mine.wan.android.events.banner.BannerEvent.BannerParam
import com.cn.mine.wan.android.repository.banner.BannerRepository
import com.cn.mine.wan.android.repository.banner.impl.BannerRepositoryImpl
import com.cn.mine.wan.android.repository.banner.source.remote.BannerDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 20:11
 * @Description:
 */
class BannerEvent: IEvent<BannerParam, EventResult<CommonEntity<List<BannerEntity>>>> {

    data class BannerParam(val value: String): EventParam()

    private val bannerRepository: BannerRepository = BannerRepositoryImpl(BannerDataSource())
    override fun execute(
        param: BannerParam?,
    ): Flow<EventResult<CommonEntity<List<BannerEntity>>>> = flow {
        val result =  bannerRepository.banner()
        emit(result.takeIf { it.isSuccess }?.let { EventResult.Success(it.getOrNull()) }?: EventResult.Failed(result.exceptionOrNull()))
    }
}