package com.cn.mine.wan.android.repository.article.source.remote

import com.cn.mine.wan.android.data.wan.android.ArticleEntity
import com.cn.mine.wan.android.data.wan.android.CommonEntity
import com.cn.mine.wan.android.data.wan.android.CommonPageEntity
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * @Author: CuiNing
 * @Time: 2025/6/8 9:36
 * @Description:
 */
interface ArticleApiService {

    /**
     * 文章列表
     */
    @GET("/article/list/{pageNum}/json")
    suspend fun article(@Path("pageNum") pageNum: Int): Response<CommonEntity<CommonPageEntity<ArticleEntity>>>
}