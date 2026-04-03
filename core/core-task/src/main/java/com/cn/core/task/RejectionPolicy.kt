package com.cn.core.task

/**
 * 背压拒绝策略
 * 
 * 定义当任务队列达到容量上限时的处理策略。
 * 
 * @see BackpressureConfig
 * @see SerialTaskQueue
 * @see ConcurrentTaskQueue
 */
enum class RejectionPolicy {
    /**
     * 抛出异常
     * 
     * 当队列满时抛出 [TaskQueueFullException]，由调用者处理。
     * 适用于必须确保任务入队的场景。
     */
    THROW_EXCEPTION,
    
    /**
     * 丢弃任务
     * 
     * 静默丢弃新任务，不抛出异常，不执行任何回调。
     * 适用于任务非关键、可丢弃的场景。
     */
    DISCARD,
    
    /**
     * 丢弃最旧任务
     * 
     * 移除队列中优先级最低、最早入队的任务，然后入队新任务。
     * 适用于希望保留最新任务的场景。
     */
    DISCARD_OLDEST,
    
    /**
     * 调用者执行
     * 
     * 在调用者线程中直接执行任务（同步执行）。
     * 适用于不希望丢失任务但可以接受阻塞的场景。
     * 注意：会导致调用者线程阻塞，谨慎在主线程使用。
     */
    CALLER_RUNS,
    
    /**
     * 阻塞等待
     * 
     * 阻塞调用者线程，直到队列有空间可用。
     * 注意：仅适用于后台线程，禁止在主线程使用。
     */
    BLOCK
}

/**
 * 任务队列已满异常
 * 
 * 当使用 [RejectionPolicy.THROW_EXCEPTION] 策略且队列满时抛出。
 * 
 * @param queueName 队列名称
 * @param queueSize 当前队列大小
 * @param maxCapacity 队列最大容量
 */
class TaskQueueFullException(
    val queueName: String,
    val queueSize: Int,
    val maxCapacity: Int
) : Exception(
    "Task queue '$queueName' is full. Current size: $queueSize, Max capacity: $maxCapacity"
)
