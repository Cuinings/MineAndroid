package com.cn.sample.test.sample.mvi

import androidx.lifecycle.viewModelScope
import com.cn.library.commom.viewmodel.BasicMviViewModel
import com.cn.library.commom.viewmodel.UiEffect
import com.cn.library.commom.viewmodel.UiIntent
import com.cn.library.commom.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * @Author: CuiNing
 * @Time: 2025/11/6 17:12
 * @Description:
 */

data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val lastAction: String = "NONE"
): UiState

sealed class CounterIntent: UiIntent {
    object Increment : CounterIntent()
    object Decrement : CounterIntent()
    object Reset : CounterIntent()
    data class SetValue(val value: Int) : CounterIntent()
    object LoadData : CounterIntent()
}

sealed class CounterEffect: UiEffect {
    data class ShowToast(val message: String) : CounterEffect()
    data class ShowSnackbar(val message: String) : CounterEffect()
    object NavigateToNext : CounterEffect()
}
@HiltViewModel
class CounterActivityViewModel @Inject constructor(): BasicMviViewModel<CounterState, CounterIntent, CounterEffect>(
    initialState = CounterState()
) {

    override fun handleIntent(intent: CounterIntent) { viewModelScope.launch {
        when (intent) {
            is CounterIntent.Increment -> handleIncrement()
            is CounterIntent.Decrement -> handleDecrement()
            is CounterIntent.Reset -> handleReset()
            is CounterIntent.SetValue -> handleSetValue(intent.value)
            is CounterIntent.LoadData -> handleLoadData()
        }
    } }

    private suspend fun handleIncrement() {
        updateState {
            copy(
                count = currentState.count + 1,
                lastAction = "INCREMENT"
            )
        }
        sendEffect(CounterEffect.ShowToast("计数增加至 ${currentState.count}"))
    }

    private suspend fun handleDecrement() {
        updateState {
            copy(
                count = currentState.count - 1,
                lastAction = "DECREMENT"
            )
        }
        if (currentState.count <= 0) {
            sendEffect(CounterEffect.ShowSnackbar("计数不能为负数"))
        }
    }

    private suspend fun handleReset() {
        updateState {
            CounterState(lastAction = "RESET")
        }
        sendEffect(CounterEffect.ShowToast("计数已重置"))
    }

    private suspend fun handleSetValue(value: Int) {
        updateState {
            copy(
                count = value,
                lastAction = "SET_VALUE"
            )
        }
    }

    private suspend fun handleLoadData() {
        updateState {
            copy(isLoading = true)
        }

        // 模拟网络请求
        kotlinx.coroutines.delay(1000)

        updateState {
            copy(
                isLoading = false,
                count = Random.nextInt(0, 100),
                lastAction = "LOAD_DATA"
            )
        }
        sendEffect(CounterEffect.NavigateToNext)
    }
}