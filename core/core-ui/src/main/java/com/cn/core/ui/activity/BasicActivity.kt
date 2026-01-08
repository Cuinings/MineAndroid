package com.cn.core.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * @Author: CuiNing
 * @Time: 2024/10/15 13:10
 * @Description:
 */
abstract class BasicActivity: AppCompatActivity() {

    // 使用实例属性替代静态TAG，避免线程安全问题
    open val TAG: String by lazy { this.javaClass.simpleName }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 必须在super.onCreate之前设置FLAG_SHOW_WALLPAPER
        if (useSystemWallpaper()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        }
        super.onCreate(savedInstanceState)

    }

    private var resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        onStartActivityResult(it.resultCode, it.data)
    }

    open fun onStartActivityResult(resultCode: Int, intent: Intent?) {  }

    /**
     * 检查悬浮窗权限，如果有则执行action，否则跳转权限设置
     */
    fun withOverlayPermission(action: () -> Unit) {
        if (Settings.canDrawOverlays(this)) {
            action.invoke()
        } else {
            // 使用更安全的权限请求方式
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).launch()
        }
    }

    /**
     * 对子类开放StartActivityForResult
     */
    protected fun Intent.launch() {
        resultLauncher.launch(this)
    }

    open fun useSystemWallpaper(): Boolean = true

    // 移除了静态TAG的companion object
}