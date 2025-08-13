package com.cn.mine.wan.android.api.source

import com.cn.library.utils.network.retrofit.RetrofitExt.retrofit
import com.cn.mine.wan.android.entity.ArticleEntity
import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.entity.CommonPageEntity
import com.cn.mine.wan.android.api.source.remote.ApiService
import com.cn.mine.wan.android.entity.BannerEntity
import com.cn.mine.wan.android.entity.UserEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/8/13 10:49
 * @Description:
 */
class ApiSource @Inject constructor() {

    private val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }

    suspend fun banner(): Result<CommonEntity<List<BannerEntity>>> {
        val response = apiService.banner()
        return if (response.isSuccessful)
            Result.success(response.body() as CommonEntity<List<BannerEntity>>)
        else
            Result.failure(Exception("Banner HTTP ERROR -> ${response.code()}"))
    }

    suspend fun article(index: Int): Result<CommonEntity<CommonPageEntity<ArticleEntity>>> {
        val response = apiService.article(index)
        return if (response.isSuccessful)
            Result.success(response.body() as CommonEntity<CommonPageEntity<ArticleEntity>>)
        else
            Result.failure(Exception("Article HTTP ERROR -> ${response.code()}"))
    }

    suspend fun login(username: String, password: String): Flow<Result<CommonEntity<UserEntity>>> = callbackFlow {
        apiService.login(username, password).takeIf {
            it.isSuccessful
        }?.let {
            trySendBlocking(Result.success<CommonEntity<UserEntity>>(it.body() as CommonEntity<UserEntity>))
        }?: trySendBlocking(Result.failure(Exception("login failed -> $")))
        awaitClose()
    }

    suspend fun register(username: String, password: String, rePassword: String): Flow<Result<String>> = callbackFlow {
        apiService.register(username, password, rePassword).takeIf {
            it.isSuccessful
        }?.let {
            trySendBlocking(Result.success(it.body().toString()))
        }?: trySendBlocking(Result.failure(Exception("login failed -> $")))
        awaitClose()
    }
}