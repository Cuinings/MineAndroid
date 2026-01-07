package com.cn.core.ui.viewmodel

import java.util.UUID

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 9:21
 * @Description:
 */
interface UiIntent {
    val stateId: String get() = UUID.randomUUID().toString()
    val timestamp: Long get() = System.currentTimeMillis()
    val targetState: String? get() = null // 指定目标状态
}