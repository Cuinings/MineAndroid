package com.cn.library.commom.viewmodel

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _uiStateFlow = MutableStateFlow(this.initUIState())
    private val _uiIntentFlow = Channel<E>()

    val uiStateFlow: SharedFlow<S> = _uiStateFlow.asStateFlow()
    val uiIntentFlow: Flow<E> = _uiIntentFlow.receiveAsFlow()


    fun sendUiState(copy: S.() -> S) = _uiStateFlow.update { copy(_uiStateFlow.value.copy()) }
    fun sendUIIntent(intent: E) = viewModelScope.launch { _uiIntentFlow.send(intent) }

    init {
        TAG = javaClass.simpleName
        viewModelScope.launch { _uiIntentFlow.receiveAsFlow().collect { handleEvent(it) } }
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