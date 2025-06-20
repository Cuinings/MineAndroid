package com.cn.mine.wan.android.repository.banner.source.remote

import com.cn.mine.wan.android.entity.BannerEntity
import com.cn.mine.wan.android.entity.CommonEntity
import retrofit2.Response
import retrofit2.http.GET

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 17:28
 * @Description:
 */
interface BannerApiService {

    /**
     * 首页Banner
     */
    @GET("/banner/json")
    suspend fun banner(): Response<CommonEntity<List<BannerEntity>>>
}