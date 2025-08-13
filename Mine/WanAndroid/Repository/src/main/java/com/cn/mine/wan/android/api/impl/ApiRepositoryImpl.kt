package com.cn.mine.wan.android.api.impl

import android.accounts.NetworkErrorException
import com.cn.library.utils.network.isNetworkAvailable
import com.cn.mine.wan.android.api.ApiRepository
import com.cn.mine.wan.android.api.source.ApiSource
import com.cn.mine.wan.android.entity.ArticleEntity
import com.cn.mine.wan.android.entity.BannerEntity
import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.entity.CommonPageEntity
import com.cn.mine.wan.android.entity.UserEntity
import com.cn.mine.wan.android.repository.RepositoryExt
import com.cn.mine.wan.android.repository.RepositoryExt.context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * @Author: CuiNing
 * @Time: 2025/6/8 9:35
 * @Description:
 */
class ApiRepositoryImpl(val source: ApiSource) : ApiRepository {

    override suspend fun banner(): Result<CommonEntity<List<BannerEntity>>> {
        return if (context.isNetworkAvailable()) source.banner() else Result.failure(NetworkErrorException("Network Error"))
    }

    override suspend fun login(
        username: String,
        password: String
    ): Flow<Result<CommonEntity<UserEntity>>> = callbackFlow {
        if (context.isNetworkAvailable()) {
            source.login(username, password).collect { trySendBlocking(it) }
        } else trySendBlocking(Result.failure(NetworkErrorException("Network Error")))
        awaitClose()
    }

    override suspend fun register(
        username: String,
        password: String,
        rePassword: String
    ): Flow<Result<String>> = callbackFlow {
        if (RepositoryExt.context.isNetworkAvailable()) {
            source.register(username, password, rePassword).collect { trySendBlocking(it) }
        } else trySendBlocking(Result.failure(NetworkErrorException("Network Error")))
        awaitClose()
    }

    override suspend fun article(
        index: Int
    ): Flow<Result<CommonEntity<CommonPageEntity<ArticleEntity>>>> = callbackFlow {
        trySendBlocking(
            (if (RepositoryExt.context.isNetworkAvailable()) source.article(index) else Result.failure(
                NetworkErrorException("Network Error")
            ))
        )
        awaitClose()
    }

}