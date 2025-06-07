package com.cn.mine.wan.android.repository.banner.impl

import android.accounts.NetworkErrorException
import com.cn.library.utils.network.isConnected
import com.cn.mine.wan.android.data.entity.BannerEntity
import com.cn.mine.wan.android.data.entity.CommonEntity
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

    override suspend fun banner(action: (Result<CommonEntity<List<BannerEntity>>?>) -> Unit) {
        context.isConnected {
            action.invoke(
                if (it) {
                    bannerDataSource.banner()
                } else {
                    Result.failure(NetworkErrorException("Network Error"))
                }
            )
        }
    }

}