package com.cn.library.common.recyclerview.adapter

import com.cn.library.common.recyclerview.adapter.module.BaseDraggableModule
import com.cn.library.common.recyclerview.adapter.module.DraggableModule

open class BaseDragBinderAdapter: BaseBinderAdapter(), DraggableModule {
    override fun addDraggableModule(baseQuickAdapter: BaseQuickAdapter<*, *>): BaseDraggableModule {
        return super.addDraggableModule(baseQuickAdapter)
    }
}