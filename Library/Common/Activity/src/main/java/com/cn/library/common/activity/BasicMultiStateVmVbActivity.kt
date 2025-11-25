package com.cn.library.common.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.cn.library.commom.viewmodel.MultiStateIntent
import com.cn.library.commom.viewmodel.MultiStateMVIViewModel
import com.cn.library.commom.viewmodel.StateContainer
import com.cn.library.commom.viewmodel.UiEffect
import com.cn.library.commom.viewmodel.UiIntent
import com.cn.library.commom.viewmodel.UiState
import kotlinx.coroutines.launch

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 9:33
 * @Description:
 */
@Suppress("UNCHECKED_CAST")
abstract class BasicMultiStateVmVbActivity<
        VB: ViewBinding,
        S: UiState,
        I: UiIntent,
        E: UiEffect,
        VM: MultiStateMVIViewModel<S, I, E>,
>(private val block: (LayoutInflater) -> VB): BasicVBActivity<VB>(block) {

    protected abstract val viewModel: VM

    // 状态观察器映射
    private val stateObservers = mutableMapOf<String, (S) -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeUI()
        observeStateContainer()
        observeEffects()
    }

    protected abstract fun initializeUI()

    /**
     * 注册状态观察器
     */
    protected fun registerStateObserver(stateKey: String, observer: (S) -> Unit) {
        stateObservers[stateKey] = observer
    }

    /**
     * 切换活动状态
     */
    protected fun switchActiveState(targetKey: String) {
        viewModel.processIntent(MultiStateIntent.SwitchState(targetKey))
    }

    /**
     * 观察状态容器变化
     */
    private fun observeStateContainer() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stateContainer.collect { container ->
                    // 通知活动状态变化
                    container.getActiveState()?.let { activeState ->
                        onActiveStateChanged(activeState)
                    }

                    // 通知特定状态观察器
                    stateObservers.forEach { (stateKey, observer) ->
                        container.getState(stateKey)?.let { state ->
                            observer(state)
                        }
                    }

                    onStateContainerUpdated(container)
                }
            }
        }
    }

    /**
     * 观察副作用
     */
    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    handleEffect(effect)
                }
            }
        }
    }

    /**
     * 抽象方法 - 活动状态变化
     */
    protected abstract fun onActiveStateChanged(state: S)

    /**
     * 抽象方法 - 状态容器更新
     */
    protected abstract fun onStateContainerUpdated(container: StateContainer<S>)

    /**
     * 抽象方法 - 处理副作用
     */
    protected abstract fun handleEffect(effect: E)

    /**
     * 安全发送意图
     */
    protected fun safeProcessIntent(intent: I) {
        try {
            viewModel.processIntent(intent)
        } catch (e: Exception) {
            handleIntentError(e, intent)
        }
    }

    /**
     * 错误处理
     */
    protected open fun handleIntentError(error: Exception, intent: I) {
        if (viewModel.debugMode) {
            println("MULTI_STATE_ERROR: Intent failed: ${intent::class.simpleName}")
        }
    }
}