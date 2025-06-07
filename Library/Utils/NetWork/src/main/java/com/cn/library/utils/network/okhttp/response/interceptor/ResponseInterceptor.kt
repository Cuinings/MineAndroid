package com.cn.library.utils.network.okhttp.response.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody

class ResponseInterceptor: Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val body = response.body
        val contentType = body?.contentType()
        val bodyString = body?.string()
        Log.d(ResponseInterceptor::class.simpleName, "\n$request\n$bodyString\n")
        return response.newBuilder().body(bodyString?.let { ResponseBody.create(contentType, it) }).build()
    }

}