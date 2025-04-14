package com.cn.library.common.recyclerview.adapter.module

import com.cn.library.common.recyclerview.adapter.BaseQuickAdapter
import com.cn.library.common.recyclerview.adapter.listener.OnUpFetchListener
import com.cn.library.common.recyclerview.adapter.listener.UpFetchListenerImp

/**
 * 需要【向上加载更多】功能的，[BaseQuickAdapter]继承此接口
 */
interface UpFetchModule {
    /**
     * 重写此方法，返回自定义模块
     * @param baseQuickAdapter BaseQuickAdapter<*, *>
     * @return BaseUpFetchModule
     */
    fun addUpFetchModule(baseQuickAdapter: BaseQuickAdapter<*, *>): BaseUpFetchModule {
        return BaseUpFetchModule(baseQuickAdapter)
    }
}

open class BaseUpFetchModule(private val baseQuickAdapter: BaseQuickAdapter<*, *>) :
    UpFetchListenerImp {

    private var mUpFetchListener: OnUpFetchListener? = null

    var isUpFetchEnable = false
    var isUpFetching = false
    /**
     * start up fetch position, default is 1.
     */
    var startUpFetchPosition = 1

    internal fun autoUpFetch(position: Int) {
        if (!isUpFetchEnable || isUpFetching) {
            return
        }
        if (position <= startUpFetchPosition) {
            mUpFetchListener?.onUpFetch()
        }
    }

    override fun setOnUpFetchListener(listener: OnUpFetchListener?) {
        this.mUpFetchListener = listener
    }
}
