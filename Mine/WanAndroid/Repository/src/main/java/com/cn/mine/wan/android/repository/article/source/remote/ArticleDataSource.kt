package com.cn.mine.wan.android.repository.article.source.remote

import com.cn.library.utils.network.retrofit.RetrofitExt.retrofit
import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.CommonEntity
import com.cn.mine.wan.android.data.entity.CommonPageEntity

/**
 * @Author: CuiNing
 * @Time: 2025/6/8 9:37
 * @Description:
 */
class ArticleDataSource {

    private val articleApiService: ArticleApiService by lazy { retrofit.create(ArticleApiService::class.java) }

    suspend fun article(index: Int): Result<CommonEntity<CommonPageEntity<ArticleEntity>>> {
        val response = articleApiService.article(index)
        return if (response.isSuccessful)
            Result.success(response.body() as CommonEntity<CommonPageEntity<ArticleEntity>>)
        else
            Result.failure(Exception("Article HTTP ERROR -> ${response.code()}"))
    }

}