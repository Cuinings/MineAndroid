package com.cn.library.common.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.cn.library.commom.viewmodel.BasicMviViewModel
import com.cn.library.commom.viewmodel.UiEffect
import com.cn.library.commom.viewmodel.UiIntent
import com.cn.library.commom.viewmodel.UiState

/**
 * @Author: CuiNing
 * @Time: 2024/11/22 15:48
 * @Description:
 */
abstract class BasicVmVBFragment<VM: BasicMviViewModel<out UiState, out UiIntent, out UiEffect>, VB: ViewBinding>(
    private val blockViewModel: (ViewModelProvider) -> VM,
    private val blockBinding: (LayoutInflater) -> VB
): BasicFragment() {

    private var _viewModel: VM? = null
    private var _binding: VB? = null

    protected val viewModel get() = requireNotNull(_viewModel)
    protected val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _viewModel = blockViewModel(ViewModelProvider(this))
        _binding = blockBinding(layoutInflater)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}