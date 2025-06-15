package com.cn.mine.wan.android.repository

import android.content.Context
import com.cn.library.utils.network.config.NetworkConfig

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 23:33
 * @Description:
 */
object RepositoryInitializer {

    fun initializer(context: Context) {
        RepositoryContextExt.context = context
        NetworkConfig.BASE_URL = "https://www.wanandroid.com"
    }


}