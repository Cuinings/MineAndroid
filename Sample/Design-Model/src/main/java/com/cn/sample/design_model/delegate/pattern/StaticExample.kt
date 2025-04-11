package com.cn.sample.design_model.delegate.pattern

/**
 * @Author:         cn
 * @Date:           2025/4/11 14:34
 * @Description:    静态委派模式
 */

class RealConfCtrl: IConfCtrl {

    companion object {
        val TAG = RealConfCtrl::class.simpleName
    }
    override fun create(name: String?) {
        println("$TAG create:$name")
    }
}

class ProxyConfCtrl: IConfCtrl {

    var realConfCtrl: RealConfCtrl = RealConfCtrl()

    override fun create(name: String?) {
        realConfCtrl.create(name)
    }
}