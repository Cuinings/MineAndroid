package com.cn.core.ui.viewmodel

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 9:23
 * @Description:
 */
data class StateContainer<S: UiState>(
    val states: Map<String, S> = emptyMap<String, S>(),
    val activeStateKey: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getState(key: String): S? = states[key]
    fun getActiveState(): S? = activeStateKey?.let { states[it] }
    fun containsState(key: String): Boolean = states.containsKey(key)
}
