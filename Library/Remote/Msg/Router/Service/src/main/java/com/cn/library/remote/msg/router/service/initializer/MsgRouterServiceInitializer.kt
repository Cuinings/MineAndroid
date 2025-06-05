package com.cn.library.remote.msg.router.service.initializer

import android.content.Context
import android.content.Intent
import androidx.startup.Initializer
import com.cn.library.remote.msg.router.service.MsgRouterService
import com.cn.library.remote.msg.router.service.config.MsgRouterServiceConfig.SERVER_PACKAGE

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 17:16
 * @Description:
 */
class MsgRouterServiceInitializer: Initializer<Boolean> {

    override fun create(context: Context): Boolean {
        context.startService(Intent(context, MsgRouterService::class.java).apply { this.`package` = SERVER_PACKAGE })
        return true
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}