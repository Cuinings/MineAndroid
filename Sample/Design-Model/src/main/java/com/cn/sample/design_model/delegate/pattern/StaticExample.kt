package com.cn.sample.design_model.delegate.pattern

/**
 * @Author:         cn
 * @Date:           2025/4/11 14:34
 * @Description:    静态委派模式
 */

class StaticRealConfCtrl: IConfCtrl {

    companion object {
        val TAG = StaticRealConfCtrl::class.simpleName
    }
    override fun create(name: String?) {
        println("$TAG create by $name")
    }

    override fun join() {
        println("$TAG join")
    }
}

class ProxyConfCtrl: IConfCtrl {

    private var realConfCtrl: StaticRealConfCtrl = StaticRealConfCtrl()

    override fun create(name: String?) {
        realConfCtrl.create(name)
    }

    override fun join() {
        realConfCtrl.join()
    }
}