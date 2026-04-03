package com.cn.core.task

/**
 * 背压配置
 * 
 * 用于配置任务队列的背压处理策略。
 * 
 * 使用示例：
 * ```kotlin
 * // 基本配置
 * val config = BackpressureConfig(
 *     maxCapacity = 100,
 *     policy = RejectionPolicy.DISCARD_OLDEST
 * )
 * 
 * // 带回调的配置
 * val config = BackpressureConfig(
 *     maxCapacity = 50,
 *     policy = RejectionPolicy.DISCARD,
 *     onRejected = { task, reason ->
 *         Log.w("TaskQueue", "任务被拒绝: ${task.name}, 原因: $reason")
 *     }
 * )
 * ```
 * 
 * @param maxCapacity 队列最大容量，默认 100。设置为 [UNBOUNDED] 表示无限制。
 * @param policy 拒绝策略，默认 [RejectionPolicy.DISCARD_OLDEST]
 * @param onRejected 任务被拒绝时的回调
 * @param onDiscarded 任务被丢弃时的回调（用于 DISCARD_OLDEST 策略）
 * @param warnThreshold 警告阈值百分比（0-100），当队列使用率达到此值时触发警告
 * @param onThresholdReached 队列使用率达到阈值时的回调
 * 
 * @see RejectionPolicy
 * @see SerialTaskQueue
 * @see ConcurrentTaskQueue
 */
data class BackpressureConfig(
    val maxCapacity: Int = DEFAULT_CAPACITY,
    val policy: RejectionPolicy = RejectionPolicy.DISCARD_OLDEST,
    val onRejected: ((task: Task<*>, reason: String) -> Unit)? = null,
    val onDiscarded: ((discardedTask: Task<*>, newTask: Task<*>) -> Unit)? = null,
    val warnThreshold: Int = DEFAULT_WARN_THRESHOLD,
    val onThresholdReached: ((currentSize: Int, maxCapacity: Int, usagePercent: Int) -> Unit)? = null
) {
    init {
        require(maxCapacity > 0 || maxCapacity == UNBOUNDED) {
            "maxCapacity must be positive or UNBOUNDED"
        }
        require(warnThreshold in 0..100) {
            "warnThreshold must be between 0 and 100"
        }
    }
    
    /**
     * 是否启用背压控制
     * 
     * 当 maxCapacity 为 UNBOUNDED 时，不启用背压控制。
     */
    val isBackpressureEnabled: Boolean
        get() = maxCapacity != UNBOUNDED
    
    /**
     * 检查队列使用率是否达到警告阈值
     * 
     * @param currentSize 当前队列大小
     * @return 是否达到警告阈值
     */
    fun isThresholdReached(currentSize: Int): Boolean {
        if (!isBackpressureEnabled) return false
        val usagePercent = (currentSize * 100) / maxCapacity
        return usagePercent >= warnThreshold
    }
    
    /**
     * 计算队列使用率百分比
     * 
     * @param currentSize 当前队列大小
     * @return 使用率百分比（0-100），无限制队列返回 0
     */
    fun calculateUsagePercent(currentSize: Int): Int {
        if (!isBackpressureEnabled) return 0
        return (currentSize * 100) / maxCapacity
    }
    
    companion object {
        /**
         * 无限制容量
         * 
         * 设置此值表示队列不限制大小，不启用背压控制。
         */
        const val UNBOUNDED = Int.MAX_VALUE
        
        /**
         * 默认队列容量
         */
        const val DEFAULT_CAPACITY = 100
        
        /**
         * 默认警告阈值（80%）
         */
        const val DEFAULT_WARN_THRESHOLD = 80
        
        /**
         * 创建无限制配置
         * 
         * 不启用背压控制，队列大小无限制。
         * 
         * @return 无限制的背压配置
         */
        fun unbounded(): BackpressureConfig {
            return BackpressureConfig(maxCapacity = UNBOUNDED)
        }
        
        /**
         * 创建丢弃策略配置
         * 
         * @param maxCapacity 最大容量
         * @return 丢弃策略的背压配置
         */
        fun discard(maxCapacity: Int = DEFAULT_CAPACITY): BackpressureConfig {
            return BackpressureConfig(
                maxCapacity = maxCapacity,
                policy = RejectionPolicy.DISCARD
            )
        }
        
        /**
         * 创建丢弃最旧策略配置
         * 
         * @param maxCapacity 最大容量
         * @return 丢弃最旧策略的背压配置
         */
        fun discardOldest(maxCapacity: Int = DEFAULT_CAPACITY): BackpressureConfig {
            return BackpressureConfig(
                maxCapacity = maxCapacity,
                policy = RejectionPolicy.DISCARD_OLDEST
            )
        }
        
        /**
         * 创建抛异常策略配置
         * 
         * @param maxCapacity 最大容量
         * @return 抛异常策略的背压配置
         */
        fun throwException(maxCapacity: Int = DEFAULT_CAPACITY): BackpressureConfig {
            return BackpressureConfig(
                maxCapacity = maxCapacity,
                policy = RejectionPolicy.THROW_EXCEPTION
            )
        }
        
        /**
         * 创建调用者执行策略配置
         * 
         * @param maxCapacity 最大容量
         * @return 调用者执行策略的背压配置
         */
        fun callerRuns(maxCapacity: Int = DEFAULT_CAPACITY): BackpressureConfig {
            return BackpressureConfig(
                maxCapacity = maxCapacity,
                policy = RejectionPolicy.CALLER_RUNS
            )
        }
    }
}
