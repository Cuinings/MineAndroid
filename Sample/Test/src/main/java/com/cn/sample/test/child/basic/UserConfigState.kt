package com.cn.sample.test.child.basic

import com.cn.library.commom.viewmodel.UiState

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 9:59
 * @Description:
 */
data class UserConfigState(
    val theme: String = "light",
    val language: String = "zh",
    val notificationsEnabled: Boolean = true,
    val fontSize: Int = 14
) : UiState {
    override val stateId: String = "user_config_${super.stateId}"
}
