package com.cn.core.ui.locale

import android.content.Context
import android.content.res.Resources
import org.json.JSONObject
import java.io.File

/**
 * 翻译覆盖层 — 运行时拦截字符串查找
 *
 * 优先级：用户导入 JSON ＞ APK 内置 JSON ＞ 内置 XML 资源
 *
 * 工作流程：
 *   1. App 启动 → loadFromFiles 加载 filesDir（用户导入的，最高优先级）
 *   2. 仍缺失的语言 → loadFromAssets 加载 APK 内置翻译
 *   3. 用户导入 → applyTranslation 存入内存 + 写入文件 → 刷新 UI
 *   4. getString   → 先查覆盖层 → 找不到用内置 XML
 */
object TranslationOverlay {

    private const val CACHE_DIR = "translations"
    private const val ASSETS_DIR = "translations"

    /** Map<语言标签, Map<资源名, 翻译值>> */
    private val cache = mutableMapOf<String, MutableMap<String, String>>()
    private var assetsLoaded = false

    // ─── 公开 API ─────────────────────────────────

    /**
     * 初始化入口：先从 filesDir 加载用户导入的翻译，再从 assets 补全内置翻译
     *
     * @return 成功加载的语言标签列表
     */
    fun init(context: Context): List<String> {
        val loaded = loadFromFiles(context)
        loadFromAssets(context)
        return loaded
    }

    /**
     * 从 filesDir 加载所有缓存的翻译到内存（用户导入的）
     */
    fun loadFromFiles(context: Context): List<String> {
        val loadedLangs = mutableListOf<String>()
        val dir = File(context.filesDir, CACHE_DIR)
        if (!dir.isDirectory) return loadedLangs

        dir.listFiles { file -> file.extension == "json" }?.forEach { file ->
            val langTag = file.nameWithoutExtension
            try {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                val map = mutableMapOf<String, String>()
                json.keys().forEach { key -> map[key] = json.getString(key) }
                cache[langTag] = map
                loadedLangs.add(langTag)
            } catch (_: Exception) { }
        }
        return loadedLangs
    }

    /**
     * 从 APK assets/translations/ 加载内置翻译
     *
     * 仅在 filesDir 中没有对应语言时加载，即用户导入优先
     */
    fun loadFromAssets(context: Context) {
        if (assetsLoaded) return
        assetsLoaded = true

        try {
            val assetFiles = context.assets.list(ASSETS_DIR) ?: return
            for (fileName in assetFiles) {
                val langTag = fileName.substringBeforeLast(".")
                // 用户已导入的翻译不覆盖
                if (cache.containsKey(langTag)) continue

                try {
                    val inputStream = context.assets.open("$ASSETS_DIR/$fileName")
                    val content = inputStream.bufferedReader(Charsets.UTF_8).readText()
                    inputStream.close()

                    val json = JSONObject(content)
                    val map = mutableMapOf<String, String>()
                    json.keys().forEach { key -> map[key] = json.getString(key) }
                    if (map.isNotEmpty()) {
                        cache[langTag] = map
                    }
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    /**
     * 应用导入的翻译：存入内存 + 持久化到文件
     */
    fun applyTranslation(context: Context, langTag: String, strings: Map<String, String>) {
        // 内存
        cache[langTag] = strings.toMutableMap()

        // 文件持久化
        val dir = File(context.filesDir, CACHE_DIR)
        dir.mkdirs()
        val file = File(dir, "$langTag.json")
        val json = JSONObject(strings).toString(2)
        file.writeText(json, Charsets.UTF_8)
    }

    /**
     * 获取覆盖翻译，找不到返回 null
     *
     * @param resourceName  资源的名称（非 ID），如 "str_finish"
     * @param langTag       当前语言标签，如 "en"
     */
    fun getString(resourceName: String, langTag: String): String? {
        return cache[langTag]?.get(resourceName)
    }

    /**
     * 便捷方法：通过 resId 获取翻译
     */
    fun getString(resources: Resources, resId: Int, langTag: String): String? {
        return try {
            val name = resources.getResourceEntryName(resId)
            getString(name, langTag)
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    /**
     * 获取已加载的语言列表
     */
    fun getLoadedLanguages(): Set<String> = cache.keys.toSet()

    /**
     * 清除指定语言的覆盖翻译
     */
    fun clearTranslation(context: Context, langTag: String) {
        cache.remove(langTag)
        val file = File(context.filesDir, "$CACHE_DIR/$langTag.json")
        file.delete()
    }

    /**
     * 检查是否有指定语言的覆盖翻译
     */
    fun hasTranslation(langTag: String): Boolean {
        return cache.containsKey(langTag) && !cache[langTag].orEmpty().isEmpty()
    }
}
