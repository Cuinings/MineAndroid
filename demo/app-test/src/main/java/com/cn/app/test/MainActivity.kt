package com.cn.app.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.cn.core.ui.activity.BasicActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : BasicActivity() {

    // 初始化ViewModel
    private val viewModel by lazy { MainViewModel() }

    // UI组件
    private lateinit var tvMessage: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCounter: TextView
    private lateinit var btnDecrement: Button
    private lateinit var btnIncrement: Button
    private lateinit var btnShowToast: Button
    private lateinit var btnStartLoading: Button
    private lateinit var btnStopLoading: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 初始化UI组件
        initUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化UI事件
        initUIEvents()

        // 观察状态变化
        observeState()

        // 观察副作用
        observeEffects()
    }

    /**
     * 初始化UI组件
     */
    private fun initUI() {
        tvMessage = findViewById(R.id.tvMessage)
        progressBar = findViewById(R.id.progressBar)
        tvCounter = findViewById(R.id.tvCounter)
        btnDecrement = findViewById(R.id.btnDecrement)
        btnIncrement = findViewById(R.id.btnIncrement)
        btnShowToast = findViewById(R.id.btnShowToast)
        btnStartLoading = findViewById(R.id.btnStartLoading)
        btnStopLoading = findViewById(R.id.btnStopLoading)
    }

    /**
     * 初始化UI事件
     */
    private fun initUIEvents() {
        // 增加按钮点击事件
        btnIncrement.setOnClickListener {
            viewModel.processIntent(MainIntent.Increment)
        }

        // 减少按钮点击事件
        btnDecrement.setOnClickListener {
            viewModel.processIntent(MainIntent.Decrement)
        }

        // 显示Toast按钮点击事件
        btnShowToast.setOnClickListener {
            viewModel.processIntent(MainIntent.ShowToast)
        }

        // 开始加载按钮点击事件
        btnStartLoading.setOnClickListener {
            viewModel.processIntent(MainIntent.StartLoading)
        }

        // 停止加载按钮点击事件
        btnStopLoading.setOnClickListener {
            viewModel.processIntent(MainIntent.StopLoading)
        }

        // 消息文本点击事件（用于演示ChangeMessage意图）
        tvMessage.setOnClickListener {
            viewModel.processIntent(MainIntent.ChangeMessage("Updated Message"))
        }
    }

    /**
     * 观察状态变化
     */
    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                Log.d(TAG, "observeState: $state")
                // 更新计数器显示
                tvCounter.text = state.counter.toString()

                // 更新消息显示
                tvMessage.text = state.message

                // 更新加载状态
                progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    /**
     * 观察副作用
     */
    private fun observeEffects() {
        lifecycleScope.launch {
            viewModel.effect.collectLatest { effect ->
                Log.d(TAG, "observeEffects: $effect")
                when (effect) {
                    // 处理显示Toast副作用
                    is MainEffect.ShowToastMessage -> {
                        Toast.makeText(this@MainActivity, effect.message, Toast.LENGTH_SHORT).show()
                    }

                    // 处理导航副作用（这里只是演示，没有实际导航）
                    is MainEffect.NavigateToDetail -> {
                        Toast.makeText(this@MainActivity, "Navigate to detail with id: ${effect.id}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun useSystemWallpaper(): Boolean = true
}