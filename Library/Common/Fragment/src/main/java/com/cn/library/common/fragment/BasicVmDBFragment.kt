package com.cn.library.common.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.cn.library.commom.viewmodel.BasicViewModel
import com.cn.library.commom.viewmodel.UIEvent
import com.cn.library.commom.viewmodel.UIState

/**
 * @Author: CuiNing
 * @Time: 2024/11/22 15:48
 * @Description:
 */
abstract class BasicVmDBFragment<VM: BasicViewModel<out UIState, out UIEvent>, DB: ViewDataBinding>(
    private val blockViewModel: (ViewModelProvider) -> VM,
    private val blockBinding: (LayoutInflater) -> DB
): BasicFragment() {

    private var _viewModel: VM? = null
    private var _binding: DB? = null

    protected val viewModel get() = requireNotNull(_viewModel)
    protected val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _viewModel = blockViewModel(ViewModelProvider(this))
        _binding = blockBinding(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onBindViewModel()
        onBindData()
        onObserver()
    }

    abstract fun onBindViewModel()
    abstract fun onBindData()
    abstract fun onObserver()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}