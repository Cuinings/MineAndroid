package com.cn.board.meet.home

import android.content.Context
import android.util.Log
import com.cn.board.database.AppInfo
import com.cn.board.meet.home.repository.AppRepository
import com.cn.core.ui.viewmodel.BasicMviViewModel
import com.cn.core.ui.viewmodel.UiEffect
import com.cn.core.ui.viewmodel.UiIntent
import com.cn.core.ui.viewmodel.UiState

class AppListViewModel : BasicMviViewModel<AppListViewModel.AppListState, AppListViewModel.AppListIntent, AppListViewModel.AppListEffect>(
    initialState = AppListState()
) {

    private lateinit var repository: AppRepository

    /**
     * 初始化
     */
    fun init(context: Context) {
        repository = AppRepository(context)
    }

    // 处理意图
    override suspend fun handleIntent(intent: AppListIntent) {
        when (intent) {
            is AppListIntent.ToggleExpanded -> {
                updateState {
                    copy(isExpanded = !isExpanded)
                }
                // 发送展开/收起效果
                sendEffect(AppListEffect.ExpandedStateChanged(!currentState.isExpanded))
            }
            is AppListIntent.LoadApps -> {
                // 显示加载状态
                updateState {
                    copy(isLoading = true, error = null)
                }
                
                try {
                    // 使用Repository加载应用数据
                    val appList = repository.loadApps()
                    
                    updateState {
                        copy(appList = appList, isLoading = false)
                    }
                    // 发送加载完成效果
                    sendEffect(AppListEffect.AppsLoaded)
                } catch (e: Exception) {
                    updateState {
                        copy(error = "加载失败: ${e.message}", isLoading = false)
                    }
                    Log.e(AppListViewModel::class.simpleName, "handleIntent: LoadApps Exception:${e.message}")
                    // 发送加载错误效果
                    sendEffect(AppListEffect.AppsLoadError(e.message ?: "未知错误"))
                }
            }
            is AppListIntent.SearchApps -> {
                updateState {
                    copy(searchQuery = intent.query)
                }
                
                try {
                    val searchResults = repository.searchApps(intent.query)
                    updateState {
                        copy(appList = searchResults)
                    }
                } catch (e: Exception) {
                    updateState {
                        copy(error = "搜索失败: ${e.message}")
                    }
                }
                
                // 发送搜索完成效果
                sendEffect(AppListEffect.SearchPerformed(intent.query))
            }
            is AppListIntent.SelectApp -> {
                // 更新应用使用统计信息
                try {
                    repository.updateAppUsage(intent.appInfo.id)
                } catch (e: Exception) {
                    // 忽略更新失败的情况
                }
                
                updateState {
                    copy(selectedApp = intent.appInfo)
                }
                // 发送应用选中效果
                sendEffect(AppListEffect.AppSelected(intent.appInfo))
            }
            is AppListIntent.ClearSelection -> {
                updateState {
                    copy(selectedApp = null)
                }
                // 发送选择清除效果
                sendEffect(AppListEffect.SelectionCleared)
            }
            is AppListIntent.UpdateAppSortOrder -> {
                try {
                    // 更新应用排序顺序
                    repository.updateAppSortOrders(intent.sortedList)
                    // 发送排序更新完成效果
                    sendEffect(AppListEffect.SortOrderUpdated)
                } catch (e: Exception) {
                    // 发送排序更新错误效果
                    sendEffect(AppListEffect.SortOrderUpdateError(e.message ?: "未知错误"))
                }
            }
        }
    }

    // 状态数据类
    data class AppListState(
        val isExpanded: Boolean = false,
        val appList: List<AppInfo> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val searchQuery: String = "",
        val selectedApp: AppInfo? = null
    ) : UiState

    // 意图密封类
    sealed class AppListIntent : UiIntent {
        object ToggleExpanded : AppListIntent()
        data class LoadApps(val context: Context) : AppListIntent()
        data class SearchApps(val query: String) : AppListIntent()
        data class SelectApp(val appInfo: AppInfo) : AppListIntent()
        object ClearSelection : AppListIntent()
        data class UpdateAppSortOrder(val sortedList: List<AppInfo>) : AppListIntent()
    }

    // 效果密封类
    sealed class AppListEffect : UiEffect {
        object AppsLoaded : AppListEffect()
        data class AppsLoadError(val message: String) : AppListEffect()
        data class ExpandedStateChanged(val isExpanded: Boolean) : AppListEffect()
        data class SearchPerformed(val query: String) : AppListEffect()
        data class AppSelected(val appInfo: AppInfo) : AppListEffect()
        object SelectionCleared : AppListEffect()
        object SortOrderUpdated : AppListEffect()
        data class SortOrderUpdateError(val message: String) : AppListEffect()
    }
}

