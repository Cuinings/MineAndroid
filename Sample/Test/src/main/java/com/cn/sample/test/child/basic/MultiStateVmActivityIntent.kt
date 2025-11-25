package com.cn.sample.test.child.basic

import com.cn.library.commom.viewmodel.UiIntent

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 10:00
 * @Description:
 */
sealed class MultiStateVmActivityIntent : UiIntent {
    // 用户配置相关意图
    data class ChangeTheme(val theme: String) : MultiStateVmActivityIntent()
    data class ChangeLanguage(val language: String) : MultiStateVmActivityIntent()
    data class ToggleNotifications(val enabled: Boolean) : MultiStateVmActivityIntent()

    // 应用数据相关意图
    object SyncData : MultiStateVmActivityIntent()
    data class UpdateUserData(val key: String, val value: Any) : MultiStateVmActivityIntent()

    // UI 相关意图
    data class NavigateTo(val screen: String) : MultiStateVmActivityIntent()
    object GoBack : MultiStateVmActivityIntent()
    data class ShowError(val message: String) : MultiStateVmActivityIntent()
}