package com.cn.board.meet.home

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.cn.board.meet.home.app.AppStreamAggregator
import com.cn.board.meet.home.app.SoftEntity
import com.cn.board.meet.home.state.State
import com.cn.core.ui.application.ApplicationContextExt.context
import com.cn.core.ui.viewmodel.BasicMviViewModel
import com.cn.core.ui.viewmodel.UiEffect
import com.cn.core.ui.viewmodel.UiIntent
import com.cn.core.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

object HomeModel : BasicMviViewModel<HomeModelState, HomeModelIntent, HomeModelEffect>(HomeModelState.Init) {

    private val appStreamAggregator by lazy { AppStreamAggregator(context = context) }

    /**
     * 聚合结果流：init() 扫描已装应用并写入/对齐 AppInfo 表后，把可见应用列表推送到此。
     *
     * 由 HomeModel 持有（数据流的真正 owner），producer（AppStreamAggregator）只负责产出、
     * 不持有流；StateFlow 会向新订阅者重放最新值，因此 AppAggregatorFrag 即使晚于 init() 创建，
     * 也能立即拿到已聚合好的数据，无需主动查询。
     */
    private val _appListFlow = MutableStateFlow<List<SoftEntity>>(emptyList())
    private val _minaAppListFlow = MutableStateFlow<List<SoftEntity>>(emptyList())
    val appListFlow: StateFlow<List<SoftEntity>> = _appListFlow
    val mainAppListFlow: StateFlow<List<SoftEntity>> = _minaAppListFlow

    override suspend fun handleIntent(intent: HomeModelIntent) {
        viewModelScope.launch {
            Log.d(HomeModel::class.simpleName, "handleIntent: ${intent.javaClass.simpleName}")
            when (intent) {
                HomeModelIntent.InitAppStream ->
                    // packageManager 扫描 + 数据库读写属重活，切到 IO 避免阻塞主线程
                    withContext(Dispatchers.IO) {
                        _appListFlow.value = appStreamAggregator.init()
                        _minaAppListFlow.value = appStreamAggregator.getMainApp(State.online)
                    }
            }
        }
    }

    /** 拖拽后持久化新的展示顺序（更新各 AppInfo.orderIndex 列），由 AppStreamAggregator 落库 */
    suspend fun updateAppOrder(order: List<SoftEntity>) {
        appStreamAggregator.persistOrder(order)
        _appListFlow.value = order
    }

    /**
     * 管理模式下点击应用：切换其 main（在线主页）/ offlineMain（离线主页）标记并持久化，
     * 随后刷新两条数据流——
     *  - [appListFlow]：列表条目随 main/offlineMain 变化显示/隐藏选择角标；
     *  - [mainAppListFlow]：编辑列表（带占位槽）按当前模式补入/移除主页应用，实现「替换占位 item」。
     *
     * @param entity 被点击的应用实体
     * @param online 当前模式：true=在线（改 main 字段），false=离线（改 offlineMain 字段）
     */
    suspend fun toggleMainFlag(entity: SoftEntity, online: Boolean) {
        val app = entity.appInfo ?: return
        if (online) {
            val selected = app.main != 1
            app.main = if (selected) 1 else 0
            // 选为在线主页应用时追加一个 mainIndex，保证编辑列表按 mainIndex 排序稳定
            if (selected) app.mainIndex = appStreamAggregator.nextMainIndex()
        } else {
            val selected = app.offlineMain != 1
            app.offlineMain = if (selected) 1 else 0
            if (selected) app.offlineMainIndex = appStreamAggregator.nextOfflineMainIndex()
        }
        appStreamAggregator.updateApp(app)
        // 刷新列表与编辑列表：用 queryAllApps()（不触发默认对齐）避免刚改的标记被覆盖回默认值
        _appListFlow.value = appStreamAggregator.queryAllApps()
        _minaAppListFlow.value = appStreamAggregator.getMainApp(online)
    }
}
