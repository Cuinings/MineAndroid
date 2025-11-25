package com.cn.library.common.recyclerview.adapter.provider

import com.cn.library.common.recyclerview.adapter.BaseNodeAdapter
import com.cn.library.common.recyclerview.adapter.entity.node.BaseNode

abstract class BaseNodeProvider : BaseItemProvider<BaseNode>() {

    override fun getAdapter(): BaseNodeAdapter? {
        return super.getAdapter() as? BaseNodeAdapter
    }

}