package com.cn.core.ui.locale

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * 多语言管理工具类
 *
 * 职责：
 * 1. 在 Application#attachBaseContext 中包装 Context，确保 Activity 启动时使用正确 Locale
 * 2. 运行时切换语言时，通过 AppCompatDelegate 通知系统重建所有 Activity
 * 3. 持久化用户语言偏好到 SharedPreferences
 *
 * 支持语言：中文（默认）、English、Русский、Português
 */
object LocaleHelper {

    private const val SP_NAME = "locale_prefs"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    /** 默认语言标签（中文） */
    const val DEFAULT_LANGUAGE_TAG = "zh-CN"

    /** 支持的语言列表 */
    val SUPPORTED_LOCALES = listOf(
        LanguageOption("zh-CN", "中文（简体）"),
        LanguageOption("en", "English"),
        LanguageOption("ru", "Русский"),
        LanguageOption("pt", "Português"),
    )

    /**
     * 在 Application#attachBaseContext 中调用，返回被包装过的 Context
     *
     * 这会确保所有 Activity 在启动时都使用用户保存的语言设置
     */
    fun wrapContext(base: Context): Context {
        val savedTag = getSavedLanguageTag(base)
        // 加载翻译覆盖：filesDir（用户导入）优先，assets（APK内置）兜底
        TranslationOverlay.init(base)
        return applyLocaleToContext(base, savedTag)
    }

    /**
     * 切换 App 语言，会触发已有 Activity 的重建
     *
     * @param context 任意 Context，用于持久化存储
     * @param languageTag BCP 47 语言标签，如 "en"、"zh-CN"
     */
    fun setLanguage(context: Context, languageTag: String) {
        saveLanguageTag(context, languageTag)
        // AppCompatDelegate.setApplicationLocales 会触发 Activity#recreate()
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageTag)
        )
    }

    /**
     * 获取当前保存的语言标签
     */
    fun getCurrentLanguageTag(context: Context): String {
        return getSavedLanguageTag(context)
    }

    /**
     * 获取当前语言对应的 Locale 对象
     */
    fun getCurrentLocale(context: Context): Locale {
        return Locale.forLanguageTag(getCurrentLanguageTag(context))
    }

    /**
     * 根据语言标签查找对应的 LanguageOption
     */
    fun findOptionByTag(tag: String): LanguageOption {
        return SUPPORTED_LOCALES.firstOrNull { it.tag == tag }
            ?: SUPPORTED_LOCALES.first()
    }

    // ─── 内部实现 ───────────────────────────────────

    private fun saveLanguageTag(context: Context, tag: String) {
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, tag)
            .apply()
    }

    private fun getSavedLanguageTag(context: Context): String {
        val prefs = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE_TAG, null) ?: DEFAULT_LANGUAGE_TAG
    }

    /**
     * 根据语言标签创建包装后的 Context
     *
     * Config 级 Locale 变更 + Locale.setDefault 双保险
     */
    private fun applyLocaleToContext(context: Context, languageTag: String): Context {
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        return context.createConfigurationContext(config)
    }

    // ─── 数据类 ─────────────────────────────────────

    /**
     * 语言选项
     *
     * @param tag BCP 47 语言标签
     * @param displayName 本地化显示名称
     */
    data class LanguageOption(
        val tag: String,
        val displayName: String,
    )
}
