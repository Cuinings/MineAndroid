package com.cn.library.common.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.cn.library.commom.viewmodel.BasicMviViewModel
import com.cn.library.commom.viewmodel.UiEffect
import com.cn.library.commom.viewmodel.UiIntent
import com.cn.library.commom.viewmodel.UiState
import kotlinx.coroutines.launch

/**
 * @Author: CuiNing
 * @Time: 2025/11/6 17:00
 * @Description:
 */
abstract class BasicStateVmVbActivity<
        VB: ViewBinding,
        S: UiState,
        I: UiIntent,
        E: UiEffect,
        VM: BasicMviViewModel<S, I, E>
>(private val block: (LayoutInflater) -> VB): BasicVBActivity<VB>(block) {

    protected abstract val viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeUI()
        observeState()
        observeEffects()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { renderState(it) }
            }
        }
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { renderEffect(it) }
            }
        }
    }

    abstract fun initializeUI()

    abstract fun renderState(s: S)

    abstract fun renderEffect(e: E)

}