package com.cn.core.ui.viewmodel

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 9:24
 * @Description:
 */
sealed class MultiStateIntent: UiIntent {
    data class SwitchState(val targetKey: String) : MultiStateIntent()
    data class UpdateSpecificState<S : UiState>(
        val stateKey: String,
        val update: (S) -> S
    ) : MultiStateIntent()
    object ResetAllStates : MultiStateIntent()
}