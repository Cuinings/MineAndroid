package com.cn.sample.test.child.basic

import com.cn.library.commom.viewmodel.UiEffect

/**
 * @Author: CuiNing
 * @Time: 2025/11/7 10:01
 * @Description:
 */
sealed class MultiStateVmActivityEffect: UiEffect {
    data class ShowToast(val message: String, override val sourceState: String) : MultiStateVmActivityEffect()
    data class ShowSnackbar(val message: String, override val sourceState: String) : MultiStateVmActivityEffect()
    object NavigateToSettings : MultiStateVmActivityEffect()

    override val sourceState: String? get() = when (this) {
        is ShowToast -> this.sourceState
        is ShowSnackbar -> this.sourceState
        is NavigateToSettings -> "ui"
    }
}