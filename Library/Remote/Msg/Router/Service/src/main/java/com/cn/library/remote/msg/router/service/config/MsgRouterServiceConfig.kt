package com.cn.library.remote.msg.router.service.config

import android.util.Log

/**
 * @Author: CuiNing
 * @Time: 2025/6/5 18:47
 * @Description:
 */
object MsgRouterServiceConfig {

    /**
     * 服务app包名
     */
    var SERVER_PACKAGE: String = ""
        set(value) { value.takeIf { it != field }?.let {
            field = it
            Log.d(MsgRouterServiceConfig::class.simpleName, "SERVER_PACKAGE:$it")
        } }
}