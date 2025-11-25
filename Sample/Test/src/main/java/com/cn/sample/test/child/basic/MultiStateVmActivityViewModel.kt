package com.cn.sample.test.child.basic

import androidx.lifecycle.viewModelScope
import com.cn.library.commom.viewmodel.MultiStateIntent
import com.cn.library.commom.viewmodel.MultiStateMVIViewModel
import com.cn.library.commom.viewmodel.UiIntent
import com.cn.library.commom.viewmodel.UiState
import kotlinx.coroutines.launch

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 10:03
 * @Description:
 */
class MultiStateVmActivityViewModel: MultiStateMVIViewModel<UiState, UiIntent, MultiStateVmActivityEffect>(
    initialStateProvider = { key ->
        when(key) {
            "user_config" -> UserConfigState()
            "app_data" -> AppDataState()
            "ui" -> MultiStateVmActivityUiState()
            else -> throw IllegalArgumentException("Unknown state key: $key")
        }
    }
) {

    init {
        // 注册多个独立状态
        registerState("user_config")
        registerState("app_data")
        registerState("ui")
        viewModelScope.launch {
            processIntent(MultiStateIntent.SwitchState("ui"))
        }
    }

    override suspend fun handleIntent(intent: UiIntent) {
        when (intent) {
            // 用户配置处理
            is MultiStateVmActivityIntent.ChangeTheme -> handleChangeTheme(intent.theme)
            is MultiStateVmActivityIntent.ChangeLanguage -> handleChangeLanguage(intent.language)
            is MultiStateVmActivityIntent.ToggleNotifications -> handleToggleNotifications(intent.enabled)

            // 应用数据处理
            is MultiStateVmActivityIntent.SyncData -> handleSyncData()
            is MultiStateVmActivityIntent.UpdateUserData -> handleUpdateUserData(intent.key, intent.value)

            // UI 处理
            is MultiStateVmActivityIntent.NavigateTo -> handleNavigateTo(intent.screen)
            is MultiStateVmActivityIntent.GoBack -> handleGoBack()
            is MultiStateVmActivityIntent.ShowError -> handleShowError(intent.message)
        }
    }

    private suspend fun handleChangeTheme(theme: String) {
        updateState("user_config") {
            (this as UserConfigState).copy(theme = theme)
        }
        sendEffect(MultiStateVmActivityEffect.ShowToast("主题已切换为: $theme", "user_config"))
    }

    private suspend fun handleChangeLanguage(language: String) {
        updateState("user_config") { (this as UserConfigState).copy(language = language)}
        sendEffect(MultiStateVmActivityEffect.ShowToast("语言已切换为: $language", "user_config"))
    }

    private suspend fun handleToggleNotifications(enabled: Boolean) {
        updateState("user_config") {
            (this as UserConfigState).copy(notificationsEnabled = enabled)
        }
        val message = if (enabled) "通知已启用" else "通知已禁用"
        sendEffect(MultiStateVmActivityEffect.ShowToast(message, "user_config"))
    }

    private suspend fun handleSyncData() {
        // 更新应用数据状态为同步中
        updateState("app_data") {
            (this as AppDataState).copy(isSyncing = true, syncError = null)
        }

        // 模拟同步过程
        kotlinx.coroutines.delay(2000)

        // 同步完成
        updateState("app_data") {
            (this as AppDataState).copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                userData = mapOf("lastSync" to System.currentTimeMillis())
            )
        }

        sendEffect(MultiStateVmActivityEffect.ShowToast("数据同步完成", "app_data"))
    }

    private suspend fun handleUpdateUserData(key: String, value: Any) {
        updateState("app_data") {
            val appDataState = this as AppDataState
            appDataState.copy(userData = appDataState.userData + (key to value))
        }
    }

    private suspend fun handleNavigateTo(screen: String) {
        updateState("ui") {
            val uiState = this as MultiStateVmActivityUiState
            val newStack = uiState.navigationStack + uiState.currentScreen
            uiState.copy(
                currentScreen = screen,
                navigationStack = newStack
            )
        }

        if (screen == "settings") {
            sendEffect(MultiStateVmActivityEffect.NavigateToSettings)
        }
    }

    private suspend fun handleGoBack() {
        updateState("ui") {
            val uiState = this as MultiStateVmActivityUiState
            if (uiState.navigationStack.isNotEmpty()) {
                val newStack = uiState.navigationStack.dropLast(1)
                val previousScreen = uiState.navigationStack.lastOrNull() ?: "home"
                uiState.copy(
                    currentScreen = previousScreen,
                    navigationStack = newStack
                )
            } else {
                uiState
            }
        }
    }

    private suspend fun handleShowError(message: String) {
        updateState("ui") {
            (this as MultiStateVmActivityUiState).copy(errorMessage = message)
        }
        sendEffect(MultiStateVmActivityEffect.ShowSnackbar(message, "ui"))
    }
}