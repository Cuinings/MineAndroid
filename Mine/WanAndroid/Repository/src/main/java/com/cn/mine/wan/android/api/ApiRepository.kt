package com.cn.mine.wan.android.api

import com.cn.mine.wan.android.entity.ArticleEntity
import com.cn.mine.wan.android.entity.BannerEntity
import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.entity.CommonPageEntity
import com.cn.mine.wan.android.entity.UserEntity
import com.cn.mine.wan.android.repository.IRepository
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import retrofit2.http.GET


/**
 * @Author: CuiNing
 * @Time: 2025/6/7 16:31
 * @Description:
 */
interface ApiRepository: IRepository {

    suspend fun article(index: Int): Flow<Result<CommonEntity<CommonPageEntity<ArticleEntity>>>>

    suspend fun banner(): Result<CommonEntity<List<BannerEntity>>>

    suspend fun login(username: String, password: String): Flow<Result<CommonEntity<UserEntity>>>

    suspend fun register(username: String, password: String, rePassword: String): Flow<Result<String>>



}