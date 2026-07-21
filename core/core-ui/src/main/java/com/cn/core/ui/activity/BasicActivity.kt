package com.cn.core.ui.activity

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cn.core.ui.locale.LocaleHelper
import com.cn.core.ui.locale.TranslationOverlay

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

    /**
     * 拦截字符串查找：优先使用导入的翻译，找不到则回退到内置资源。
     *
     * 注意：Context.getString / Fragment.getString 是 final 方法，**不能**直接 override。
     * 因此这里改为 override getResources()，返回一个拦截 getString / getText 的 Resources 子类。
     * 所有通过 getResources().getString(...) 的调用（含 XML 布局解析、Fragment 的字符串获取）
     * 都会走这条翻译路径。
     */
    @Suppress("DEPRECATION")
    private val translatingResources: Resources by lazy {
        val base = super.getResources()
        object : Resources(base.assets, base.displayMetrics, base.configuration) {
            private val langTag: String
                get() = LocaleHelper.getCurrentLanguageTag(this@BasicActivity)

            override fun getString(id: Int): String {
                return TranslationOverlay.getString(base, id, langTag) ?: super.getString(id)
            }

            override fun getString(id: Int, vararg formatArgs: Any?): String {
                return String.format(getString(id), *formatArgs)
            }

            override fun getText(id: Int): CharSequence {
                return TranslationOverlay.getString(base, id, langTag) ?: super.getText(id)
            }

            override fun getText(id: Int, def: CharSequence?): CharSequence {
                return TranslationOverlay.getString(base, id, langTag) ?: super.getText(id, def)
            }
        }
    }

    override fun getResources(): Resources = translatingResources

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