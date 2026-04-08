package com.cn.board.meet.home.blur

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cn.board.meet.home.R

/**
 * @author: cn
 * @time: 2026/4/8 10:11
 * @history
 * @description:
 */
class BlurActivity : AppCompatActivity() {

    private lateinit var blurView: CrossWindowBlurBackgroundView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置窗口为透明
        setupTransparentWindow()

        setContentView(R.layout.activity_blur)

        // 初始化模糊View
        blurView = findViewById(R.id.cross_window_blur_view)

        // 设置配置
        val config = CrossWindowBlurBackgroundView.Config(
            blurRadius = 20,
            cornerRadius = 16f,
            overlayColor = Color.WHITE,
            overlayAlpha = 0.6f
        )

        // 启用模糊
        blurView.enableCrossWindowBlur(config)

        // 设置监听器
        blurView.setOnBlurStateChangedListener(object :
            CrossWindowBlurBackgroundView.OnBlurStateChangedListener {
            override fun onBlurEnabled(isEnabled: Boolean) {
                // 处理模糊启用/禁用
                if (!isEnabled) {
                    // 显示提示
                    showToast("系统禁用了模糊效果")
                }
            }

            override fun onBlurRadiusChanged(radius: Int) {
                // 处理半径变化
            }
        })

        // 设置控制按钮
        setupControlButtons()
    }

    private fun setupTransparentWindow() {
        val window = window

        // 设置窗口为半透明
//        window.setTranslucent(true)

        // Android 12+ 启用窗口模糊支持
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 启用跨窗口模糊
            window.attributes = window.attributes.apply {
                // 设置窗口属性以支持模糊
                this.blurBehindRadius = 0  // 由各个View单独控制
//                this.areBlurBehindEffectsSupported = true
            }
        }

        // 设置状态栏和导航栏透明
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // 允许绘制到系统栏后面
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 控制状态栏和导航栏
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun setupControlButtons() {
        // 模糊强度控制
        findViewById<View>(R.id.btn_light).setOnClickListener {
            blurView.updateBlurRadius(10)
        }

        findViewById<View>(R.id.btn_medium).setOnClickListener {
            blurView.updateBlurRadius(20)
        }

        findViewById<View>(R.id.btn_heavy).setOnClickListener {
            blurView.updateBlurRadius(30)
        }

        // 圆角控制
        findViewById<View>(R.id.btn_round).setOnClickListener {
            blurView.setCornerRadius(16f)
        }

        findViewById<View>(R.id.btn_rect).setOnClickListener {
            blurView.setCornerRadius(0f)
        }

        // 模糊开关
        findViewById<View>(R.id.btn_toggle).setOnClickListener {
            if (blurView.isEnabled) {
                blurView.disableCrossWindowBlur()
            } else {
                blurView.enableCrossWindowBlur()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // 暂停时禁用模糊以节省性能
        blurView.disableCrossWindowBlur()
    }

    override fun onResume() {
        super.onResume()
        // 恢复时重新启用模糊
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (windowManager.isCrossWindowBlurEnabled) {
                blurView.enableCrossWindowBlur()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        blurView.disableCrossWindowBlur()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}