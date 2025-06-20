package com.cn.mine.wan.android.repository.banner.impl

import android.accounts.NetworkErrorException
import com.cn.library.utils.network.isNetworkAvailable
import com.cn.mine.wan.android.entity.BannerEntity
import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.repository.RepositoryContextExt.context
import com.cn.mine.wan.android.repository.banner.BannerRepository
import com.cn.mine.wan.android.repository.banner.source.remote.BannerDataSource

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 16:31
 * @Description:
 */
class BannerRepositoryImpl(
    private val bannerDataSource: BannerDataSource
): BannerRepository {

    override suspend fun banner(): Result<CommonEntity<List<BannerEntity>>?> {
        return if (context.isNetworkAvailable()) bannerDataSource.banner() else Result.failure(NetworkErrorException("Network Error"))
    }

}