package com.cn.app.test.fragment

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cn.app.test.R

/**
 * 首页 Fragment —— 演示状态保留
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var clickCount = 0
    private var tvCounter: TextView? = null
    private var etInput: EditText? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvCounter = view.findViewById(R.id.tv_counter)
        etInput = view.findViewById(R.id.et_input)

        view.setOnClickListener {
            clickCount++
            tvCounter?.text = "点击: $clickCount 次"
        }

        // 恢复之前的计数
        tvCounter?.text = "点击: $clickCount 次"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("FragmentDemo", "HomeFragment onCreate")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        android.util.Log.d("FragmentDemo", "HomeFragment onDestroyView")
        tvCounter = null
        etInput = null
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("FragmentDemo", "HomeFragment onDestroy")
    }
}
