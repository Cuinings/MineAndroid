package com.cn.tosetting

class SettingBuilder {
    private val settings = mutableListOf<Setting>()
    private var filter: ((Setting) -> Boolean)? = null
    private var categories = mutableSetOf<SettingCategory>()

    fun add(setting: Setting): SettingBuilder {
        settings.add(setting)
        return this
    }

    fun addAll(vararg settings: Setting): SettingBuilder {
        this.settings.addAll(settings)
        return this
    }

    fun category(category: SettingCategory): SettingBuilder {
        categories.add(category)
        return this
    }

    fun categories(vararg categories: SettingCategory): SettingBuilder {
        this.categories.addAll(categories)
        return this
    }

    fun filter(predicate: (Setting) -> Boolean): SettingBuilder {
        filter = predicate
        return this
    }

    fun brandSpecific(): SettingBuilder {
        filter = { it.requiresBrandSpecific }
        return this
    }

    fun build(): List<Setting> {
        val result = if (settings.isNotEmpty()) {
            settings.toList()
        } else if (categories.isNotEmpty()) {
            Setting.all.filter { it.category in categories }
        } else {
            Setting.all
        }
        return filter?.let { result.filter(it) } ?: result
    }
}

fun settings(block: SettingBuilder.() -> Unit): List<Setting> {
    return SettingBuilder().apply(block).build()
}

fun settingsByCategory(vararg categories: SettingCategory): List<Setting> {
    return SettingBuilder().categories(*categories).build()
}

fun brandSpecificSettings(): List<Setting> {
    return SettingBuilder().brandSpecific().build()
}
