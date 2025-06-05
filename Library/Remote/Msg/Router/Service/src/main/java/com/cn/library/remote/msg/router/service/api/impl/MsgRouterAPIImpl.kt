package com.cn.library.remote.msg.router.service.api.impl

import android.util.Log
import com.cn.library.remote.msg.router.client.api.MsgRouterAPI
import com.cn.library.remote.msg.router.client.api.MsgRouterCallback
import com.cn.library.remote.msg.router.client.bean.Msg
import com.cn.library.remote.msg.router.service.config.MsgRouterServiceConfig.SERVER_PACKAGE

class MsgRouterAPIImpl: MsgRouterAPI {

    companion object {
        val TAG = MsgRouterAPI::class.simpleName
    }

    private val subscriberMsgMap: HashMap<String, MutableList<String>> = HashMap()
    private val callbacks: HashMap<String, MsgRouterCallback> = HashMap()

    override fun initServerApp(pkg: String) {
        if (SERVER_PACKAGE.isBlank()) SERVER_PACKAGE = pkg
    }

    override fun subscribeMsg(
        msgIds: MutableList<String>,
        process: String
    ) {
        unSubscribeMsg(process)
        synchronized(subscriberMsgMap) {
            subscriberMsgMap[process] = msgIds.onEach {
                Log.i(TAG, "subscribeMsg:$process -> $it")
            }
        }
    }

    override fun unSubscribeMsg(process: String) {
        synchronized(subscriberMsgMap) {
            subscriberMsgMap[process]?.apply { Log.i(TAG, "unSubscribeMsg: $process") }?.clear()
        }
    }

    override fun dispatcherMsg(msg: Msg) {
        subscriberMsgMap.forEach { (process, subcribes) ->
            subcribes.takeIf { it.contains(msg.content.code) }?.let {
                callbacks[process]?.dispatchMsg(msg)?.apply {
                    Log.d(TAG, "dispatcherMsg: $process, ${msg.content.code} dispatcher success")
                }?:Log.e(TAG, "$process, unbind Callback")
            }?:Log.e(TAG, "$process no subscribe ${msg.content.code}")
        }
    }

    override fun registerCallback(
        callback: MsgRouterCallback,
        source: String
    ) {
        callbacks[source] = callback
        Log.d(TAG, "registerCallback: $source")
    }

    override fun unRegisterCallback(
        callback: MsgRouterCallback,
        source: String
    ) {
        callbacks.takeIf { it.containsKey(source) }?.remove(source)
        Log.d(TAG, "unRegisterCallback: $source")
    }
}