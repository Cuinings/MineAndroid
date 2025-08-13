package com.cn.mine.wan.android.api.source.remote

import com.cn.mine.wan.android.entity.ArticleEntity
import com.cn.mine.wan.android.entity.BannerEntity
import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.entity.CommonPageEntity
import com.cn.mine.wan.android.entity.UserEntity
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * @Author: CuiNing
 * @Time: 2025/8/13 10:49
 * @Description:
 */
interface ApiService {

    /**
     * 文章列表
     */
    @GET("/article/list/{pageNum}/json")
    suspend fun article(@Path("pageNum") pageNum: Int): Response<CommonEntity<CommonPageEntity<ArticleEntity>>>

    /**
     * 首页Banner
     */
    @GET("/banner/json")
    suspend fun banner(): Response<CommonEntity<List<BannerEntity>>>

    /**
     * 登录
     */
    @FormUrlEncoded
    @POST("/user/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<CommonEntity<UserEntity>>

    /**
     * 注册
     */
    @FormUrlEncoded
    @POST("/user/register")
    suspend fun register(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("repassword") rePassword: String
    ): Response<String>
}