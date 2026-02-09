package com.cn.core.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object Debounce {

    private var defaultCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private val jobMap = ConcurrentHashMap<Any, Job>()
    private val mutex = Mutex()

    /**
     * 设置默认的CoroutineScope
     * @param scope 要设置的CoroutineScope
     */
    fun setDefaultCoroutineScope(scope: CoroutineScope) {
        defaultCoroutineScope = scope
    }

    /**
     * 获取当前默认的CoroutineScope
     * @return 当前默认的CoroutineScope
     */
    fun getDefaultCoroutineScope(): CoroutineScope {
        return defaultCoroutineScope
    }

    /**
     * 使用协程实现的debounce功能
     * @param duration 延迟时间，单位毫秒
     * @param task 要执行的任务
     */
    fun debounce(duration: Long = 500L, task: () -> Unit) {
        debounce(duration, task, defaultCoroutineScope)
    }

    /**
     * 使用协程实现的debounce功能（兼容旧API）
     * @param duration 延迟时间，单位毫秒
     * @param task 要执行的任务
     * @param timer 保留参数，确保API兼容性
     */
    fun debounce(duration: Long = 500L, task: () -> Unit, timer: Any) {
        // 忽略timer参数，使用协程实现
        debounce(defaultCoroutineScope, duration, task)
    }
    
    /**
     * 使用协程实现的debounce功能
     * @param scope 要使用的CoroutineScope，默认使用全局CoroutineScope
     * @param duration 延迟时间，单位毫秒
     * @param task 要执行的任务
     */
    fun debounce(scope: CoroutineScope = defaultCoroutineScope, duration: Long = 500L, task: () -> Unit) {
        // 使用任务对象本身作为键，确保唯一性
        val key = task
        scope.launch {
            mutex.withLock {
                // 取消之前的任务
                jobMap[key]?.cancel(CancellationException("New debounce task for key: $key"))

                // 创建新任务
                val job = scope.launch {
                    try {
                        delay(duration)
                        task.invoke()
                    } finally {
                        // 无论任务是正常执行还是被取消，都从jobMap中移除
                        jobMap.remove(key)
                    }
                }

                jobMap[key] = job
            }
        }
    }

    /**
     * 取消指定任务的debounce
     * @param task 要取消的任务
     */
    fun cancel(task: () -> Unit) {
        val key = task
        jobMap[key]?.cancel()
        jobMap.remove(key)
    }

    /**
     * 取消所有debounce任务
     */
    fun cancelAll() {
        jobMap.values.forEach { it.cancel() }
        jobMap.clear()
    }
}