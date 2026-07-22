package com.cn.core.ui.locale

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONObject

/**
 * 翻译文件导入器
 *
 * 通过系统文件选择器（SAF）让用户选择 JSON 翻译文件，
 * 解析后缓存到本地，并触发 UI 刷新。
 *
 * 用法：
 * ```
 * // 在 Activity 的 onCreate 之前注册：
 * private val importer = TranslationImporter.register(this)
 *
 * // 点击按钮时打开选择器：
 * importer.pickFile()
 * ```
 */
class TranslationImporter private constructor(
    private val launcher: ActivityResultLauncher<Array<String>>,
) {

    companion object {
        /**
         * 创建并注册 TranslationImporter
         * 必须在 Activity#onCreate 之前调用
         *
         * 注意：必须传入 [ComponentActivity]（如 AppCompatActivity），
         * 因为只有它持有 [androidx.activity.result.ActivityResultRegistry]。
         */
        fun register(activity: ComponentActivity): TranslationImporter {
            val launcher = activity.activityResultRegistry.register(
                "translation_import_${activity.hashCode()}",
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let { handleFileSelected(activity, it.toString()) }
            }
            return TranslationImporter(launcher)
        }

        // ─── 内部逻辑（无需实例状态，作为伴生对象函数） ──────────

        private fun handleFileSelected(activity: Activity, uriString: String) {
            try {
                val uri = Uri.parse(uriString)
                val inputStream = activity.contentResolver.openInputStream(uri)
                    ?: throw Exception("无法打开文件")

                val content = inputStream.bufferedReader(Charsets.UTF_8).readText()
                inputStream.close()

                // 验证 JSON
                val json = JSONObject(content)

                // 推断语言标签
                val fileName = getFileName(activity, uri)
                val langTag = inferLanguageTag(fileName)

                // 转为 Map
                val strings = mutableMapOf<String, String>()
                json.keys().forEach { key -> strings[key] = json.getString(key) }

                if (strings.isEmpty()) {
                    Toast.makeText(activity, "翻译文件内容为空", Toast.LENGTH_SHORT).show()
                    return
                }

                // 应用翻译
                TranslationOverlay.applyTranslation(activity, langTag, strings)

                // 如果当前正好是该语言，刷新 UI
                val currentLang = LocaleHelper.getCurrentLanguageTag(activity)
                if (currentLang == langTag) {
                    // 通过切换语言来触发 Activity 重建
                    LocaleHelper.setLanguage(activity, langTag)
                } else {
                    Toast.makeText(
                        activity,
                        "已导入 ${langTag} 翻译（${strings.size} 条），切换到该语言后生效",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    activity,
                    "导入失败：${e.localizedMessage ?: "文件格式不正确"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        /**
         * 从 URI 推断文件名
         */
        private fun getFileName(context: android.content.Context, uri: Uri): String {
            var name = "unknown.json"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )
                    if (displayNameIndex >= 0) {
                        name = cursor.getString(displayNameIndex) ?: name
                    }
                }
            }
            return name
        }

        /**
         * 从文件名推断 BCP 47 语言标签
         *
         * 匹配规则：
         *   en.json       → "en"
         *   zh-CN.json    → "zh-CN"
         *   english.json  → "en"
         *   translations_en.json → "en"
         */
        private fun inferLanguageTag(fileName: String): String {
            val nameWithoutExt = fileName.substringBeforeLast(".")

            // 精确匹配：文件名就是语言标签
            val knownTags = LocaleHelper.SUPPORTED_LOCALES.map { it.tag }.toSet()
            if (nameWithoutExt in knownTags) {
                return nameWithoutExt
            }

            // 子串匹配：zh-CN、en、ru、pt 出现在文件名中
            for (tag in knownTags) {
                if (nameWithoutExt.contains(tag, ignoreCase = true)) {
                    return tag
                }
            }

            // 模糊匹配：english → en, russian → ru, portuguese → pt
            return when {
                nameWithoutExt.contains("english", ignoreCase = true) -> "en"
                nameWithoutExt.contains("russian", ignoreCase = true) || nameWithoutExt.contains("русск", ignoreCase = true) -> "ru"
                nameWithoutExt.contains("portug", ignoreCase = true) || nameWithoutExt.contains("portugu", ignoreCase = true) -> "pt"
                nameWithoutExt.contains("chinese", ignoreCase = true) || nameWithoutExt.contains("中文", ignoreCase = true) -> "zh-CN"
                else -> {
                    // 最后尝试取后缀：translations_en → en
                    val lastSegment = nameWithoutExt.split("_", "-").lastOrNull() ?: return "en"
                    if (lastSegment.length in 2..5 && lastSegment in knownTags) lastSegment else "en"
                }
            }
        }
    }

    /**
     * 弹出系统文件选择器，限选 JSON 文件
     */
    fun pickFile() {
        launcher.launch(arrayOf("application/json", "*/*"))
    }
}
