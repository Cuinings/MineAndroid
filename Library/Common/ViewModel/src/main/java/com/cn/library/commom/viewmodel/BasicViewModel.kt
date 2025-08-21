package com.cn.library.commom.viewmodel

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @Author:         cn
 * @Date:           2025/4/9 17:26
 * @Description:
 */
@Keep
interface UIState
@Keep
interface UIEvent
abstract class BasicViewModel<S: UIState, E: UIEvent>: ViewModel() {

    companion object {
        lateinit var TAG: String
    }

    private val _uiIntent = Channel<E>()
    private val _uiState = MutableStateFlow(this.initUIState())

    val uiState: SharedFlow<S> = _uiState.asStateFlow()

    fun sendUIIntent(intent: E) = viewModelScope.launch { _uiIntent.send(intent) }
    fun sendUiState(copy: S.() -> S) {
        _uiState.update { copy(_uiState.value) }
    }
    fun sendState(action: (MutableStateFlow<S>) -> Unit) {
        action.invoke(_uiState)
    }

    init {
        TAG = javaClass.simpleName
        viewModelScope.launch { _uiIntent.receiveAsFlow().collect { handleEvent(it) } }
    }

    /**
     * 初始化UI状态
     */
    abstract fun initUIState(): S

    /**
     * 处理意图
     */
    abstract fun handleEvent(event: E)

}

/*
class CustomMutableStateFlow<T> (
    initialValue: T,
    private val areEqual: (old: T, new: T) -> Boolean
): MutableStateFlow<T> {
    private val _state = MutableStateFlow<T>(initialValue)
    override var value: T
        get() = _state.value
        set(value) {
            if (areEqual(_state.value, value)) {
                _state.value = value
            }
        }
}*/
