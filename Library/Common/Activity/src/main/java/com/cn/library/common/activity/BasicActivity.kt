package com.cn.library.common.activity

import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        TAG = this.javaClass.simpleName
        useSystemWallpaper().takeIf { it }?.let {
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
     * 是否有悬浮权限
     */
    fun canDrawOverlays(action: () -> Unit) = if (Settings.canDrawOverlays(this)) action.invoke() else Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).launch()

    /**
     * 对子类开放StartActivityForResult
     */
    protected fun Intent.launch() {
        resultLauncher.launch(this)
    }

    open fun useSystemWallpaper(): Boolean = true

    companion object {
        lateinit var TAG: String
    }

}