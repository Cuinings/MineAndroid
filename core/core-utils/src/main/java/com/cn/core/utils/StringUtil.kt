package com.cn.core.utils

/**
 * 字符串工具类
 */
object StringUtil {

    /**
     * 检查字符串是否为空
     */
    fun isEmpty(str: String?): Boolean {
        return str == null || str.isEmpty()
    }

    /**
     * 检查字符串是否为非空
     */
    fun isNotEmpty(str: String?): Boolean {
        return !isEmpty(str)
    }

    /**
     * 检查字符串是否为空白
     */
    fun isBlank(str: String?): Boolean {
        return str == null || str.isBlank()
    }

    /**
     * 检查字符串是否为非空白
     */
    fun isNotBlank(str: String?): Boolean {
        return !isBlank(str)
    }

    /**
     * 获取字符串长度
     */
    fun length(str: String?): Int {
        return str?.length ?: 0
    }

    /**
     * 字符串转整数
     */
    fun toInt(str: String?, defaultValue: Int = 0): Int {
        return try {
            str?.toInt() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * 字符串转长整数
     */
    fun toLong(str: String?, defaultValue: Long = 0L): Long {
        return try {
            str?.toLong() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * 字符串转浮点数
     */
    fun toFloat(str: String?, defaultValue: Float = 0f): Float {
        return try {
            str?.toFloat() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * 字符串转双精度浮点数
     */
    fun toDouble(str: String?, defaultValue: Double = 0.0): Double {
        return try {
            str?.toDouble() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * 字符串转布尔值
     */
    fun toBoolean(str: String?, defaultValue: Boolean = false): Boolean {
        return try {
            str?.toBoolean() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * 截取字符串
     */
    fun substring(str: String?, startIndex: Int, endIndex: Int = Int.MAX_VALUE): String {
        if (str == null) return ""
        val actualEndIndex = minOf(endIndex, str.length)
        return if (startIndex >= actualEndIndex) "" else str.substring(startIndex, actualEndIndex)
    }

    /**
     * 分割字符串
     */
    fun split(str: String?, delimiter: String): List<String> {
        return str?.split(delimiter) ?: emptyList()
    }

    /**
     * 连接字符串
     */
    fun join(collection: Collection<String>?, separator: String = ""): String {
        return collection?.joinToString(separator) ?: ""
    }

    /**
     * 格式化字符串
     */
    fun format(format: String, vararg args: Any?): String {
        return try {
            String.format(format, *args)
        } catch (e: Exception) {
            format
        }
    }

    /**
     * 移除字符串中的空白字符
     */
    fun trim(str: String?): String {
        return str?.trim() ?: ""
    }

    /**
     * 移除字符串开头的空白字符
     */
    fun trimStart(str: String?): String {
        return str?.trimStart() ?: ""
    }

    /**
     * 移除字符串结尾的空白字符
     */
    fun trimEnd(str: String?): String {
        return str?.trimEnd() ?: ""
    }

    /**
     * 替换字符串中的指定字符
     */
    fun replace(str: String?, oldChar: Char, newChar: Char): String {
        return str?.replace(oldChar, newChar) ?: ""
    }

    /**
     * 替换字符串中的指定子串
     */
    fun replace(str: String?, oldValue: String, newValue: String): String {
        return str?.replace(oldValue, newValue) ?: ""
    }

    /**
     * 忽略大小写比较字符串
     */
    fun equalsIgnoreCase(str1: String?, str2: String?): Boolean {
        return str1?.equals(str2, ignoreCase = true) ?: (str2 == null)
    }

    /**
     * 检查字符串是否包含指定子串
     */
    fun contains(str: String?, substring: String): Boolean {
        return str?.contains(substring) ?: false
    }

    /**
     * 检查字符串是否以指定前缀开头
     */
    fun startsWith(str: String?, prefix: String): Boolean {
        return str?.startsWith(prefix) ?: false
    }

    /**
     * 检查字符串是否以指定后缀结尾
     */
    fun endsWith(str: String?, suffix: String): Boolean {
        return str?.endsWith(suffix) ?: false
    }

    /**
     * 将字符串转换为大写
     */
    fun toUpperCase(str: String?): String {
        return str?.uppercase() ?: ""
    }

    /**
     * 将字符串转换为小写
     */
    fun toLowerCase(str: String?): String {
        return str?.lowercase() ?: ""
    }
}