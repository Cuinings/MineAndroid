
package com.cn.sample.test.sample.mvi

import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import com.cn.library.common.activity.BasicStateVmVbActivity
import com.cn.sample.test.databinding.ActivityCounterBinding


/**
 * @Author: CuiNing
 * @Time: 2025/11/6 17:12
 * @Description:
 */

class CounterActivity: BasicStateVmVbActivity<ActivityCounterBinding, CounterState, CounterIntent, CounterEffect, CounterActivityViewModel>({
    ActivityCounterBinding.inflate(it)
}) {

    override val viewModel by viewModels<CounterActivityViewModel>()

    private lateinit var tvCount: TextView
    private lateinit var tvAction: TextView
    private lateinit var btnIncrement: Button
    private lateinit var btnDecrement: Button
    private lateinit var btnReset: Button
    private lateinit var btnLoadData: Button

    override fun initializeUI() {
        // 创建简单布局
        tvCount = TextView(this).apply { textSize = 24f }
        tvAction = TextView(this).apply { textSize = 16f }
        btnIncrement = Button(this).apply { text = "增加" }
        btnDecrement = Button(this).apply { text = "减少" }
        btnReset = Button(this).apply { text = "重置" }
        btnLoadData = Button(this).apply { text = "加载数据" }

        val layout = androidx.appcompat.widget.LinearLayoutCompat(this).apply {
            orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
            addView(tvCount)
            addView(tvAction)
            addView(btnIncrement)
            addView(btnDecrement)
            addView(btnReset)
            addView(btnLoadData)
        }

        setContentView(layout)

        // 设置点击事件
        btnIncrement.setOnClickListener {
            viewModel.processIntent(CounterIntent.Increment)
        }
        btnDecrement.setOnClickListener {
            viewModel.processIntent(CounterIntent.Decrement)
        }
        btnReset.setOnClickListener {
            viewModel.processIntent(CounterIntent.Reset)
        }
        btnLoadData.setOnClickListener {
            viewModel.processIntent(CounterIntent.LoadData)
        }
    }

    override fun renderState(state: CounterState) {
        tvCount.text = "当前计数: ${state.count}"
        tvAction.text = "最后操作: ${state.lastAction}"

        val isLoading = state.isLoading
        btnIncrement.isEnabled = !isLoading
        btnDecrement.isEnabled = !isLoading
        btnReset.isEnabled = !isLoading
        btnLoadData.isEnabled = !isLoading

        if (isLoading) {
            tvAction.text = "加载中..."
        }
    }

    override fun renderEffect(effect: CounterEffect) {
        when (effect) {
            is CounterEffect.ShowToast -> {
                // 实际项目中显示Toast
                android.widget.Toast.makeText(this, effect.message, android.widget.Toast.LENGTH_SHORT).show()
            }
            is CounterEffect.ShowSnackBar -> {
                // 实际项目中显示Snackbar
            }
            is CounterEffect.NavigateToNext -> {
                // 实际项目中处理导航
            }
        }
    }
}
