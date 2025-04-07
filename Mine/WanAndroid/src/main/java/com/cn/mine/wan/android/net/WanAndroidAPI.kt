package com.cn.mine.wan.android.net

import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.BannerEntity
import com.cn.mine.wan.android.data.entity.CommonData
import com.cn.mine.wan.android.data.entity.CommonPageData
import retrofit2.http.GET
import retrofit2.http.Path

interface WanAndroidAPI {

    /**
     * 首页Banner
     */
    @GET("banner/json")
    suspend fun banner(): CommonData<List<BannerEntity>>

    /**
     * 文章列表
     */
    @GET("article/list/{pageNum}/json")
    suspend fun article(@Path("pageNum") pageNum: Int): CommonData<CommonPageData<ArticleEntity>>
}