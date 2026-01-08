package com.cn.app.test

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.cn.core.ui.activity.BasicActivity
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 启动页 - 解决启动白屏问题
 */
class SplashActivity : BasicActivity() {

    private lateinit var splitInstallManager: SplitInstallManager
    private val resourceModuleName = "app_test_resources"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 SplitInstallManager
        splitInstallManager = SplitInstallManagerFactory.create(this)

        // 加载资源包
        loadResourceModule()
    }

    /**
     * 加载资源包模块
     */
    private fun loadResourceModule() {
        // 检查模块是否已安装
        if (splitInstallManager.installedModules.contains(resourceModuleName)) {
            Log.d(TAG, "资源包已安装，直接跳转")
            navigateToNextScreen()
            return
        }

        Log.d(TAG, "开始加载资源包")

        // 创建安装请求
        val request = SplitInstallRequest.newBuilder()
            .addModule(resourceModuleName)
            .build()

        // 开始安装
        splitInstallManager.startInstall(request)
            .addOnSuccessListener {
                Log.d(TAG, "资源包安装请求提交成功: $it")
            }
            .addOnFailureListener {
                Log.e(TAG, "资源包安装请求失败", it)
                Toast.makeText(this, "资源包加载失败: ${it.message}", Toast.LENGTH_SHORT).show()
                // 即使加载失败，也继续应用流程
                navigateToNextScreen()
            }

        // 监听安装状态
        splitInstallManager.registerListener {sessionState ->
            when (sessionState.status()) {
                SplitInstallSessionStatus.DOWNLOADING -> {
                    Log.d(TAG, "资源包下载中: ${sessionState.bytesDownloaded()}/${sessionState.totalBytesToDownload()}")
                }
                SplitInstallSessionStatus.INSTALLED -> {
                    Log.d(TAG, "资源包安装成功")
                    // 资源包安装成功后跳转
                    navigateToNextScreen()
                }
                SplitInstallSessionStatus.FAILED -> {
                    Log.e(TAG, "资源包安装失败: ${sessionState.errorCode()}")
                    Toast.makeText(this, "资源包加载失败", Toast.LENGTH_SHORT).show()
                    // 即使加载失败，也继续应用流程
                    navigateToNextScreen()
                }
                else -> {
                    Log.d(TAG, "资源包状态: ${sessionState.status()}")
                }
            }
        }
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