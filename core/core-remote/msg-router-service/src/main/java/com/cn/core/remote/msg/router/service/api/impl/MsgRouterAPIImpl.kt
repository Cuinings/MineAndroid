package com.cn.core.remote.msg.router.service.api.impl

import android.os.IBinder
import android.util.Log
import com.cn.core.remote.msg.router.client.api.MsgRouterAPI
import com.cn.core.remote.msg.router.client.api.MsgRouterCallback
import com.cn.core.remote.msg.router.client.bean.Msg
import com.cn.core.remote.msg.router.service.config.MsgRouterServiceConfig.SERVER_PACKAGE
import java.util.concurrent.ConcurrentHashMap

class MsgRouterAPIImpl : MsgRouterAPI {

    companion object {
        val TAG = MsgRouterAPI::class.simpleName
    }

    private val subscriberMsgMap: HashMap<String, MutableList<String>> = HashMap()
    private val callbacks: ConcurrentHashMap<String, CallbackWrapper> = ConcurrentHashMap()

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
                val wrapper = callbacks[process]
                if (wrapper != null) {
                    try {
                        wrapper.callback.dispatchMsg(msg)
                    } catch (e: Exception) {
                        Log.e(TAG, "$process dispatchMsg error: ${e.message}")
                        removeCallback(process)
                    }
                } else {
                    Log.e(TAG, "$process, unbind Callback")
                }
            } ?: Log.e(TAG, "$process no subscribe ${msg.content.code}")
        }
    }

    override fun registerCallback(
        callback: MsgRouterCallback,
        source: String
    ) {
        val wrapper = CallbackWrapper(callback, source)
        callbacks[source] = wrapper
        Log.d(TAG, "registerCallback: $source")
    }

    override fun unRegisterCallback(
        callback: MsgRouterCallback,
        source: String
    ) {
        removeCallback(source)
        Log.d(TAG, "unRegisterCallback: $source")
    }

    private fun removeCallback(source: String) {
        callbacks.remove(source)?.unlink()
    }

    private inner class CallbackWrapper(
        val callback: MsgRouterCallback,
        private val source: String
    ) : IBinder.DeathRecipient {

        private var linked: Boolean = false

        init {
            link()
        }

        private fun link() {
            try {
                val binder = getBinder(callback)
                if (binder != null) {
                    binder.linkToDeath(this, 0)
                    linked = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "linkToDeath failed: ${e.message}")
            }
        }

        fun unlink() {
            if (linked) {
                try {
                    val binder = getBinder(callback)
                    binder?.unlinkToDeath(this, 0)
                } catch (e: Exception) {
                    Log.e(TAG, "unlinkToDeath failed: ${e.message}")
                }
                linked = false
            }
        }

        override fun binderDied() {
            Log.d(TAG, "binderDied: $source")
            callbacks.remove(source)
            linked = false
        }

        private fun getBinder(callback: MsgRouterCallback): IBinder? {
            return try {
                val callbackClass = callback.javaClass
                val method = callbackClass.getDeclaredMethod("asBinder")
                method.invoke(callback) as? IBinder
            } catch (e: Exception) {
                Log.e(TAG, "getBinder failed: ${e.message}")
                null
            }
        }
    }
}
