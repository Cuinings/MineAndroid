package com.cn.board.meet.home.blur

import android.app.Activity
import android.graphics.Outline
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import java.util.function.Consumer

/**
 * @author: cn
 * @time: 2026/4/8 10:07
 * @history
 * @description:跨窗口模糊管理器
 */
object CrossWindowBlurManager {

    private var blurEnabledListener: Consumer<Boolean?>? = null

    /**
     * 为View启用跨窗口模糊
     */
    fun enableCrossWindowBlurForView(view: View, blurRadius: Int = 20) {
        val activity = view.context as? Activity ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            enableNativeCrossWindowBlur(activity, view, blurRadius)
        } else {
            // Android 11及以下的降级方案
            enableFallbackBlur(view)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun enableNativeCrossWindowBlur(activity: Activity, view: View, blurRadius: Int) {
        val window = activity.window

        // 1. 启用窗口跨窗口模糊
        window.setBackgroundBlurRadius(blurRadius)

        // 2. 监听模糊状态变化
        setupBlurStateListener(activity, view)

        // 3. 设置View的模糊背景
        setupViewBlurBackground(view, blurRadius)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupBlurStateListener(activity: Activity, view: View) {
        // 正确的监听器设置
        blurEnabledListener = Consumer { isEnabled ->
            onBlurStateChanged(isEnabled == true, view)
        }

        activity.windowManager.addCrossWindowBlurEnabledListener(
            activity.mainExecutor,
            blurEnabledListener!!
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun onBlurStateChanged(isEnabled: Boolean, view: View) {
        if (isEnabled) {
            // 系统启用了模糊
            view.post {
                // 重新应用模糊效果
                val activity = view.context as? Activity
                activity?.window?.setBackgroundBlurRadius(20)
            }
        } else {
            // 系统禁用了模糊，显示降级效果
            showFallbackEffect(view)
        }
    }

    private fun setupViewBlurBackground(view: View, blurRadius: Int) {
        // 设置View的背景为半透明，让下面的模糊效果透出来
        val alpha = when (blurRadius) {
            in 0..10 -> 0.2f
            in 11..20 -> 0.4f
            else -> 0.6f
        }

        view.setBackgroundColor(
            android.graphics.Color.argb(
                (alpha * 255).toInt(),
                255, 255, 255
            )
        )
    }

    private fun enableFallbackBlur(view: View) {
        // 使用半透明背景模拟模糊效果
        val overlayColor = android.graphics.Color.argb(102, 255, 255, 255) // 40% 白色
        view.setBackgroundColor(overlayColor)

        // 设置圆角
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(
                        0, 0,
                        view.width,
                        view.height,
                        16f
                    )
                }
            }
            view.clipToOutline = true
        }
    }

    private fun showFallbackEffect(view: View) {
        // 显示降级效果
        val fallbackColor = android.graphics.Color.argb(153, 255, 255, 255) // 60% 白色
        view.setBackgroundColor(fallbackColor)
    }

    /**
     * 移除监听器
     */
    fun removeListener(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurEnabledListener?.let {
                activity.windowManager.removeCrossWindowBlurEnabledListener(it)
            }
        }
        blurEnabledListener = null
    }
}