package com.cn.core.task

/**
 * 任务状态
 * 
 * 表示任务在生命周期中的不同状态，用于监控任务执行进度。
 * 
 * 状态流转：
 * - Pending -> Running -> Completed
 * - 任意状态 -> Cancelled（取消时）
 * 
 * 使用示例：
 * ```kotlin
 * task.onStateChange { state ->
 *     when (state) {
 *         is TaskState.Pending -> showWaiting(state.queuePosition)
 *         is TaskState.Running -> showProgress(state.progress)
 *         is TaskState.Completed -> hideProgress()
 *         is TaskState.Cancelled -> showCancelled()
 *     }
 * }
 * ```
 * 
 * @see Task
 * @see TaskCallback
 */
sealed class TaskState {
    /**
     * 等待中状态
     * 
     * 任务已加入队列，等待执行。
     * 
     * @param queuePosition 队列中的位置（从1开始），-1表示未知
     * @param queueSize 队列总任务数
     */
    data class Pending(
        val queuePosition: Int = -1,
        val queueSize: Int = 0
    ) : TaskState()

    /**
     * 执行中状态
     * 
     * 任务正在执行，可通过进度回调获取执行进度。
     * 
     * @param progress 执行进度，0.0 ~ 1.0
     * @param message 进度消息，可选
     */
    data class Running(
        val progress: Float = 0f,
        val message: String? = null
    ) : TaskState()

    /**
     * 已完成状态
     * 
     * 任务执行完成，可能是成功或失败。
     * 
     * @param success 是否成功
     * @param error 失败时的异常信息
     */
    data class Completed(
        val success: Boolean,
        val error: Throwable? = null
    ) : TaskState()

    /**
     * 已取消状态
     * 
     * 任务被取消执行。
     */
    data object Cancelled : TaskState()
}

/**
 * 任务进度更新器
 * 
 * 用于在任务执行过程中报告进度更新。
 * 在任务 block 中通过参数接收此接口实例。
 * 
 * 使用示例：
 * ```kotlin
 * val task = task<Unit> {
 *     block { progress ->
 *         for (i in 0..100) {
 *             progress.report(i / 100f, "Processing $i%")
 *         }
 *     }
 * }
 * ```
 */
interface ProgressReporter {
    /**
     * 报告进度更新
     * 
     * @param progress 进度值，范围 0.0 ~ 1.0
     * @param message 进度消息，可选
     */
    fun report(progress: Float, message: String? = null)
}

/**
 * 任务回调接口
 * 
 * 统一的任务回调接口，包含状态变化、成功、失败等回调。
 * 
 * @param T 任务返回值类型
 */
interface TaskCallback<T> {
    /**
     * 状态变化回调
     * 
     * @param state 新的任务状态
     */
    fun onStateChanged(state: TaskState) {}

    /**
     * 成功回调
     * 
     * @param result 任务执行结果
     */
    fun onSuccess(result: T) {}

    /**
     * 失败回调
     * 
     * @param error 异常信息
     */
    fun onError(error: Throwable) {}

    /**
     * 完成回调
     * 
     * 无论成功或失败都会调用。
     */
    fun onComplete() {}
}

/**
 * 任务回调适配器
 * 
 * 提供 [TaskCallback] 的空实现，方便子类选择性重写。
 */
open class TaskCallbackAdapter<T> : TaskCallback<T>
