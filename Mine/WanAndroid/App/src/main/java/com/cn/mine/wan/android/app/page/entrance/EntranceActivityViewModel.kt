package com.cn.mine.wan.android.app.page.entrance

import androidx.lifecycle.viewModelScope
import com.cn.library.commom.viewmodel.BasicViewModel
import com.cn.library.commom.viewmodel.UIEvent
import com.cn.library.commom.viewmodel.UIState
import com.cn.library.common.flow.collectByScope
import com.cn.mine.wan.android.entity.EntranceEntity
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.entrance.EntranceEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/6/14 10:35
 * @Description:
 */
@HiltViewModel
class EntranceActivityViewModel @Inject constructor(): BasicViewModel<EntranceActivityUIState, EntranceActivityUIEvent>() {

    @Inject
    lateinit var entranceEvent: EntranceEvent


    override fun initUIState(): EntranceActivityUIState {
        return EntranceActivityUIState(EntranceUIState.INIT)
    }

    override fun handleEvent(event: EntranceActivityUIEvent) {
        when (event) {
            EntranceActivityUIEvent.LoadEntrance -> entranceEvent.execute(null).collectByScope(viewModelScope) {
                when(it) {
                    is EventResult.Success<MutableList<EntranceEntity>> -> sendUiState { copy(entranceUIState = EntranceUIState.Entrance(it.result?:arrayListOf())) }
                    else -> {}
                }

            }
        }

    }
}

data class EntranceActivityUIState(val entranceUIState: EntranceUIState): UIState


sealed class EntranceUIState: UIState {
    object INIT: EntranceUIState()
    data class Entrance(val entrances: MutableList<EntranceEntity>): EntranceUIState()
}

sealed class EntranceActivityUIEvent: UIEvent {
    object LoadEntrance: EntranceActivityUIEvent()
}