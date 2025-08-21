package com.cn.sample.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @Author: CuiNing
 * @Time: 2025/8/18 13:43
 * @Description:
 */

interface State

interface Event

data class TestState(
    val micPowerState: TestMicPowerState
): State

sealed class TestMicPowerState: State {
    object Init: TestMicPowerState()
    data class MicPower(val enable: Boolean, val progress: Float): TestMicPowerState()
}

sealed class TestMicPowerStateEvent: Event

fun main() { runBlocking {


    val scope = CoroutineScope(Dispatchers.Default)
    fun initUIState(): TestState = TestState(TestMicPowerState.Init)
    val _uiState = MutableStateFlow(initUIState())
    val uiState: SharedFlow<TestState> = _uiState.asStateFlow()


    repeat(100) { count ->
        delay(100)
        println("count:$count")
        _uiState.update { it.copy(micPowerState = TestMicPowerState.MicPower(true, count.toFloat()).apply {
            println("$this")
        }) }
    }
    uiState.map { it.micPowerState }.collect {
        when(it) {
            TestMicPowerState.Init -> {}
            is TestMicPowerState.MicPower -> println("Mic Power -> ${it.enable}, ${it.progress}")
        }
    }
} }