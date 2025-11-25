package com.cn.library.commom.viewmodel

import androidx.annotation.Keep
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @Author:         cn
 * @Date:           2025/4/9 17:26
 * @Description:
 */
/**
 * MVI ViewModel 基类
 * @param S 状态类型，必须实现 UiState
 * @param I 意图类型，必须实现 UiIntent
 * @param E 副作用类型，必须实现 UiEffect
 * @param initialState 初始状态
 */
abstract class BasicMviViewModel<S: UiState, I: UiIntent, E: UiEffect>(
    val initialState: S
): BasicViewModel() {

    // 状态管理
    private val _state = MutableStateFlow(initialState)
    val state: SharedFlow<S> = _state.asStateFlow()

    // 副作用管理（Channel 确保一次性消费）
    private val _effect = Channel<E>()
    val effect = _effect.receiveAsFlow()

    // 当前状态（只读）
    val currentState: S
        get() = _state.value
    /**
     * 处理意图的入口方法
     */
    fun processIntent(intent: I) = viewModelScope.launch { handleIntent(intent) }

    /**
     * 处理意图
     */
    abstract fun handleIntent(intent: I)

    /**
     * 安全更新状态 - 确保创建新状态对象
     */
    protected fun updateState(update: S.() -> S) {
        _state.update { currentState ->
            update(currentState)
        }
    }

    /**
     * 发送副作用（一次性事件）
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    /**
     * 重置状态到初始值
     */
    protected fun resetState() {
        _state.value = initialState
    }

}
