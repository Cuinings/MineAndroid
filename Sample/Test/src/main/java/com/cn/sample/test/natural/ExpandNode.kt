package com.cn.sample.test.natural

import com.cn.library.common.recyclerview.adapter.entity.node.BaseExpandNode
import com.cn.library.common.recyclerview.adapter.entity.node.BaseNode
import java.util.concurrent.CopyOnWriteArrayList

class ExpandNode(
    val name: String = "",                           //部门名称
    var level: Int = 0,                                 //层级
    var group: Boolean = false,                         //组
    var online: Boolean = false,                        //在线离线
    var selected: Boolean = false,                      //是否选中
    var child: CopyOnWriteArrayList<BaseNode>? = null
): BaseExpandNode() {
    override val childNode: MutableList<BaseNode>? get() = child
}
