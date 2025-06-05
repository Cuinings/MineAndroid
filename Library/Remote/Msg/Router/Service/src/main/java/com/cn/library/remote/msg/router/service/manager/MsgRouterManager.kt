package com.cn.library.remote.msg.router.service.manager

import android.content.Context
import com.cn.library.remote.RemoteManager
import com.cn.library.remote.msg.router.service.api.impl.MsgRouterAPIImpl

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 14:43
 * @Description:
 */
class MsgRouterManager(context: Context): RemoteManager(context) {

    override fun registerAnyList(): MutableList<Any> = mutableListOf<Any>().apply {
        add(MsgRouterAPIImpl().apply {

        })
    }
}