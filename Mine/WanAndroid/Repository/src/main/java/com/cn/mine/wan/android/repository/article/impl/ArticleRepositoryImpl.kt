package com.cn.mine.wan.android.repository.article.impl

import android.accounts.NetworkErrorException
import com.cn.library.utils.network.isNetworkAvailable
import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.CommonEntity
import com.cn.mine.wan.android.data.entity.CommonPageEntity
import com.cn.mine.wan.android.repository.RepositoryContextExt.context
import com.cn.mine.wan.android.repository.article.ArticleRepository
import com.cn.mine.wan.android.repository.article.source.remote.ArticleDataSource

/**
 * @Author: CuiNing
 * @Time: 2025/6/8 9:35
 * @Description:
 */
class ArticleRepositoryImpl(
    private val articleDataSource: ArticleDataSource
): ArticleRepository {

    override suspend fun article(index: Int): Result<CommonEntity<CommonPageEntity<ArticleEntity>>> {
        return if (context.isNetworkAvailable()) articleDataSource.article(index) else Result.failure(NetworkErrorException("Network Error"))
    }

}