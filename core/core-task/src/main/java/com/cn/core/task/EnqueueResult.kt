package com.cn.core.task

/**
 * 入队结果
 * 
 * 表示任务入队操作的结果，用于处理背压场景。
 * 
 * 使用示例：
 * ```kotlin
 * when (val result = queue.enqueueWithResult(task)) {
 *     is EnqueueResult.Success -> {
 *         println("任务已入队，ID: ${result.task.id}")
 *     }
 *     is EnqueueResult.Rejected -> {
 *         println("任务被拒绝，原因: ${result.reason}")
 *         // 处理拒绝逻辑
 *     }
 *     is EnqueueResult.Discarded -> {
 *         println("任务被丢弃，被丢弃的任务: ${result.discardedTask.name}")
 *     }
 *     is EnqueueResult.CallerExecuted -> {
 *         println("任务已在调用者线程执行")
 *     }
 * }
 * ```
 * 
 * @see RejectionPolicy
 * @see SerialTaskQueue
 * @see ConcurrentTaskQueue
 */
sealed class EnqueueResult {
    
    /**
     * 入队成功
     * 
     * @param task 成功入队的任务
     */
    data class Success(val task: Task<*>) : EnqueueResult()
    
    /**
     * 任务被拒绝
     * 
     * 当使用 [RejectionPolicy.THROW_EXCEPTION] 或 [RejectionPolicy.DISCARD] 策略时返回。
     * 
     * @param task 被拒绝的任务
     * @param reason 拒绝原因
     * @param queueSize 当前队列大小
     * @param maxCapacity 队列最大容量
     */
    data class Rejected(
        val task: Task<*>,
        val reason: String,
        val queueSize: Int,
        val maxCapacity: Int
    ) : EnqueueResult()
    
    /**
     * 任务入队成功，但丢弃了旧任务
     * 
     * 当使用 [RejectionPolicy.DISCARD_OLDEST] 策略时返回。
     * 
     * @param task 新入队的任务
     * @param discardedTask 被丢弃的旧任务
     */
    data class Discarded(
        val task: Task<*>,
        val discardedTask: Task<*>
    ) : EnqueueResult()
    
    /**
     * 任务已在调用者线程执行
     * 
     * 当使用 [RejectionPolicy.CALLER_RUNS] 策略时返回。
     * 
     * @param task 执行的任务
     * @param success 是否执行成功
     * @param error 执行错误（如果失败）
     */
    data class CallerExecuted(
        val task: Task<*>,
        val success: Boolean,
        val error: Throwable? = null
    ) : EnqueueResult()
    
    /**
     * 判断入队是否成功
     * 
     * @return 如果任务成功入队或执行则返回 true
     */
    val isSuccess: Boolean
        get() = this is Success || this is CallerExecuted
    
    /**
     * 判断任务是否被拒绝
     * 
     * @return 如果任务被拒绝或丢弃则返回 true
     */
    val isRejected: Boolean
        get() = this is Rejected
}
