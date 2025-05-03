package com.cn.library.remote.msg.router.client

import android.content.Context
import android.util.Log
import com.cn.library.remote.Check.processName
import com.cn.library.remote.RemoteApiManager.getApiFromCache
import com.cn.library.remote.RemoteBindCallback
import com.cn.library.remote.RemoteBuilder
import com.cn.library.remote.msg.router.client.MsgRouter.ACTION_MSG
import com.cn.library.remote.msg.router.client.api.MsgRouterAPI
import com.cn.library.remote.msg.router.client.api.MsgRouterCallback
import com.cn.library.remote.msg.router.client.bean.Msg
import com.cn.library.remote.msg.router.client.bean.MsgBody
import com.cn.library.remote.msg.router.client.callback.MsgCallback

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 16:01
 * @Description:
 */
object MsgRouterProvider {

    private val TAG = MsgRouterProvider::class.java.simpleName

    internal var msgCallback: MsgCallback? = null

    private var router: MsgRouterAPI? = null

    internal fun Msg.dispatcher() = router?.dispatcherMsg(this)

    internal fun subscribeMsg(msgIds: MutableList<String>) = router?.subscribeMsg(msgIds, processName)

    internal fun MsgBody.dispatcher(target: String) = processName.takeIf { it.isNotBlank() }?.let {
        Msg(it, target, this).dispatcher()
    }

    internal var callback = object: MsgRouterCallback{
        override fun dispatchMsg(msg: Msg) {
            msgCallback?.onRcvMsg(msg, msg.source)
        }
    }

    internal var processName: String = ""
        set(value) { if (field != value) {
            field = value
            Log.d(MsgRouterProvider::class.simpleName, "processName:$field")
        } }

    fun Context.bindMsgRouter(result: (Boolean) -> Unit): Any = RemoteBuilder(this.apply {
        processName = this@bindMsgRouter.processName()
    })
        .svrPkg("com.cn.mine.wan.android")
        .action(ACTION_MSG)
        .createInstance(MsgRouterAPI::class.java)
        .registerCallback(callback)
        .bindCallback(object: RemoteBindCallback{
            override fun bindResult(boolean: Boolean) {
                boolean.takeIf { it }?.let {
                    router =  getApiFromCache(MsgRouterAPI::class.java)?.apply {
                        this.registerCallback(callback, processName)
                        subscribeMsg(msgCallback?.onSubScribe()?: mutableListOf(), processName)
                    }
                }?: {
                    router?.unSubscribeMsg(processName)
                    router?.unRegisterCallback(callback, processName)
                    router = null
                }
                result.invoke(boolean)
            }

        })
        .build()

}