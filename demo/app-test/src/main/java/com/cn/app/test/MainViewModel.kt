package com.cn.app.test

import com.cn.core.ui.viewmodel.BasicMviViewModel
import com.cn.core.ui.viewmodel.UiEffect
import com.cn.core.ui.viewmodel.UiIntent
import com.cn.core.ui.viewmodel.UiState

// 1. 定义UI状态类，实现UiState接口
data class MainState(
    val counter: Int = 0,
    val message: String = "Hello MVI",
    val isLoading: Boolean = false
) : UiState

// 2. 定义意图类，实现UiIntent接口
sealed class MainIntent : UiIntent {
    object Increment : MainIntent()
    object Decrement : MainIntent()
    data class ChangeMessage(val newMessage: String) : MainIntent()
    object ShowToast : MainIntent()
    object StartLoading : MainIntent()
    object StopLoading : MainIntent()
}

// 3. 定义副作用类，实现UiEffect接口
sealed class MainEffect : UiEffect {
    data class ShowToastMessage(val message: String) : MainEffect()
    data class NavigateToDetail(val id: Int) : MainEffect()
}

// 4. 实现ViewModel类，继承BasicMviViewModel
class MainViewModel : BasicMviViewModel<MainState, MainIntent, MainEffect>(
    initialState = MainState()
) {
    override suspend fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.Increment -> {
                updateState { copy(counter = counter + 1) }
            }
            is MainIntent.Decrement -> {
                updateState { copy(counter = counter - 1) }
            }
            is MainIntent.ChangeMessage -> {
                updateState { copy(message = intent.newMessage) }
            }
            is MainIntent.ShowToast -> {
                sendEffect(MainEffect.ShowToastMessage("Current counter: ${currentState.counter}"))
            }
            is MainIntent.StartLoading -> {
                updateState { copy(isLoading = true) }
            }
            is MainIntent.StopLoading -> {
                updateState { copy(isLoading = false) }
            }
        }
    }
}
