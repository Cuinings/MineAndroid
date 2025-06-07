package com.cn.library.utils.network.okhttp

import com.cn.library.utils.network.okhttp.response.interceptor.ResponseInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 23:25
 * @Description:
 */
object OkHttpExt {

    val okHttpClient by lazy { OkHttpClient.Builder()
        .addInterceptor(ResponseInterceptor())
        .addInterceptor(HttpLoggingInterceptor())
//        .cookieJar(cookie)
        .build() }
}