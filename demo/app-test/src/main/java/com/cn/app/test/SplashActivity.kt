package com.cn.app.test

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.cn.core.ui.activity.BasicActivity

/**
 * 启动页 - 解决启动白屏问题
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : BasicActivity() {

    private val splashDelay = 1500L // 启动页显示时间：1.5秒

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 延迟后跳转到引导页或主页面
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, splashDelay)
    }

    /**
     * 跳转到下一个页面
     */
    private fun navigateToNextScreen() {
        // 检查是否是首次启动应用
        val isFirstLaunch = checkIsFirstLaunch()

        val intent = if (isFirstLaunch) {
            Intent(this, GuideActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        finish() // 结束启动页，防止用户返回
    }

    /**
     * 检查是否是首次启动
     */
    private fun checkIsFirstLaunch(): Boolean {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return !sharedPref.getBoolean("guide_shown", false)
    }

    override fun useSystemWallpaper(): Boolean = false
}