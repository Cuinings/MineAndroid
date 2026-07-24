package com.cn.board.home.data.state

import com.cn.board.home.entity.SoftEntity

/**
 * Home 模块统一 UI 状态（位于 data 层，避免跨模块循环依赖）。
 */
data class HomeUiState(
    val allApps: List<SoftEntity> = emptyList(),
    val homeAppList: List<SoftEntity> = emptyList(),
    val mainAppList: List<SoftEntity> = emptyList(),
    val editMode: Boolean = false,
    val isLoading: Boolean = true,
)
