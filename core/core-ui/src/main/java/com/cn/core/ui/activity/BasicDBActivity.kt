package com.cn.core.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ViewDataBinding

/**
 * @Author: CuiNing
 * @Time: 2024/11/22 15:48
 * @Description:
 */
abstract class BasicDBActivity<DB: ViewDataBinding>(
    private val block: (LayoutInflater) -> DB
): BasicActivity() {

    private var _binding: DB? = null

    protected val binding get() = requireNotNull(_binding)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = block(layoutInflater).apply {
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
        _binding = null
        super.onDestroy()
    }

}