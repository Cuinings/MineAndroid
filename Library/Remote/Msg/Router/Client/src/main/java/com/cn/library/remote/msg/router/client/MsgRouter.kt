package com.cn.library.remote.msg.router.client

import android.util.Log
import com.cn.library.remote.msg.router.client.MsgRouterProvider.dispatcher
import com.cn.library.remote.msg.router.client.annotation.Dispatcher
import com.cn.library.remote.msg.router.client.annotation.Subscribe
import com.cn.library.remote.msg.router.client.bean.Msg
import com.cn.library.remote.msg.router.client.bean.MsgBody
import com.cn.library.remote.msg.router.client.callback.MsgCallback
import com.cn.library.remote.msg.router.client.dispathcer.MsgRouterDispatcher
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 16:00
 * @Description:
 */
object MsgRouter {

    const val ACTION_MSG = "com.cn.library.remote.msg.router"

    fun MsgCallback.bind() { MsgRouterProvider.msgCallback = this }

    fun subscribe(msgIds: Array<String>) = router.subscribeMsg(msgIds,
        MsgRouterProvider.processName
    )

    fun dispatcherMsg(body: MsgBody) = router.dispatcher(body)

    private val router by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Proxy.newProxyInstance(
            MsgRouterDispatcher::class.java.classLoader,
            arrayOf<Class<*>>(MsgRouterDispatcher::class.java)
        ) { _: Any, method: Method, anyArray: Array<Any> -> dispatcher(method, anyArray) } as  MsgRouterDispatcher
    }

    private fun dispatcher(method: Method, anyArray: Array<Any>) {
        Log.d(TAG, "dispatcher: ${method.name}, ${anyArray.size}")
        method.annotations.forEach { annotation ->
            when(annotation) {
                is Dispatcher -> annotation.value.forEach { routerTarget ->
                    routerTarget.value.takeIf { it.isNotBlank() }?.let {
                        (anyArray[0] as MsgBody).dispatcher(it)
                    } ?: Msg(content = anyArray[0] as MsgBody).dispatcher()
                }
                is Subscribe -> MsgRouterProvider.subscribeMsg(anyArray[0] as MutableList<String>)
            }
        }
    }

    private val TAG = MsgRouter::class.simpleName

}