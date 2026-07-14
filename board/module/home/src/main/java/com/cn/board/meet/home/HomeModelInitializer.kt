package com.cn.board.meet.home

import android.content.Context
import androidx.startup.Initializer

/**
 * 无侵入式初始化入口。
 *
 * 由 androidx.startup 的 InitializationProvider 在 Application.onCreate() 之前自动发现并调用，
 * 因此无需修改 ProxyApplication 或其 Manifest。
 *
 * create() 仅负责"点火"——通过 HomeModel.processIntent 在 viewModelScope 中异步触发
 * AppStreamAggregator.init()，create() 自身不执行耗时操作、不阻塞主线程，符合 App Startup 要求。
 */
class HomeModelInitializer : Initializer<Boolean> {

    override fun create(context: Context): Boolean {
        HomeModel.processIntent(HomeModelIntent.InitAppStream)
        return true
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}
