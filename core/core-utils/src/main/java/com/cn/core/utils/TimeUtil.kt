package com.cn.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @author: cn
 * @time: 2026/2/10 15:00
 * @history
 * @description: 时间工具类，用于时间格式转换
 */
object TimeUtil {

    // 时间格式常量
    const val FORMAT_YYYY_MM_DD = "yyyy-MM-dd"
    const val FORMAT_YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm"
    const val FORMAT_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss"
    const val FORMAT_HH_MM = "HH:mm"
    const val FORMAT_HH_MM_SS = "HH:mm:ss"

    /**
     * 将时间戳转换为指定格式的字符串
     * @param timestamp 时间戳（毫秒）
     * @param format 时间格式
     * @return 格式化后的时间字符串
     */
    fun timestampToString(timestamp: Long, format: String = FORMAT_YYYY_MM_DD_HH_MM_SS): String {
        return try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 将时间字符串转换为时间戳
     * @param timeStr 时间字符串
     * @param format 时间格式
     * @return 时间戳（毫秒），转换失败返回-1
     */
    fun stringToTimestamp(timeStr: String, format: String = FORMAT_YYYY_MM_DD_HH_MM_SS): Long {
        return try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.parse(timeStr)?.time ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 获取当前时间戳
     * @return 当前时间戳（毫秒）
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    /**
     * 获取当前时间字符串
     * @param format 时间格式
     * @return 当前时间字符串
     */
    fun getCurrentTimeString(format: String = FORMAT_YYYY_MM_DD_HH_MM_SS): String {
        return timestampToString(getCurrentTimestamp(), format)
    }

    /**
     * 格式化时间差，返回友好的时间差描述
     * @param timestamp 时间戳（毫秒）
     * @return 友好的时间差描述，如"刚刚"、"5分钟前"、"1小时前"等
     */
    fun formatTimeDiff(timestamp: Long): String {
        val now = getCurrentTimestamp()
        val diff = now - timestamp

        return when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${(diff / (60 * 1000)).toInt()}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${(diff / (60 * 60 * 1000)).toInt()}小时前"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${(diff / (24 * 60 * 60 * 1000)).toInt()}天前"
            else -> timestampToString(timestamp, FORMAT_YYYY_MM_DD)
        }
    }

    /**
     * 格式化持续时间，返回友好的持续时间描述
     * @param duration 持续时间（毫秒）
     * @return 友好的持续时间描述，如"5秒"、"3分钟"、"2小时"等
     */
    fun formatDuration(duration: Long): String {
        return when {
            duration < 1000 -> "${duration}毫秒"
            duration < 60 * 1000 -> "${(duration / 1000).toInt()}秒"
            duration < 60 * 60 * 1000 -> "${(duration / (60 * 1000)).toInt()}分钟"
            duration < 24 * 60 * 60 * 1000 -> "${(duration / (60 * 60 * 1000)).toInt()}小时"
            else -> "${(duration / (24 * 60 * 60 * 1000)).toInt()}天"
        }
    }

    /**
     * 获取指定时间的年份
     * @param timestamp 时间戳（毫秒）
     * @return 年份
     */
    fun getYear(timestamp: Long): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy", Locale.getDefault())
            sdf.format(Date(timestamp)).toInt()
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 获取指定时间的月份（1-12）
     * @param timestamp 时间戳（毫秒）
     * @return 月份
     */
    fun getMonth(timestamp: Long): Int {
        return try {
            val sdf = SimpleDateFormat("MM", Locale.getDefault())
            sdf.format(Date(timestamp)).toInt()
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 获取指定时间的日期（1-31）
     * @param timestamp 时间戳（毫秒）
     * @return 日期
     */
    fun getDay(timestamp: Long): Int {
        return try {
            val sdf = SimpleDateFormat("dd", Locale.getDefault())
            sdf.format(Date(timestamp)).toInt()
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 获取指定时间的小时（0-23）
     * @param timestamp 时间戳（毫秒）
     * @return 小时
     */
    fun getHour(timestamp: Long): Int {
        return try {
            val sdf = SimpleDateFormat("HH", Locale.getDefault())
            sdf.format(Date(timestamp)).toInt()
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 获取指定时间的分钟（0-59）
     * @param timestamp 时间戳（毫秒）
     * @return 分钟
     */
    fun getMinute(timestamp: Long): Int {
        return try {
            val sdf = SimpleDateFormat("mm", Locale.getDefault())
            sdf.format(Date(timestamp)).toInt()
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 获取指定时间的秒钟（0-59）
     * @param timestamp 时间戳（毫秒）
     * @return 秒钟
     */
    fun getSecond(timestamp: Long): Int {
        return try {
            val sdf = SimpleDateFormat("ss", Locale.getDefault())
            sdf.format(Date(timestamp)).toInt()
        } catch (e: Exception) {
            -1
        }
    }
}
