package com.cn.app.test

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import com.cn.core.ui.activity.BasicActivity

/**
 * 引导页 - 首次启动时显示
 */
class GuideActivity : BasicActivity() {

    private val countdownTime = 3 // 倒计时时间：3秒
    private var remainingTime = countdownTime
    private lateinit var countdownHandler: Handler
    private lateinit var countdownRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)

        // 初始化UI
        val tvTitle = findViewById<TextView>(R.id.tvGuideTitle)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val tvCountdown = findViewById<TextView>(R.id.tvCountdown)

        // 设置标题
        tvTitle.text = "欢迎使用 MVI 示例应用"

        // 开始按钮点击事件
        btnStart.setOnClickListener {
            // 取消倒计时
            cancelCountdown()

            // 标记为已显示过引导页
            markGuideShown()

            // 跳转到主页面
            startActivity(Intent(this, MainActivity::class.java))
            finish() // 结束引导页，防止用户返回
        }

        // 初始化倒计时
        initCountdown(tvCountdown)

        // 启动倒计时
        startCountdown()
    }

    /**
     * 初始化倒计时
     */
    private fun initCountdown(tvCountdown: TextView) {
        countdownHandler = Handler(Looper.getMainLooper())
        countdownRunnable = Runnable {
            remainingTime--
            tvCountdown.text = "自动跳过 ${remainingTime}s"

            if (remainingTime > 0) {
                // 继续倒计时
                countdownHandler.postDelayed(countdownRunnable, 1000)
            } else {
                // 倒计时结束，自动跳转到主页面
                markGuideShown()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    /**
     * 启动倒计时
     */
    private fun startCountdown() {
        countdownHandler.postDelayed(countdownRunnable, 1000)
    }

    /**
     * 取消倒计时
     */
    private fun cancelCountdown() {
        countdownHandler.removeCallbacks(countdownRunnable)
    }

    /**
     * 标记引导页已显示
     * 使用SharedPreferences持久化存储
     */
    private fun markGuideShown() {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPref.edit().putBoolean("guide_shown", true).apply()
    }

    /**
     * 检查是否是首次启动
     */
    private fun isFirstLaunch(): Boolean {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return !sharedPref.getBoolean("guide_shown", false)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 确保在Activity销毁时取消倒计时，避免内存泄漏
        cancelCountdown()
    }

    override fun useSystemWallpaper(): Boolean = false
}