package com.cn.mine.wan.android.events

import androidx.annotation.IntRange

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 20:07
 * @Description:事件结果
 */
sealed class EventResult<out T> {
    data class Success<T>(val result: T?): EventResult<T>()
    data class Failed(val e: Throwable?): EventResult<Nothing>()
    data class Loading(@IntRange(from = 0, to = 100) val index: Int): EventResult<Nothing>()
}