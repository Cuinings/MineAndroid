package com.cn.board.meet.home

import com.cn.core.ui.viewmodel.BasicMviViewModel
import com.cn.core.ui.viewmodel.UiEffect
import com.cn.core.ui.viewmodel.UiIntent
import com.cn.core.ui.viewmodel.UiState

/**
 * @author: cn
 * @time: 14/7/2026 下午 5:10
 * @history
 * @description:
 */

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

    override suspend fun handleIntent(intent: HomeActivityIntent) {
        when (intent) {
            is HomeActivityIntent.Refresh -> {
                // 进入加载态
                updateState {
                    when (this) {
                        is HomeActivityState.Init -> HomeActivityState.Ready(isRefreshing = true)
                        is HomeActivityState.Ready -> copy(isRefreshing = true)
                    }
                }
                // TODO: 在此发起真实数据加载（如 AppStreamAggregator.aggregate()）
                // 加载完成后退出加载态
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
}