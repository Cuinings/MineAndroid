package com.cn.tosetting

import android.content.Context
import android.widget.Toast

object SettingExecutor {

    private var config: SettingConfig = SettingConfig()

    fun init(config: SettingConfig.() -> Unit) {
        this.config = SettingConfig().apply(config)
    }

    fun execute(context: Context, setting: Setting): Boolean {
        val result = setting.execute(context)
        if (!result && config.showErrorToast) {
            Toast.makeText(context, "无法打开${setting.title}", Toast.LENGTH_SHORT).show()
        }
        config.onResult?.invoke(setting, result)
        return result
    }

    fun executeAll(context: Context, settings: List<Setting>): Map<Setting, Boolean> {
        return settings.associateWith { execute(context, it) }
    }

    fun executeByCategory(context: Context, category: SettingCategory): Map<Setting, Boolean> {
        return executeAll(context, Setting.byCategory(category))
    }

    class SettingConfig {
        var showErrorToast: Boolean = true
        var onResult: ((Setting, Boolean) -> Unit)? = null

        fun onResult(listener: (Setting, Boolean) -> Unit) {
            onResult = listener
        }
    }
}

fun Context.openSetting(setting: Setting): Boolean {
    return SettingExecutor.execute(this, setting)
}

fun Context.openSettings(vararg settings: Setting): Map<Setting, Boolean> {
    return SettingExecutor.executeAll(this, settings.toList())
}

fun Context.openSettingsByCategory(category: SettingCategory): Map<Setting, Boolean> {
    return SettingExecutor.executeByCategory(this, category)
}
