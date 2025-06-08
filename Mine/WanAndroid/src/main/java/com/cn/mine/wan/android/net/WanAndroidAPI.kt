package com.cn.mine.wan.android.net

import com.cn.mine.wan.android.data.entity.CommonEntity
import com.cn.mine.wan.android.data.entity.UserEntity
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

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

}