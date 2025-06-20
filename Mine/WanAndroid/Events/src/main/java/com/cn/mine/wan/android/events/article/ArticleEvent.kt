package com.cn.mine.wan.android.events.article

import com.cn.mine.wan.android.entity.ArticleEntity
import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.entity.CommonPageEntity
import com.cn.mine.wan.android.events.EventParam
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.IEvent
import com.cn.mine.wan.android.events.article.ArticleEvent.ArticleParam
import com.cn.mine.wan.android.repository.article.ArticleRepository
import com.cn.mine.wan.android.repository.article.impl.ArticleRepositoryImpl
import com.cn.mine.wan.android.repository.article.source.remote.ArticleDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * @Author: CuiNing
 * @Time: 2025/6/8 9:26
 * @Description:
 */
class ArticleEvent: IEvent<ArticleParam, EventResult<CommonEntity<CommonPageEntity<ArticleEntity>>>> {

    data class ArticleParam(val index: Int): EventParam()

    private val articleRepository: ArticleRepository = ArticleRepositoryImpl(ArticleDataSource())

    override fun execute(
        param: ArticleParam?,
    ): Flow<EventResult<CommonEntity<CommonPageEntity<ArticleEntity>>>> = flow {
        val result = articleRepository.article(param?.index?:0)
        emit(result.takeIf { it.isSuccess }?.let { EventResult.Success(it.getOrNull()) }?: EventResult.Failed(result.exceptionOrNull()))
    }
}