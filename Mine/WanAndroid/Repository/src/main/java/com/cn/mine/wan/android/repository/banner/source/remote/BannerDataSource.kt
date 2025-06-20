package com.cn.mine.wan.android.repository.banner.source.remote

import com.cn.library.utils.network.retrofit.RetrofitExt.retrofit
import com.cn.mine.wan.android.entity.BannerEntity
import com.cn.mine.wan.android.entity.CommonEntity

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 17:30
 * @Description:
 */
class BannerDataSource {

    private val bannerApiService: BannerApiService by lazy { retrofit.create(BannerApiService::class.java) }

    suspend fun banner(): Result<CommonEntity<List<BannerEntity>>> {
        val response = bannerApiService.banner()
        return if (response.isSuccessful)
            Result.success(response.body() as CommonEntity<List<BannerEntity>>)
        else
            Result.failure(Exception("Banner HTTP ERROR -> ${response.code()}"))
    }
}