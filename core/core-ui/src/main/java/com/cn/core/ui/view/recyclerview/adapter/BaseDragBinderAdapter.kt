package com.cn.core.ui.view.recyclerview.adapter

import com.cn.core.ui.view.recyclerview.adapter.module.BaseDraggableModule
import com.cn.core.ui.view.recyclerview.adapter.module.DraggableModule

open class BaseDragBinderAdapter: BaseBinderAdapter(), DraggableModule {
    override fun addDraggableModule(baseQuickAdapter: BaseQuickAdapter<*, *>): BaseDraggableModule {
        return super.addDraggableModule(baseQuickAdapter)
    }
}