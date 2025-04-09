package com.cn.library.commom.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @Author:         cn
 * @Date:           2025/4/9 17:26
 * @Description:
 */
interface UIState
interface UIIntent
abstract class BasicViewModel<S: UIState, I: UIIntent>: ViewModel() {
    private val _uiStateFlow = MutableStateFlow(this.initUIState())

    /**
     * 初始化UI状态
     */
    abstract fun initUIState(): S

    /**
     * 处理意图
     */
    abstract fun handleIntent(intent: I)


}