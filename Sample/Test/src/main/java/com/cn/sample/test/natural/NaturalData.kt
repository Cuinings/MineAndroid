package com.cn.sample.test.natural

import com.cn.library.common.recyclerview.adapter.entity.node.BaseNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.CopyOnWriteArrayList

object NaturalData {

    var navData: MutableList<NaturalNode> = arrayListOf<NaturalNode>().apply {
        add(NaturalNode(achName = "全部列表"))
        add(NaturalNode(achName = "收藏列表"))
        add(NaturalNode(achName = "会议点事终端"))
        add(NaturalNode(achName = "布控球"))
        add(NaturalNode(achName = "执法仪"))
        add(NaturalNode(achName = "IPC"))
        add(NaturalNode(achName = "对讲机"))
        add(NaturalNode(achName = "未知设备"))
    }

    /**
     * 所有资源
     */
    var naturalCommonData: NaturalNode = NaturalNode(achName = "所有资源").apply {
        child = CopyOnWriteArrayList<BaseNode>().apply {
            add(NaturalNode(achName = "1 group").apply {
                isExpanded = true
                group = true
                level = 0
                child = CopyOnWriteArrayList<BaseNode>().apply {
                    add(NaturalNode(achName = "1-1").apply {
                        online = true
                        level = 1
                    })
                    add(NaturalNode(achName = "1-2").apply {
                        level = 1
                    })
                    add(NaturalNode(achName = "1-3").apply {
                        group = true
                        level = 1
                        child = CopyOnWriteArrayList<BaseNode>().apply {
                            add(NaturalNode(achName = "1-3-1").apply {
                                online = true
                                level = 2
                            })
                            add(NaturalNode(achName = "1-3-2").apply {
                                online = false
                                level = 2
                            })
                            add(NaturalNode(achName = "1-3-2").apply {
                                online = true
                                level = 2
                            })
                        }
                    })
                }
            })
            add(NaturalNode(achName = "2").apply {
                online = true
                level = 0
            })
            add(NaturalNode(achName = "3").apply {
                online = false
                level = 0
            })
            add(NaturalNode(achName = "4").apply {
                online = true
                level = 0
            })
            add(NaturalNode(achName = "55555555555555555555555555555555555555555555555555555555555555555555555555555555555").apply {
                online = false
                level = 0
            })
            add(NaturalNode(achName = "666666666666666666666666666666666666666666666666666666666666666666666666666666666666666").apply {
                isExpanded = false
                group = true
                level = 0
                child = CopyOnWriteArrayList<BaseNode>().apply {
                    add(NaturalNode(achName = "6-1").apply {
                        online = true
                        level = 1
                    })
                    add(NaturalNode(achName = "6-2").apply {
                        level = 1
                    })
                    add(NaturalNode(achName = "6-3").apply {
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
    private var currentNaturalData: NaturalNode? = null


    suspend fun naturalTestData(action: suspend (NaturalNode?) -> Unit) = flow {
        currentNaturalData = naturalCommonData
        delay(1000)
        emit(currentNaturalData)
    }.flowOn(Dispatchers.IO).collect{ action.invoke(it) }

    /**
     * 过滤后的资源
     */
    private var naturalFilterData: NaturalNode? = null
}