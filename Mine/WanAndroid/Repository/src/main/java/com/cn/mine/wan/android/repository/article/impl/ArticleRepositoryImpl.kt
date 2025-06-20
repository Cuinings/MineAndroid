package com.cn.mine.wan.android.repository.article.impl

import android.accounts.NetworkErrorException
import com.cn.library.utils.network.isNetworkAvailable
import com.cn.mine.wan.android.entity.ArticleEntity
import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.entity.CommonPageEntity
import com.cn.mine.wan.android.repository.RepositoryContextExt.context
import com.cn.mine.wan.android.repository.article.ArticleRepository
import com.cn.mine.wan.android.repository.article.source.remote.ArticleDataSource
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/6/8 9:35
 * @Description:
 */
class ArticleRepositoryImpl @Inject constructor(@Inject val articleDataSource: ArticleDataSource): ArticleRepository {

    override suspend fun article(index: Int): Result<CommonEntity<CommonPageEntity<ArticleEntity>>> {
        return if (context.isNetworkAvailable()) articleDataSource.article(index) else Result.failure(NetworkErrorException("Network Error"))
    }

}