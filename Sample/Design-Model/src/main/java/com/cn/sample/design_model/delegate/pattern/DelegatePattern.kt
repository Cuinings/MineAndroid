package com.cn.sample.design_model.delegate.pattern

import java.lang.reflect.Proxy

/**
 * @Author:         cn
 * @Date:           2025/4/11 14:42
 * @Description:
 */
fun main() {

    val confCtrl: IConfCtrl = ProxyConfCtrl()
    confCtrl.create("Static")
    confCtrl.join()

    val proxyConfCtrl = Proxy.newProxyInstance(
        IConfCtrl::class.java.classLoader,
        arrayOf(IConfCtrl::class.java),
        ConfCtrlHandler(DynamicRealConfCtrl())
    ) as IConfCtrl

    proxyConfCtrl.create("Dynamic")
    proxyConfCtrl.join()

}