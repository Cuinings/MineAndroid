package com.cn.app.test.fragment

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cn.app.test.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 我的 Fragment —— 演示 Fragment 实例是否被重建
 * onCreate 时记录创建时间，replace 策略中每次切换都会变化
 */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val createTime: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tv_create_time).text = "创建时间: $createTime"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("FragmentDemo", "ProfileFragment onCreate @ $createTime")
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("FragmentDemo", "ProfileFragment onDestroy @ $createTime")
    }
}
