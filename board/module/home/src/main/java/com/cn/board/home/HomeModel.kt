package com.cn.board.home

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cn.board.home.data.state.HomeUiState
import com.cn.board.home.domain.state.SessionStateHolder
import com.cn.board.home.domain.usecase.UseCaseFactory
import com.cn.board.home.entity.SoftEntity
import com.cn.board.home.room.repsoitory.repository
import com.cn.board.home.data.appInfoData
import com.cn.board.home.function.BooleanLiveData
import com.cn.core.ui.application.ApplicationContextExt.context
import com.cn.core.ui.viewmodel.BasicMviViewModel
import com.cn.core.ui.viewmodel.UiEffect
import com.cn.core.ui.viewmodel.UiIntent
import com.cn.core.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class HomeModelState : UiState {
    object Init : HomeModelState()
}

sealed class HomeModelIntent : UiIntent {
    object InitAppStream : HomeModelIntent()
}

sealed class HomeModelEffect : UiEffect

object HomeModel : BasicMviViewModel<HomeModelState, HomeModelIntent, HomeModelEffect>(HomeModelState.Init) {

    /** 在线状态流 (来自 Domain 层) */
    private val isOnlineFlow = SessionStateHolder.state.map { state ->
        state.isOpenAPS || state.commandDispatcherModel || state.mcuModel
    }

    /** 新版统一 UI 状态 (StateFlow) */
    val homeUiState: StateFlow<HomeUiState> = repository
        .observeHomeUiState(isOnlineFlow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /** 旧版兼容 */
    val homeAppList = MutableLiveData<MutableList<SoftEntity>>()
    val allApps: MutableSharedFlow<MutableList<SoftEntity>> = MutableSharedFlow(replay = 1)
    val softMainAppList = MutableLiveData<MutableList<SoftEntity>>()
    val appPermission = BooleanLiveData().default(true)

    fun initUseCases() {
        if (!UseCaseFactory.initialized) {
            UseCaseFactory.init(
                repository = repository,
                packageManager = context.packageManager,
                defaultAppList = appInfoData.defaultAppList,
                filterPackages = appInfoData.filterAppPkg.toSet(),
            )
        }
    }

    override suspend fun handleIntent(intent: HomeModelIntent) {
        Log.d(HomeModel::class.simpleName, "handleIntent: ${intent.javaClass.simpleName}")
        when (intent) {
            HomeModelIntent.InitAppStream -> {
                initUseCases()
                syncAndBuildHome()
            }
        }
    }

    private suspend fun syncAndBuildHome() {
        viewModelScope.launch {
            try {
                val entities = withContext(Dispatchers.IO) {
                    UseCaseFactory.syncAppDatabase.execute()
                }
                Log.d("HomeModel", "SyncAppDatabase: ${entities.size} apps")

                val (homeList, _) = UseCaseFactory.buildHomeAppList.execute()
                val filledHome = UseCaseFactory.buildHomeAppList.fillToSix(homeList.toMutableList())

                homeAppList.postValue(ArrayList(filledHome))
                softMainAppList.postValue(ArrayList(homeList))

                val allSoftware = entities.map { SoftEntity(it) }.toMutableList()
                allApps.emit(allSoftware)
                Log.d("HomeModel", "allApps emitted: ${allSoftware.size} items")

            } catch (e: Exception) {
                Log.e("HomeModel", "syncAndBuildHome failed", e)
            }
        }
    }
}
