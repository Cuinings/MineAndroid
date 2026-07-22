package com.cn.core.ui.locale

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.appcompat.app.AlertDialog

/**
 * 语言选择对话框
 *
 * 包含：
 * 1. "导入翻译文件"按钮 — 从本地选择 JSON 文件更新翻译
 * 2. 语言列表 — 切换 App 语言
 * 3. 每条语言选项会显示是否已导入自定义翻译（[已导入]）
 *
 * 用法：
 * ```
 * // 在 Activity 中：
 * private val translationImporter = TranslationImporter.register(this)
 *
 * // 显示对话框：
 * LanguagePickerDialog.show(this, translationImporter) { option ->
 *     // 语言切换后的回调（可选）
 * }
 * ```
 */
object LanguagePickerDialog {

    fun show(
        activity: Activity,
        importer: TranslationImporter? = null,
        onSelected: ((LocaleHelper.LanguageOption) -> Unit)? = null,
    ) {
        val options = LocaleHelper.SUPPORTED_LOCALES
        val currentTag = LocaleHelper.getCurrentLanguageTag(activity)

        // 构建显示名称，标记已导入翻译的语言
        val checkedIndex = options.indexOfFirst { it.tag == currentTag }.coerceAtLeast(0)
        val displayNames = options.map { option ->
            if (TranslationOverlay.hasTranslation(option.tag)) {
                val label = "${option.displayName}  [已导入]"
                SpannableString(label).apply {
                    setSpan(StyleSpan(Typeface.ITALIC), label.length - 4, label.length, 0)
                }
            } else {
                option.displayName
            }
        }.toTypedArray<CharSequence>()

        AlertDialog.Builder(activity)
            .setTitle(getTitleText(activity))
            // 导入按钮（正下方）
            .setPositiveButton(getImportText(activity)) { _, _ ->
                importer?.pickFile()
            }
            // 语言列表
            .setSingleChoiceItems(displayNames, checkedIndex) { dialog: DialogInterface, which: Int ->
                val selected = options[which]
                if (selected.tag != currentTag) {
                    LocaleHelper.setLanguage(activity, selected.tag)
                    onSelected?.invoke(selected)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getCancelText(activity), null)
            .show()
    }

    private fun getTitleText(context: Context): String {
        return getLocalizedText(
            context,
            zh = "选择语言",
            en = "Select Language",
            ru = "Выберите язык",
            pt = "Selecionar idioma"
        )
    }

    private fun getImportText(context: Context): String {
        return getLocalizedText(
            context,
            zh = "📂 导入翻译文件",
            en = "📂 Import Translation",
            ru = "📂 Импорт перевода",
            pt = "📂 Importar tradução"
        )
    }

    private fun getCancelText(context: Context): String {
        return getLocalizedText(
            context,
            zh = "取消",
            en = "Cancel",
            ru = "Отмена",
            pt = "Cancelar"
        )
    }

    private fun getLocalizedText(context: Context, zh: String, en: String, ru: String, pt: String): String {
        val tag = LocaleHelper.getCurrentLanguageTag(context)
        return when (tag) {
            "zh-CN" -> zh
            "en" -> en
            "ru" -> ru
            "pt" -> pt
            else -> en
        }
    }
}
