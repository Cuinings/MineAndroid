package com.cn.core.ui.view.recyclerview.adapter.provider

import com.cn.core.ui.view.recyclerview.adapter.BaseNodeAdapter
import com.cn.core.ui.view.recyclerview.adapter.entity.node.BaseNode

abstract class BaseNodeProvider : BaseItemProvider<BaseNode>() {

    override fun getAdapter(): BaseNodeAdapter? {
        return super.getAdapter() as? BaseNodeAdapter
    }

}