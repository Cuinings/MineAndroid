package com.cn.mine.wan.android.repository

import android.annotation.SuppressLint
import android.content.Context
import com.cn.mine.wan.android.api.ApiRepository
import com.cn.mine.wan.android.api.impl.ApiRepositoryImpl
import com.cn.mine.wan.android.api.source.ApiSource

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 16:41
 * @Description:
 */
@SuppressLint("StaticFieldLeak")
object RepositoryExt {

    lateinit var context: Context

    val apiRepository: ApiRepository = ApiRepositoryImpl(ApiSource())

}