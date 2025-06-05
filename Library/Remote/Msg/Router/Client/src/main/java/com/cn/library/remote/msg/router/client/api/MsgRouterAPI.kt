package com.cn.library.remote.msg.router.client.api

import com.cn.library.remote.andlinker.annotation.Callback
import com.cn.library.remote.andlinker.annotation.Inout
import com.cn.library.remote.andlinker.annotation.RemoteInterface
import com.cn.library.remote.msg.router.client.bean.Msg

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 15:05
 * @Description:
 */
@RemoteInterface
interface MsgRouterAPI {

    fun initServerApp(pkg: String)

    /**
     * 订阅消息
     */
    fun subscribeMsg(@Inout msgIds: MutableList<String>, process: String)

    /**
     * 取消订阅
     */
    fun unSubscribeMsg(process: String)

    /**
     * 消息分发
     */
    fun dispatcherMsg(@Inout msg: Msg)

    /**
     * 注册回调
     * @param callback
     * @param source
     */
    fun registerCallback(@Callback callback: MsgRouterCallback, source: String)

    /**
     * 反注册回调
     * @param callback
     * @param source
     */
    fun unRegisterCallback(@Callback callback: MsgRouterCallback, source: String)
}