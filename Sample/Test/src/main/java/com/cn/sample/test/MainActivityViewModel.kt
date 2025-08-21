package com.cn.sample.test

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.cn.library.commom.viewmodel.BasicViewModel
import com.cn.library.commom.viewmodel.UIEvent
import com.cn.library.commom.viewmodel.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Objects
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/8/18 9:41
 * @Description:
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(): BasicViewModel<MainActivityState, MainActivityEvent>() {

    override fun initUIState(): MainActivityState = MainActivityState(
        MicPowerState.Init
    )

    override fun handleEvent(event: MainActivityEvent) {
        when(event) {
            MainActivityEvent.StartMic -> viewModelScope.launch {
                var count = 0f
                var repeatCount = 0
                while(count < 1f) {
//                    sendUiState { copy(micPowerState = MicPowerState.MicPower(repeatCount % 2 == 0, count)) }
                    sendState { state ->
                        state.update { it.copy(micPowerState = MicPowerState.MicPower(repeatCount % 2 == 0, count).apply {
                            Log.d(TAG, "handleEvent: ${this.hashCode()}")
                        }) }
                    }
                    delay(1000)
                    count += 0.01f
                    if (count >= 1f) {
                        count = 0f
                        repeatCount ++
                    }
                }
            }
        }
    }
}

data class MainActivityState(
    val micPowerState: MicPowerState
): UIState /*{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MainActivityState) return false
        return micPowerState == other.micPowerState
    }

    override fun hashCode(): Int = Objects.hash(micPowerState)
}*/

sealed class MicPowerState(): UIState {

    object Init: MicPowerState()
    data class MicPower(
        val enable: Boolean,
        val progress: Float
    ): MicPowerState() /*{
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MicPower) return false
            return enable == other.enable && progress == other.progress
        }

        override fun hashCode(): Int = Objects.hash(enable, progress)
    }*/
}

sealed class MainActivityEvent: UIEvent {
    object StartMic: MainActivityEvent()
}