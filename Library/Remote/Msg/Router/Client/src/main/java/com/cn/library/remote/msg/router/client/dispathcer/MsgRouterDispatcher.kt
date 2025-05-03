package com.cn.library.remote.msg.router.client.dispathcer

import com.cn.library.remote.msg.router.client.annotation.Dispatcher
import com.cn.library.remote.msg.router.client.annotation.Subscribe
import com.cn.library.remote.msg.router.client.bean.MsgBody

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 15:59
 * @Description:
 */
interface MsgRouterDispatcher {

    @Dispatcher
    fun dispatcher(body: MsgBody)

    @Subscribe
    fun subscribeMsg(msgIds: Array<String>, pkg: String)

}