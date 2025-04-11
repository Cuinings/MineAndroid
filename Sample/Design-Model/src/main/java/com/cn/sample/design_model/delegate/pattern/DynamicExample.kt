package com.cn.sample.design_model.delegate.pattern

import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

/**
 * @Author:         cn
 * @Date:           2025/4/11 14:34
 * @Description:    静态委派模式
 */
class DynamicRealConfCtrl: IConfCtrl {

    companion object {
        val TAG = DynamicRealConfCtrl::class.simpleName
    }
    override fun create(name: String?) {
        println("$TAG create:$name")
    }
}

class ConfCtrlHandler(private val iConfCtrl: Any): InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        println("${DynamicRealConfCtrl::class.simpleName} invoke: ${method?.name?:"NULL"}, ${args?.size}")
        args?.forEach {
            println("arg:$it")
        }
        return method?.invoke(iConfCtrl, args?.get(0) ?: "")
    }
}