package com.cn.sample.design_model.delegate.pattern

import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

/**
 * @Author:         cn
 * @Date:           2025/4/11 14:34
 * @Description:    动态委派模式
 */
class DynamicRealConfCtrl: IConfCtrl {

    companion object {
        val TAG = DynamicRealConfCtrl::class.simpleName
    }
    override fun create(name: String?) {
        println("$TAG create by $name")
    }

    override fun join() {
        println("$TAG join:")
    }
}

class ConfCtrlHandler(private val any: Any): InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
//        println("${DynamicRealConfCtrl::class.simpleName} invoke: ${method?.name?:"NULL"}, ${args?.size}")
//        args?.forEach { println("arg:$it") }
        return if (null == args) method?.invoke(any) else method?.invoke(any, *args)
    }
}