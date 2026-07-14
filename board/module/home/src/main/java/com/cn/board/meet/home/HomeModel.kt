package com.cn.board.meet.home

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.cn.board.meet.home.app.AppStreamAggregator
import com.cn.core.ui.viewmodel.BasicMviViewModel
import com.cn.core.ui.viewmodel.UiEffect
import com.cn.core.ui.viewmodel.UiIntent
import com.cn.core.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home 业务模型（应用级单例）。
 *
 * 通过 [HomeModelInitializer] 在 App 启动早期（早于 ProxyApplication.onCreate）由
 * androidx.startup 自动触发预热，对 ProxyApplication 零侵入。
 */
sealed class HomeModelState : UiState {
    object Init : HomeModelState()
}

sealed class HomeModelIntent : UiIntent {
    /** 触发 App 信息流预热：扫描已装应用并写入/对齐 AppInfo 表 */
    object InitAppStream : HomeModelIntent()
}

sealed class HomeModelEffect : UiEffect

object HomeModel :
    BasicMviViewModel<HomeModelState, HomeModelIntent, HomeModelEffect>(HomeModelState.Init) {

    private val appStreamAggregator = AppStreamAggregator()

    override suspend fun handleIntent(intent: HomeModelIntent) {
        Log.d(HomeModel::class.simpleName, "handleIntent: ${intent.javaClass.simpleName}")
        when (intent) {
            HomeModelIntent.InitAppStream ->
                // packageManager 扫描 + 数据库读写属重活，切到 IO 避免阻塞主线程
                withContext(Dispatchers.IO) {
                    appStreamAggregator.init()
                }
        }
    }
}
