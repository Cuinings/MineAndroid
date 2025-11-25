package com.cn.library.commom.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 9:25
 * @Description:
 */
abstract class MultiStateMVIViewModel<S: UiState, I: UiIntent, E: UiEffect>(
    private val initialStateProvider: (String) -> S
): BasicViewModel() {

    companion object {
        private const val MAX_HISTORY_SIZE = 50
    }

    // 状态容器管理
    private val _stateContainer = MutableStateFlow(StateContainer<S>())
    val stateContainer: StateFlow<StateContainer<S>> = _stateContainer.asStateFlow()

    // 副作用管理
    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // 调试模式
    var debugMode: Boolean = false
        private set

    // 状态变更历史（用于调试和回滚）
    private val stateHistory = mutableListOf<StateContainer<S>>()

    /**
     * 获取当前活动状态
     */
    val currentActiveState: S?
        get() = _stateContainer.value.getActiveState()

    /**
     * 获取特定状态
     */
    fun getState(key: String): S? = _stateContainer.value.getState(key)

    /**
     * 处理意图的入口方法
     */
    open fun processIntent(intent: UiIntent) {
        viewModelScope.launch {
            if (debugMode) {
                println("MULTI_STATE_DEBUG: Processing intent: ${intent.javaClass.simpleName}")
                saveStateHistory()
            }

            try {
                // 先处理多状态特定的意图
                when (intent) {
                    is MultiStateIntent.SwitchState -> {
                        handleSwitchState(intent.targetKey)
                        return@launch
                    }
                    is MultiStateIntent.UpdateSpecificState<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        handleUpdateSpecificState(intent as MultiStateIntent.UpdateSpecificState<S>)
                        return@launch
                    }
                    is MultiStateIntent.ResetAllStates -> {
                        handleResetAllStates()
                        return@launch
                    }
                }

                // 处理普通意图
                handleIntent(intent)

            } catch (e: Exception) {
                if (debugMode) {
                    println("MULTI_STATE_ERROR: Intent processing failed: ${e.message}")
                }
                handleIntentError(e, intent)
            }
        }
    }

    /**
     * 抽象方法 - 子类实现具体的意图处理逻辑
     */
    protected abstract suspend fun handleIntent(intent: UiIntent)

    /**
     * 注册新状态
     */
    protected fun registerState(key: String, initialState: S? = null) {
        _stateContainer.update { container ->
            if (container.containsState(key)) {
                if (debugMode) {
                    println("MULTI_STATE_WARN: State $key already exists")
                }
                return@update container
            }

            val newState = initialState ?: initialStateProvider(key)
            val newStates = container.states + (key to newState)
            val newActiveState = if (container.activeStateKey == null) key else container.activeStateKey

            container.copy(
                states = newStates,
                activeStateKey = newActiveState,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    /**
     * 切换活动状态
     */
    private suspend fun handleSwitchState(targetKey: String) {
        if (!_stateContainer.value.containsState(targetKey)) {
            if (debugMode) {
                println("MULTI_STATE_ERROR: State $targetKey not found")
            }
            return
        }

        _stateContainer.update { container ->
            container.copy(
                activeStateKey = targetKey,
                lastUpdated = System.currentTimeMillis()
            )
        }

        if (debugMode) {
            println("MULTI_STATE_DEBUG: Switched to state: $targetKey")
        }
    }

    /**
     * 更新特定状态
     */
    private suspend fun handleUpdateSpecificState(intent: MultiStateIntent.UpdateSpecificState<S>) {
        val currentContainer = _stateContainer.value
        val currentState = currentContainer.getState(intent.stateKey)

        if (currentState == null) {
            if (debugMode) {
                println("MULTI_STATE_ERROR: State ${intent.stateKey} not found for update")
            }
            return
        }

        _stateContainer.update { container ->
            val updatedState = intent.update(currentState)
            val newStates = container.states + (intent.stateKey to updatedState)

            container.copy(
                states = newStates,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    /**
     * 重置所有状态
     */
    private suspend fun handleResetAllStates() {
        _stateContainer.update { container ->
            val resetStates = container.states.mapValues { (key, _) ->
                initialStateProvider(key)
            }

            container.copy(
                states = resetStates,
                lastUpdated = System.currentTimeMillis()
            )
        }

        if (debugMode) {
            println("MULTI_STATE_DEBUG: All states reset to initial values")
        }
    }

    /**
     * 更新活动状态
     */
    protected fun updateActiveState(update: S.() -> S) {
        val currentContainer = _stateContainer.value
        val activeKey = currentContainer.activeStateKey
        val activeState = currentContainer.getActiveState()

        if (activeKey == null || activeState == null) {
            if (debugMode) {
                println("MULTI_STATE_ERROR: No active state to update")
            }
            return
        }

        _stateContainer.update { container ->
            val updatedState = update(activeState)
            val newStates = container.states + (activeKey to updatedState)

            container.copy(
                states = newStates,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    /**
     * 更新特定状态（公开方法）
     */
    protected fun updateState(key: String, update: S.() -> S) {
        val currentState = _stateContainer.value.getState(key)
        if (currentState == null) {
            if (debugMode) {
                println("MULTI_STATE_ERROR: State $key not found")
            }
            return
        }

        _stateContainer.update { container ->
            val updatedState = update(currentState)
            val newStates = container.states + (key to updatedState)

            container.copy(
                states = newStates,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    /**
     * 发送副作用
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            if (debugMode) {
                println("MULTI_STATE_DEBUG: Sending effect: ${effect::class.simpleName}")
            }
            _effect.send(effect)
        }
    }

    /**
     * 保存状态历史（用于调试）
     */
    @SuppressLint("NewApi")
    private fun saveStateHistory() {
        stateHistory.add(_stateContainer.value)
        if (stateHistory.size > MAX_HISTORY_SIZE) {
            stateHistory.removeFirst()
        }
    }

    /**
     * 获取状态历史（用于调试）
     */
    fun getStateHistory(): List<StateContainer<S>> = stateHistory.toList()

    /**
     * 启用调试模式
     */
    fun enableDebugMode(enable: Boolean = true) {
        this.debugMode = enable
    }

    /**
     * 错误处理
     */
    protected open suspend fun handleIntentError(error: Exception, intent: UiIntent) {
        // 子类可重写具体错误处理逻辑
    }
}