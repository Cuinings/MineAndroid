package com.cn.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * SharedPreferences工具类
 */
object SPUtil {

    private const val DEFAULT_NAME = "app_sp"

    /**
     * 获取SharedPreferences实例
     */
    private fun getSp(context: Context, name: String = DEFAULT_NAME): SharedPreferences {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    /**
     * 存储字符串
     */
    fun putString(context: Context, key: String, value: String, name: String = DEFAULT_NAME) {
        getSp(context, name).edit { putString(key, value) }
    }

    /**
     * 获取字符串
     */
    fun getString(context: Context, key: String, defaultValue: String = "", name: String = DEFAULT_NAME): String {
        return getSp(context, name).getString(key, defaultValue) ?: defaultValue
    }

    /**
     * 存储整数
     */
    fun putInt(context: Context, key: String, value: Int, name: String = DEFAULT_NAME) {
        getSp(context, name).edit { putInt(key, value) }
    }

    /**
     * 获取整数
     */
    fun getInt(context: Context, key: String, defaultValue: Int = 0, name: String = DEFAULT_NAME): Int {
        return getSp(context, name).getInt(key, defaultValue)
    }

    /**
     * 存储长整数
     */
    fun putLong(context: Context, key: String, value: Long, name: String = DEFAULT_NAME) {
        getSp(context, name).edit { putLong(key, value) }
    }

    /**
     * 获取长整数
     */
    fun getLong(context: Context, key: String, defaultValue: Long = 0L, name: String = DEFAULT_NAME): Long {
        return getSp(context, name).getLong(key, defaultValue)
    }

    /**
     * 存储浮点数
     */
    fun putFloat(context: Context, key: String, value: Float, name: String = DEFAULT_NAME) {
        getSp(context, name).edit { putFloat(key, value) }
    }

    /**
     * 获取浮点数
     */
    fun getFloat(context: Context, key: String, defaultValue: Float = 0f, name: String = DEFAULT_NAME): Float {
        return getSp(context, name).getFloat(key, defaultValue)
    }

    /**
     * 存储布尔值
     */
    fun putBoolean(context: Context, key: String, value: Boolean, name: String = DEFAULT_NAME) {
        getSp(context, name).edit { putBoolean(key, value) }
    }

    /**
     * 获取布尔值
     */
    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false, name: String = DEFAULT_NAME): Boolean {
        return getSp(context, name).getBoolean(key, defaultValue)
    }

    /**
     * 存储字符串集合
     */
    fun putStringSet(context: Context, key: String, value: Set<String>, name: String = DEFAULT_NAME) {
        getSp(context, name).edit { putStringSet(key, value) }
    }

    /**
     * 获取字符串集合
     */
    fun getStringSet(context: Context, key: String, defaultValue: Set<String> = emptySet(), name: String = DEFAULT_NAME): Set<String> {
        return getSp(context, name).getStringSet(key, defaultValue) ?: defaultValue
    }

    /**
     * 移除指定键值对
     */
    fun remove(context: Context, key: String, name: String = DEFAULT_NAME) {
        getSp(context, name).edit { remove(key) }
    }

    /**
     * 清除所有数据
     */
    fun clear(context: Context, name: String = DEFAULT_NAME) {
        getSp(context, name).edit { clear() }
    }

    /**
     * 检查是否包含指定键
     */
    fun contains(context: Context, key: String, name: String = DEFAULT_NAME): Boolean {
        return getSp(context, name).contains(key)
    }

    /**
     * 获取所有键值对
     */
    fun getAll(context: Context, name: String = DEFAULT_NAME): Map<String, *> {
        return getSp(context, name).all
    }
}