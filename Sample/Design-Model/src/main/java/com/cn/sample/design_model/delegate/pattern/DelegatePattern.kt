package com.cn.sample.design_model.delegate.pattern

import java.lang.reflect.Proxy

/**
 * @Author:         cn
 * @Date:           2025/4/11 14:42
 * @Description:
 */
fun main() {
    println("1111")
    val confCtrl: IConfCtrl = ProxyConfCtrl()
    confCtrl.create("Static")

    val dynamicRealConfCtrl: IConfCtrl = DynamicRealConfCtrl()

    val proxyConfCtrl = Proxy.newProxyInstance(
        IConfCtrl::class.java.classLoader,
        arrayOf(IConfCtrl::class.java),
        ConfCtrlHandler(dynamicRealConfCtrl)
    ) as IConfCtrl
    proxyConfCtrl.create("Dynamic")

}