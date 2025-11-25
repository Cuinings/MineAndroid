package com.cn.sample.test.main

import androidx.lifecycle.viewModelScope
import com.cn.library.commom.viewmodel.BasicMviViewModel
import com.cn.library.commom.viewmodel.UiEffect
import com.cn.library.commom.viewmodel.UiIntent
import com.cn.library.commom.viewmodel.UiState
import com.cn.sample.test.sample.mvi.CounterEffect
import com.cn.sample.test.sample.mvi.CounterIntent
import com.cn.sample.test.sample.mvi.CounterState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/8/18 9:41
 * @Description:
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(): BasicMviViewModel<CounterState, CounterIntent, CounterEffect>(
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
                count = count + 1,
                lastAction = "INCREMENT"
            )
        }
        sendEffect(CounterEffect.ShowToast("计数增加至 ${currentState.count}"))
    }

    private suspend fun handleDecrement() {
        updateState {
            copy(
                count = count - 1,
                lastAction = "DECREMENT"
            )
        }
        if (currentState.count <= 0) {
            sendEffect(CounterEffect.ShowSnackBar("计数不能为负数"))
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
                count = 42,
                lastAction = "LOAD_DATA"
            )
        }
        sendEffect(CounterEffect.NavigateToNext)
    }
}