package com.cn.app.test.fragment

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cn.app.test.R

/**
 * 消息 Fragment —— 演示业务状态保留
 */
class MessageFragment : Fragment(R.layout.fragment_message) {

    private var msgCount = 0
    private var tvMsgCount: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvMsgCount = view.findViewById(R.id.tv_msg_count)
        tvMsgCount?.text = "$msgCount 条新消息"

        view.findViewById<Button>(R.id.btn_simulate).setOnClickListener {
            msgCount++
            tvMsgCount?.text = "$msgCount 条新消息"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("FragmentDemo", "MessageFragment onCreate")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        android.util.Log.d("FragmentDemo", "MessageFragment onDestroyView")
        tvMsgCount = null
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("FragmentDemo", "MessageFragment onDestroy")
    }
}
