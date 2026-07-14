package com.cn.core.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.cn.core.ui.viewmodel.BasicViewModel

/**
 * @Author: CuiNing
 * @Time: 2024/11/22 15:48
 * @Description:
 */
abstract class BasicVmDBActivity<VM: BasicViewModel, DB: ViewDataBinding>(
    private val blockViewModel: (ViewModelProvider) -> VM,
    private val blockBinding: (LayoutInflater) -> DB
): BasicActivity() {

    private var _viewModel: VM? = null
    private var _binding: DB? = null

    protected val viewModel get() = requireNotNull(_viewModel)
    protected val binding get() = requireNotNull(_binding)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _viewModel = blockViewModel(ViewModelProvider(this))
        _binding = blockBinding(layoutInflater).apply {
            setContentView(root)
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
        onBindLayout()
    }

    abstract fun onBindLayout()

    override fun onDestroy() {
        _viewModel = null
        _binding = null
        super.onDestroy()
    }

}