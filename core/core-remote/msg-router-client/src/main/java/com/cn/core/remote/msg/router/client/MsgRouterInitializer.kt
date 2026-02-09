package com.cn.core.remote.msg.router.client

import android.content.Context
import androidx.startup.Initializer
import com.cn.core.remote.msg.router.client.BindResult.bindMsgRouterResult
import com.cn.core.remote.msg.router.client.MsgRouterProvider.bindMsgRouter

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 16:18
 * @Description:
 */
class MsgRouterInitializer: Initializer<Boolean> {
    override fun create(context: Context): Boolean {
        context.bindMsgRouter { bindMsgRouterResult = it }
        return true
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}