package com.cn.board.meet.home

import android.content.Context
import com.cn.board.database.AppDatabase
import com.cn.board.database.AppDao
import com.cn.board.database.AppInfo
import com.cn.core.ui.viewmodel.BasicMviViewModel
import com.cn.core.ui.viewmodel.UiEffect
import com.cn.core.ui.viewmodel.UiIntent
import com.cn.core.ui.viewmodel.UiState

class AppListViewModel : BasicMviViewModel<AppListViewModel.AppListState, AppListViewModel.AppListIntent, AppListViewModel.AppListEffect>(
    initialState = AppListState()
) {

    // 保存上下文引用
    private lateinit var context: Context
    
    // 延迟初始化Room数据库
    private lateinit var db: AppDatabase
    private lateinit var appDao: AppDao

    // 初始化数据库
    fun initDatabase(context: Context) {
        this.context = context
        db = androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        )
        // 添加数据库迁移配置
        .addMigrations(com.cn.board.database.AppDatabase.MIGRATION_1_2)
        .build()
        appDao = db.appDao()
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
                
                // 使用Room数据库加载应用数据
                try {
                    // 检查数据库是否已初始化
                    if (!::appDao.isInitialized) {
                        throw IllegalStateException("数据库未初始化")
                    }
                    
                    // 检查数据库中是否已有数据
                    var appList = appDao.getAllApps()
                    
                    // 如果数据库为空，使用AppUtil获取实际的系统应用数据
                    if (appList.isEmpty()) {
                        // 在IO线程中获取系统应用数据
                        val systemApps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            AppUtil.getAllInstalledApps(intent.context)
                        }
                        
                        // 将AppUtil.AppUtilInfo转换为项目中的AppInfo类型
                        val sampleApps = systemApps.map {
                            AppInfo(
                                isSystemApp = it.isSystemApp,
                                packageName = it.packageName,
                                lastUsedTime = System.currentTimeMillis(),
                            )
                        }
                        
                        // 批量插入应用数据
                        appDao.insertApps(sampleApps)
                        // 重新从数据库加载
                        appList = appDao.getAllApps()
                    }
                    
                    updateState {
                        copy(appList = appList, isLoading = false)
                    }
                    // 发送加载完成效果
                    sendEffect(AppListEffect.AppsLoaded)
                } catch (e: Exception) {
                    updateState {
                        copy(error = "加载失败: ${e.message}", isLoading = false)
                    }
                    // 发送加载错误效果
                    sendEffect(AppListEffect.AppsLoadError(e.message ?: "未知错误"))
                }
            }
            is AppListIntent.SearchApps -> {
                updateState {
                    copy(searchQuery = intent.query)
                }
                
                // 使用Room数据库搜索应用
                try {
                    // 检查数据库是否已初始化
                    if (!::appDao.isInitialized) {
                        throw IllegalStateException("数据库未初始化")
                    }
                    
                    val searchResults = appDao.searchApps("%${intent.query}%")
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
                    if (::appDao.isInitialized) {
                        appDao.updateAppUsage(intent.appInfo.id, System.currentTimeMillis())
                    }
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
        }
    }

    // 清理数据库连接
    fun clearDatabase() {
        if (::db.isInitialized) {
            db.close()
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
    }

    // 效果密封类
    sealed class AppListEffect : UiEffect {
        object AppsLoaded : AppListEffect()
        data class AppsLoadError(val message: String) : AppListEffect()
        data class ExpandedStateChanged(val isExpanded: Boolean) : AppListEffect()
        data class SearchPerformed(val query: String) : AppListEffect()
        data class AppSelected(val appInfo: AppInfo) : AppListEffect()
        object SelectionCleared : AppListEffect()
    }
}

