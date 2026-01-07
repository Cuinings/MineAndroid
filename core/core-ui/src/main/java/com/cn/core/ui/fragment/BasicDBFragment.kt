package com.cn.core.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding

/**
 * @Author: CuiNing
 * @Time: 2024/11/22 15:48
 * @Description:
 */
abstract class BasicDBFragment<DB: ViewDataBinding>(
    private val block: (LayoutInflater) -> DB
): BasicFragment() {

    private var _binding: DB? = null

    protected val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = block(layoutInflater)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}