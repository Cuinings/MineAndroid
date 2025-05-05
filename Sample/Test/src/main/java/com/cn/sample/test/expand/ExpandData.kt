package com.cn.sample.test.expand

import com.cn.library.common.recyclerview.adapter.entity.node.BaseNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.CopyOnWriteArrayList

object ExpandData {

    var navData: MutableList<ExpandNode> = arrayListOf<ExpandNode>().apply {
        add(ExpandNode(name = "全部列表"))
        add(ExpandNode(name = "收藏列表"))
        add(ExpandNode(name = "会议点事终端"))
        add(ExpandNode(name = "布控球"))
        add(ExpandNode(name = "执法仪"))
        add(ExpandNode(name = "IPC"))
        add(ExpandNode(name = "对讲机"))
        add(ExpandNode(name = "未知设备"))
    }

    /**
     * 所有资源
     */
    var naturalCommonData: ExpandNode = ExpandNode(name = "所有资源").apply {
        child = CopyOnWriteArrayList<BaseNode>().apply {
            add(ExpandNode(name = "1 group").apply {
                isExpanded = true
                group = true
                level = 0
                child = CopyOnWriteArrayList<BaseNode>().apply {
                    add(ExpandNode(name = "1-1").apply {
                        online = true
                        level = 1
                    })
                    add(ExpandNode(name = "1-2").apply {
                        level = 1
                    })
                    add(ExpandNode(name = "1-3").apply {
                        group = true
                        level = 1
                        child = CopyOnWriteArrayList<BaseNode>().apply {
                            add(ExpandNode(name = "1-3-1").apply {
                                online = true
                                level = 2
                            })
                            add(ExpandNode(name = "1-3-2").apply {
                                online = false
                                level = 2
                            })
                            add(ExpandNode(name = "1-3-2").apply {
                                online = true
                                level = 2
                            })
                        }
                    })
                }
            })
            add(ExpandNode(name = "2").apply {
                online = true
                level = 0
            })
            add(ExpandNode(name = "3").apply {
                online = false
                level = 0
            })
            add(ExpandNode(name = "4").apply {
                online = true
                level = 0
            })
            add(ExpandNode(name = "55555555555555555555555555555555555555555555555555555555555555555555555555555555555").apply {
                online = false
                level = 0
            })
            add(ExpandNode(name = "666666666666666666666666666666666666666666666666666666666666666666666666666666666666666").apply {
                isExpanded = false
                group = true
                level = 0
                child = CopyOnWriteArrayList<BaseNode>().apply {
                    add(ExpandNode(name = "6-1").apply {
                        online = true
                        level = 1
                    })
                    add(ExpandNode(name = "6-2").apply {
                        level = 1
                    })
                    add(ExpandNode(name = "6-3").apply {
                        online = true
                        level = 1
                    })
                }
            })
        }
    }

    /**
     * 列表展示资源
     */
    private var currentNaturalData: ExpandNode? = null


    suspend fun naturalTestData(action: suspend (ExpandNode?) -> Unit) = flow {
        currentNaturalData = naturalCommonData
        delay(1000)
        emit(currentNaturalData)
    }.flowOn(Dispatchers.IO).collect{ action.invoke(it) }

    /**
     * 过滤后的资源
     */
    private var naturalFilterData: ExpandNode? = null
}