package com.cn.library.commom.viewmodel

import java.util.UUID

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 9:21
 * @Description:状态
 */
interface UiState {
    val stateId: String get() = UUID.randomUUID().toString()
    val timestamp: Long get() = System.currentTimeMillis()
}