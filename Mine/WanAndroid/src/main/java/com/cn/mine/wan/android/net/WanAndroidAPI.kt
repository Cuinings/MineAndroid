package com.cn.mine.wan.android.net

import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.BannerEntity
import com.cn.mine.wan.android.data.entity.CommonEntity
import com.cn.mine.wan.android.data.entity.CommonPageEntity
import com.cn.mine.wan.android.data.entity.UserEntity
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
    suspend fun login(@Field("username") username: String, @Field("password") password: String): CommonEntity<UserEntity>

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
    suspend fun register(@Field("username") username: String, @Field("password") password: String, @Field("repassword")  repassword: String): CommonEntity<UserEntity>



    /**
     * 文章列表
     */
    @GET("/article/list/{pageNum}/json")
    suspend fun article(@Path("pageNum") pageNum: Int): CommonEntity<CommonPageEntity<ArticleEntity>>
}