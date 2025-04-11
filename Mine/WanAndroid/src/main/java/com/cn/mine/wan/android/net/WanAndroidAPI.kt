package com.cn.mine.wan.android.net

import com.cn.library.data.wan.android.entity.ArticleEntity
import com.cn.library.data.wan.android.entity.BannerEntity
import com.cn.library.data.wan.android.entity.CommonData
import com.cn.library.data.wan.android.entity.CommonPageData
import com.cn.library.data.wan.android.entity.UserEntity
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WanAndroidAPI {

    /**
     * 登录
     */
    @POST("/user/login")
    @FormUrlEncoded
    suspend fun login(@Field("username") username: String, @Field("password") password: String): CommonData<UserEntity>

    /**
     * 登出
     */
    @GET("/user/logout/json")
    suspend fun logout(@Field("username") username: String, @Field("password") password: String): String

    /**
     * 注册
     */
    @POST("/user/register")
    @FormUrlEncoded
    suspend fun register(@Field("username") username: String, @Field("password") password: String, @Field("repassword")  repassword: String): CommonData<UserEntity>

    /**
     * 首页Banner
     */
    @GET("/banner/json")
    suspend fun banner(): CommonData<List<BannerEntity>>

    /**
     * 文章列表
     */
    @GET("/article/list/{pageNum}/json")
    suspend fun article(@Path("pageNum") pageNum: Int): CommonData<CommonPageData<ArticleEntity>>
}