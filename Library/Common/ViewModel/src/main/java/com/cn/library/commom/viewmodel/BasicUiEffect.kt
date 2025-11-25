package com.cn.library.commom.viewmodel

import java.util.UUID

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 9:21
 * @Description:
 */
interface UiEffect {
    val stateId: String get() = UUID.randomUUID().toString()
    val timestamp: Long get() = System.currentTimeMillis()
    val sourceState: String? get() = null // 来源状态标识
}