package com.cn.board.home

import androidx.lifecycle.viewModelScope
import com.cn.board.home.data.state.HomeUiState
import com.cn.board.home.domain.state.SessionStateHolder
import com.cn.board.home.domain.usecase.UseCaseFactory
import com.cn.board.home.entity.SoftEntity
import com.cn.board.home.function.UnPeekLiveData
import com.cn.board.home.room.repsoitory.repository
import com.cn.board.home.data.appInfoData
import com.cn.core.ui.viewmodel.BasicMviViewModel
import com.cn.core.ui.viewmodel.UiEffect
import com.cn.core.ui.viewmodel.UiIntent
import com.cn.core.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class HomeActivityEffect: UiEffect {
    data class Toast(val msg: String): HomeActivityEffect()
    object OpenWallpaperSettings : HomeActivityEffect()
}
sealed class HomeActivityState: UiState {
    object Init: HomeActivityState()
    data class Ready(val isRefreshing: Boolean = false): HomeActivityState()
}
sealed class HomeActivityIntent: UiIntent {
    object Refresh: HomeActivityIntent()
    object OpenWallpaperSettings: HomeActivityIntent()
}

class HomeActivityViewModel: BasicMviViewModel<HomeActivityState, HomeActivityIntent, HomeActivityEffect>(HomeActivityState.Init) {

    private val isOnlineFlow = SessionStateHolder.state.map { state ->
        state.isOpenAPS || state.commandDispatcherModel || state.mcuModel
    }

    val uiState: StateFlow<HomeUiState> = repository
        .observeHomeUiState(isOnlineFlow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val appMainList: UnPeekLiveData<ArrayList<SoftEntity>> =
        UnPeekLiveData.Builder<ArrayList<SoftEntity>>().set(ArrayList()).create()

    init {
        viewModelScope.launch {
            uiState.collect { state ->
                appMainList.postValue(ArrayList(state.homeAppList))
                // 同步 in-memory mainAppData (拖拽排序依赖)
                appInfoData.mainAppData.clear()
                appInfoData.mainAppData.addAll(state.mainAppList)
            }
        }
    }

    override suspend fun handleIntent(intent: HomeActivityIntent) {
        when (intent) {
            is HomeActivityIntent.Refresh -> {
                updateState {
                    when (this) {
                        is HomeActivityState.Init -> HomeActivityState.Ready(isRefreshing = true)
                        is HomeActivityState.Ready -> copy(isRefreshing = true)
                    }
                }
                updateState {
                    when (this) {
                        is HomeActivityState.Init -> HomeActivityState.Ready(isRefreshing = false)
                        is HomeActivityState.Ready -> copy(isRefreshing = false)
                    }
                }
                sendEffect(HomeActivityEffect.Toast("refreshed"))
            }
            is HomeActivityIntent.OpenWallpaperSettings -> {
                sendEffect(HomeActivityEffect.OpenWallpaperSettings)
            }
        }
    }

    /**
     * 持久化应用首页状态（main/offlineMain 已由 Binder 在内存中翻转完毕）。
     */
    fun saveHomeAppState(entity: SoftEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            entity.appInfo?.let { repository.update(it) }
        }
    }

    fun reorderHomeApps(list: List<SoftEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            UseCaseFactory.manageHomeAppOrder.reorderHomeApps(list)
        }
    }
}
