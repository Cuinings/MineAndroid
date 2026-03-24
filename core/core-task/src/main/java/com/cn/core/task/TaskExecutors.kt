package com.cn.core.task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 任务执行器
 * 
 * 提供常用的协程调度器封装和便捷的线程切换方法。
 * 简化在不同线程上执行任务的代码编写。
 * 
 * 包含三种主要调度器：
 * - [io]: IO 调度器，适用于网络请求、文件读写等 IO 密集型操作
 * - [computation]: 计算调度器，适用于 CPU 密集型计算任务
 * - [main]: 主线程调度器，适用于 UI 更新操作
 * 
 * 使用示例：
 * ```kotlin
 * // 在 IO 线程执行网络请求
 * val data = TaskExecutors.runOnIO { 
 *     apiService.fetchData() 
 * }
 * 
 * // 在计算线程执行数据处理
 * val result = TaskExecutors.runOnComputation { 
 *     processData(data) 
 * }
 * 
 * // 在主线程更新 UI
 * TaskExecutors.runOnMain { 
 *     updateUI(result) 
 * }
 * ```
 * 
 * @see Dispatchers
 */
object TaskExecutors {
    /**
     * IO 调度器
     * 
     * 适用于 IO 密集型操作，如：
     * - 网络请求
     * - 文件读写
     * - 数据库操作
     * - Socket 通信
     * 
     * 该调度器会根据需要创建额外的线程，适合执行阻塞操作。
     */
    val io: CoroutineDispatcher = Dispatchers.IO

    /**
     * 计算调度器
     * 
     * 适用于 CPU 密集型操作，如：
     * - 数据处理
     * - 图像处理
     * - 复杂计算
     * - 排序和搜索
     * 
     * 该调度器使用固定数量的线程（通常等于 CPU 核心数）。
     */
    val computation: CoroutineDispatcher = Dispatchers.Default

    /**
     * 主线程调度器
     * 
     * 适用于 UI 相关操作，如：
     * - 更新 UI 组件
     * - 显示 Toast/Snackbar
     * - 启动 Activity/Fragment
     * 
     * 注意：在非 Android 环境中可能不可用。
     */
    val main: CoroutineDispatcher = Dispatchers.Main

    /**
     * 在 IO 线程执行任务
     * 
     * 切换到 [io] 调度器执行指定的挂起函数。
     * 适用于网络请求、文件读写等 IO 操作。
     * 
     * @param T 返回值类型
     * @param block 要执行的挂起函数
     * @return 函数执行结果
     * 
     * 使用示例：
     * ```kotlin
     * val data = TaskExecutors.runOnIO { 
     *     withContext(Dispatchers.IO) {
     *         // 网络请求或文件操作
     *         fetchDataFromNetwork()
     *     }
     * }
     * ```
     */
    suspend fun <T> runOnIO(block: suspend () -> T): T {
        return withContext(io) { block() }
    }

    /**
     * 在计算线程执行任务
     * 
     * 切换到 [computation] 调度器执行指定的挂起函数。
     * 适用于数据处理、图像处理等 CPU 密集型操作。
     * 
     * @param T 返回值类型
     * @param block 要执行的挂起函数
     * @return 函数执行结果
     * 
     * 使用示例：
     * ```kotlin
     * val result = TaskExecutors.runOnComputation { 
     *     // 复杂计算
     *     processLargeDataSet(data)
     * }
     * ```
     */
    suspend fun <T> runOnComputation(block: suspend () -> T): T {
        return withContext(computation) { block() }
    }

    /**
     * 在主线程执行任务
     * 
     * 切换到 [main] 调度器执行指定的挂起函数。
     * 适用于 UI 更新操作。
     * 
     * @param T 返回值类型
     * @param block 要执行的挂起函数
     * @return 函数执行结果
     * 
     * 使用示例：
     * ```kotlin
     * TaskExecutors.runOnMain { 
     *     // 更新 UI
     *     textView.text = result
     * }
     * ```
     */
    suspend fun <T> runOnMain(block: suspend () -> T): T {
        return withContext(main) { block() }
    }

    /**
     * 在指定调度器执行任务
     * 
     * 切换到指定的调度器执行挂起函数。
     * 提供更灵活的线程控制。
     * 
     * @param T 返回值类型
     * @param dispatcher 目标调度器
     * @param block 要执行的挂起函数
     * @return 函数执行结果
     * 
     * 使用示例：
     * ```kotlin
     * val result = TaskExecutors.runOnDispatcher(
     *     dispatcher = Dispatchers.Unconfined
     * ) {
     *     // 在指定调度器执行
     *     doSomething()
     * }
     * ```
     */
    suspend fun <T> runOnDispatcher(
        dispatcher: CoroutineDispatcher,
        block: suspend () -> T
    ): T {
        return withContext(dispatcher) { block() }
    }
}
