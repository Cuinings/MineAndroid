package com.cn.sample.test.child.basic

import com.cn.library.commom.viewmodel.UiState

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 9:58
 * @Description:
 */
data class AppDataState(
    val userData: Map<String, Any> = emptyMap(),
    val lastSyncTime: Long = 0,
    val isSyncing: Boolean = false,
    val syncError: String? = null
): UiState {
    override val stateId: String = "app_data_${super.stateId}"
}
