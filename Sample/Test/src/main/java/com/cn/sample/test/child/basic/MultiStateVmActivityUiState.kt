package com.cn.sample.test.child.basic

import com.cn.library.commom.viewmodel.UiState

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 10:00
 * @Description:
 */
data class MultiStateVmActivityUiState(
    val currentScreen: String = "home",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigationStack: List<String> = emptyList()
) : UiState {
    override val stateId: String = "multi_state_ui_${super.stateId}"
}
