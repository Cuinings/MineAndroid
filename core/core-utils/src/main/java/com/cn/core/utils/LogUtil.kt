package com.cn.core.utils

import android.util.Log

/**
 * 日志工具类
 */
object LogUtil {

    private const val TAG = "AppLog"
    private var isDebug = true

    /**
     * 设置是否为调试模式
     */
    fun setDebug(debug: Boolean) {
        isDebug = debug
    }

    /**
     * 获取是否为调试模式
     */
    fun isDebug(): Boolean {
        return isDebug
    }

    /**
     * 打印Verbose级别日志
     */
    fun v(tag: String = TAG, message: String) {
        if (isDebug) {
            Log.v(tag, message)
        }
    }

    /**
     * 打印Debug级别日志
     */
    fun d(tag: String = TAG, message: String) {
        if (isDebug) {
            Log.d(tag, message)
        }
    }

    /**
     * 打印Info级别日志
     */
    fun i(tag: String = TAG, message: String) {
        if (isDebug) {
            Log.i(tag, message)
        }
    }

    /**
     * 打印Warn级别日志
     */
    fun w(tag: String = TAG, message: String) {
        if (isDebug) {
            Log.w(tag, message)
        }
    }

    /**
     * 打印Error级别日志
     */
    fun e(tag: String = TAG, message: String) {
        if (isDebug) {
            Log.e(tag, message)
        }
    }

    /**
     * 打印Error级别日志（带异常）
     */
    fun e(tag: String = TAG, message: String, throwable: Throwable) {
        if (isDebug) {
            Log.e(tag, message, throwable)
        }
    }

    /**
     * 打印长日志（分段打印）
     */
    fun logLong(tag: String = TAG, message: String) {
        if (!isDebug) return
        val maxLength = 4000
        val length = message.length
        if (length <= maxLength) {
            Log.d(tag, message)
        } else {
            var start = 0
            var end = maxLength
            while (start < length) {
                if (end > length) {
                    end = length
                }
                Log.d(tag, message.substring(start, end))
                start = end
                end += maxLength
            }
        }
    }

    /**
     * 打印对象
     */
    fun logObject(tag: String = TAG, obj: Any?) {
        if (!isDebug) return
        when (obj) {
            null -> Log.d(tag, "null")
            is String -> Log.d(tag, obj)
            is Number -> Log.d(tag, obj.toString())
            is Boolean -> Log.d(tag, obj.toString())
            is Char -> Log.d(tag, obj.toString())
            is Array<*> -> Log.d(tag, obj.contentToString())
            is Collection<*> -> Log.d(tag, obj.toString())
            is Map<*, *> -> Log.d(tag, obj.toString())
            else -> Log.d(tag, obj.toString())
        }
    }

    /**
     * 打印JSON字符串
     */
    fun logJson(tag: String = TAG, json: String) {
        if (!isDebug) return
        try {
            val jsonIndent = json.toJsonIndent()
            Log.d(tag, jsonIndent)
        } catch (e: Exception) {
            Log.e(tag, "Invalid JSON: $json", e)
        }
    }

    /**
     * 将JSON字符串格式化
     */
    private fun String.toJsonIndent(): String {
        val sb = StringBuilder()
        var indentLevel = 0
        var inString = false
        for (char in this) {
            when (char) {
                '{', '[' -> {
                    sb.append(char)
                    if (!inString) {
                        sb.append('\n')
                        indentLevel++
                        for (i in 0 until indentLevel) {
                            sb.append("  ")
                        }
                    }
                }
                '}', ']' -> {
                    if (!inString) {
                        sb.append('\n')
                        indentLevel--
                        for (i in 0 until indentLevel) {
                            sb.append("  ")
                        }
                    }
                    sb.append(char)
                }
                ',' -> {
                    sb.append(char)
                    if (!inString) {
                        sb.append('\n')
                        for (i in 0 until indentLevel) {
                            sb.append("  ")
                        }
                    }
                }
                '"' -> {
                    sb.append(char)
                    inString = !inString
                }
                else -> {
                    sb.append(char)
                }
            }
        }
        return sb.toString()
    }
}