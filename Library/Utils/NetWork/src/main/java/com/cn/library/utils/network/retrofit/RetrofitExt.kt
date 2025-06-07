package com.cn.library.utils.network.retrofit

import com.cn.library.utils.network.config.NetworkConfig.BASE_URL
import com.cn.library.utils.network.okhttp.OkHttpExt.okHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.getValue

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 23:27
 * @Description:
 */
object RetrofitExt {
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}