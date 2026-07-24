package com.cn.board.home.domain.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 会话状态持有者。
 *
 * 替代全局可变 [com.cn.board.home.state.State] 对象。
 * 通过 StateFlow 对外暴露状态，确保线程安全和单向数据流。
 *
 * Phase 2: 当前作为兼容层，保持与旧全局 State 相同的接口。
 * Phase 3: 将逐步按职责拆分为多个更细粒度的 StateFlow。
 */
object SessionStateHolder {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    // === 便捷属性访问 ===

    val isLoadingApp: Boolean get() = _state.value.isLoadingApp
    val isOpenAPS: Boolean get() = _state.value.isOpenAPS
    val bpModel: Boolean get() = _state.value.bpModel
    val chromeEnable: Boolean get() = _state.value.chromeEnable
    val mcuModel: Boolean get() = _state.value.mcuModel
    val smModel: Boolean get() = _state.value.smModel
    val commandDispatcherModel: Boolean get() = _state.value.commandDispatcherModel
    val vrsLoginState: Boolean get() = _state.value.vrsLoginState
    val isInConf: Boolean get() = _state.value.isInConf
    val isSendAss: Boolean get() = _state.value.isSendAss

    // === 状态更新方法 ===

    fun setIsLoadingApp(value: Boolean) {
        _state.update { it.copy(isLoadingApp = value) }
    }

    fun setIsOpenAPS(value: Boolean) {
        _state.update { it.copy(isOpenAPS = value) }
    }

    fun setBpModel(value: Boolean) {
        _state.update { it.copy(bpModel = value) }
    }

    fun setChromeEnable(value: Boolean) {
        _state.update { it.copy(chromeEnable = value) }
    }

    fun setMcuModel(value: Boolean) {
        _state.update { it.copy(mcuModel = value) }
    }

    fun setSmModel(value: Boolean) {
        _state.update { it.copy(smModel = value) }
    }

    fun setCommandDispatcherModel(value: Boolean) {
        _state.update { it.copy(commandDispatcherModel = value) }
    }

    fun setVrsLoginState(value: Boolean) {
        _state.update { it.copy(vrsLoginState = value) }
    }

    fun setIsInConf(value: Boolean) {
        _state.update { it.copy(isInConf = value) }
    }

    fun update(transform: (SessionState) -> SessionState) {
        _state.update(transform)
    }
}
