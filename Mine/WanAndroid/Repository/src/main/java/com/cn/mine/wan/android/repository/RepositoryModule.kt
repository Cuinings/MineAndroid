package com.cn.mine.wan.android.repository

import android.content.Context
import android.util.Log
import com.cn.library.utils.network.config.NetworkConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 23:33
 * @Description:
 */
class RepositoryModule @Inject constructor() {
    fun initializer(context: Context) {
        RepositoryExt.context = context
        NetworkConfig.BASE_URL = "https://www.wanandroid.com"
        NetworkConfig.context = context
        Log.d("RepositoryModule", "initializer: ")
    }
}