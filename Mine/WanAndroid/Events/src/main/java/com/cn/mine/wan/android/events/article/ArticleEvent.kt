package com.cn.mine.wan.android.events.article

import com.cn.mine.wan.android.api.ApiRepository
import com.cn.mine.wan.android.entity.ArticleEntity
import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.entity.CommonPageEntity
import com.cn.mine.wan.android.events.EventParam
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.IEventWithParam
import com.cn.mine.wan.android.events.article.ArticleEvent.ArticleParam
import com.cn.mine.wan.android.repository.RepositoryExt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/6/8 9:26
 * @Description:
 */
class ArticleEvent @Inject constructor(): IEventWithParam<ArticleParam, EventResult<CommonEntity<CommonPageEntity<ArticleEntity>>>> {

    data class ArticleParam(val index: Int): EventParam()

    private val articleRepository: ApiRepository = RepositoryExt.apiRepository

    override fun execute(
        param: ArticleParam,
    ): Flow<EventResult<CommonEntity<CommonPageEntity<ArticleEntity>>>> = callbackFlow {
        articleRepository.article(param?.index?:0).collect {
            trySendBlocking(it.takeIf { it.isSuccess }?.let { EventResult.Success(it.getOrNull()) } ?: EventResult.Failed(it.exceptionOrNull()))
        }
        awaitClose()
    }
}