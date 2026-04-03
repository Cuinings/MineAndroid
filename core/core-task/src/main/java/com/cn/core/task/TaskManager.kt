package com.cn.core.task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * 任务管理器
 * 
 * 全局单例对象，用于统一管理多个任务队列。
 * 提供创建、获取、移除队列的接口，以及便捷的任务提交方法。
 * 
 * 特性：
 * - 全局单例管理
 * - 支持创建命名队列
 * - 提供默认队列
 * - 统一的任务提交接口
 * - 支持全局任务取消
 * - 支持任务状态回调
 * - 队列状态监控
 * 
 * 使用示例：
 * ```kotlin
 * // 提交有序任务（带状态回调）
 * TaskManager.submitSerial(
 *     priority = TaskPriority.HIGH,
 *     block = { progress -> fetchData(progress) },
 *     onSuccess = { result -> handleResult(result) },
 *     onStateChange = { state -> handleState(state) }
 * )
 * 
 * // 提交并发任务
 * TaskManager.submitConcurrent(
 *     priority = TaskPriority.NORMAL,
 *     block = { progress -> downloadImage(url, progress) },
 *     onSuccess = { bitmap -> displayImage(bitmap) }
 * )
 * 
 * // 创建自定义队列
 * val customQueue = TaskManager.createSerialQueue("custom")
 * customQueue.enqueue { doSomething() }
 * ```
 * 
 * @see SerialTaskQueue
 * @see ConcurrentTaskQueue
 * @see Task
 * @see TaskState
 */
object TaskManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val serialQueues = ConcurrentHashMap<String, SerialTaskQueue>()
    private val concurrentQueues = ConcurrentHashMap<String, ConcurrentTaskQueue>()

    /**
     * 默认有序队列
     * 
     * 当调用 [submitSerial] 且未指定队列名时使用此队列。
     */
    private val defaultSerialQueue: SerialTaskQueue by lazy {
        createSerialQueue("default_serial")
    }

    /**
     * 默认并发队列
     * 
     * 当调用 [submitConcurrent] 且未指定队列名时使用此队列。
     */
    private val defaultConcurrentQueue: ConcurrentTaskQueue by lazy {
        createConcurrentQueue("default_concurrent")
    }

    /**
     * 创建有序任务队列
     * 
     * 创建一个新的有序任务队列，如果同名队列已存在则返回现有队列。
     * 
     * @param name 队列名称，用于后续获取队列
     * @param dispatcher 协程调度器，默认为 [Dispatchers.Default]
     * @return 创建或获取的有序任务队列
     */
    fun createSerialQueue(
        name: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): SerialTaskQueue {
        return serialQueues.getOrPut(name) {
            SerialTaskQueue(scope, dispatcher)
        }
    }

    /**
     * 创建并发任务队列
     * 
     * 创建一个新的并发任务队列，如果同名队列已存在则返回现有队列。
     * 
     * @param name 队列名称，用于后续获取队列
     * @param maxConcurrency 最大并发数，默认为 [ConcurrentTaskQueue.DEFAULT_CONCURRENCY]
     * @param dispatcher 协程调度器，默认为 [Dispatchers.Default]
     * @return 创建或获取的并发任务队列
     */
    fun createConcurrentQueue(
        name: String,
        maxConcurrency: Int = ConcurrentTaskQueue.DEFAULT_CONCURRENCY,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): ConcurrentTaskQueue {
        return concurrentQueues.getOrPut(name) {
            ConcurrentTaskQueue(scope, maxConcurrency, dispatcher)
        }
    }

    /**
     * 获取有序任务队列
     * 
     * 根据名称获取已创建的有序任务队列。
     * 
     * @param name 队列名称
     * @return 对应的有序任务队列，如果不存在则返回 null
     */
    fun getSerialQueue(name: String): SerialTaskQueue? = serialQueues[name]

    /**
     * 获取并发任务队列
     * 
     * 根据名称获取已创建的并发任务队列。
     * 
     * @param name 队列名称
     * @return 对应的并发任务队列，如果不存在则返回 null
     */
    fun getConcurrentQueue(name: String): ConcurrentTaskQueue? = concurrentQueues[name]

    /**
     * 移除队列
     * 
     * 移除指定名称的队列（包括有序和并发队列）。
     * 移除前会取消该队列中的所有任务。
     * 
     * @param name 要移除的队列名称
     */
    fun removeQueue(name: String) {
        serialQueues[name]?.cancelAll()
        serialQueues.remove(name)
        concurrentQueues[name]?.cancelAll()
        concurrentQueues.remove(name)
    }

    /**
     * 提交有序任务
     * 
     * 将任务提交到指定的有序队列中执行。
     * 如果队列不存在，则使用默认队列。
     * 
     * @param T 任务返回值类型
     * @param task 要执行的任务
     * @param queueName 队列名称，默认为 "default_serial"
     * @return 提交的任务实例
     */
    fun <T> submitSerial(
        task: Task<T>,
        queueName: String = "default_serial"
    ): Task<T> {
        val queue = serialQueues[queueName] ?: defaultSerialQueue
        return queue.enqueue(task)
    }

    /**
     * 创建并提交有序任务（带进度报告）
     * 
     * 便捷方法，创建一个新任务并提交到有序队列。
     * 
     * @param T 任务返回值类型
     * @param queueName 队列名称，默认为 "default_serial"
     * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
     * @param block 任务执行的挂起函数，接收进度报告器
     * @param onSuccess 成功回调，可选
     * @param onError 失败回调，可选
     * @param onStateChange 状态变化回调，可选
     * @return 创建并提交的任务实例
     */
    fun <T> submitSerial(
        queueName: String = "default_serial",
        priority: TaskPriority = TaskPriority.NORMAL,
        block: suspend (ProgressReporter) -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        onStateChange: ((TaskState) -> Unit)? = null
    ): Task<T> {
        val queue = serialQueues[queueName] ?: defaultSerialQueue
        return queue.enqueue(priority, block, onSuccess, onError, onStateChange)
    }

    /**
     * 创建并提交有序任务（简化版本，无进度报告）
     * 
     * @param T 任务返回值类型
     * @param queueName 队列名称，默认为 "default_serial"
     * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
     * @param block 任务执行的挂起函数
     * @param onSuccess 成功回调，可选
     * @param onError 失败回调，可选
     * @param onStateChange 状态变化回调，可选
     * @return 创建并提交的任务实例
     */
    fun <T> submitSerial(
        queueName: String = "default_serial",
        priority: TaskPriority = TaskPriority.NORMAL,
        block: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        onStateChange: ((TaskState) -> Unit)? = null
    ): Task<T> {
        val queue = serialQueues[queueName] ?: defaultSerialQueue
        return queue.enqueue(priority, block, onSuccess, onError, onStateChange)
    }

    /**
     * 提交并发任务
     * 
     * 将任务提交到指定的并发队列中执行。
     * 如果队列不存在，则使用默认队列。
     * 
     * @param T 任务返回值类型
     * @param task 要执行的任务
     * @param queueName 队列名称，默认为 "default_concurrent"
     * @return 提交的任务实例
     */
    fun <T> submitConcurrent(
        task: Task<T>,
        queueName: String = "default_concurrent"
    ): Task<T> {
        val queue = concurrentQueues[queueName] ?: defaultConcurrentQueue
        return queue.enqueue(task)
    }

    /**
     * 创建并提交并发任务（带进度报告）
     * 
     * 便捷方法，创建一个新任务并提交到并发队列。
     * 
     * @param T 任务返回值类型
     * @param queueName 队列名称，默认为 "default_concurrent"
     * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
     * @param block 任务执行的挂起函数，接收进度报告器
     * @param onSuccess 成功回调，可选
     * @param onError 失败回调，可选
     * @param onStateChange 状态变化回调，可选
     * @return 创建并提交的任务实例
     */
    fun <T> submitConcurrent(
        queueName: String = "default_concurrent",
        priority: TaskPriority = TaskPriority.NORMAL,
        block: suspend (ProgressReporter) -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        onStateChange: ((TaskState) -> Unit)? = null
    ): Task<T> {
        val queue = concurrentQueues[queueName] ?: defaultConcurrentQueue
        return queue.enqueue(priority, block, onSuccess, onError, onStateChange)
    }

    /**
     * 创建并提交并发任务（简化版本，无进度报告）
     * 
     * @param T 任务返回值类型
     * @param queueName 队列名称，默认为 "default_concurrent"
     * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
     * @param block 任务执行的挂起函数
     * @param onSuccess 成功回调，可选
     * @param onError 失败回调，可选
     * @param onStateChange 状态变化回调，可选
     * @return 创建并提交的任务实例
     */
    fun <T> submitConcurrent(
        queueName: String = "default_concurrent",
        priority: TaskPriority = TaskPriority.NORMAL,
        block: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        onStateChange: ((TaskState) -> Unit)? = null
    ): Task<T> {
        val queue = concurrentQueues[queueName] ?: defaultConcurrentQueue
        return queue.enqueue(priority, block, onSuccess, onError, onStateChange)
    }

    /**
     * 取消指定任务
     * 
     * 在所有队列中查找并取消指定ID的任务。
     * 
     * @param taskId 要取消的任务ID
     * @return 如果成功取消任务则返回 true，否则返回 false
     */
    fun cancelTask(taskId: String): Boolean {
        var cancelled = false
        serialQueues.values.forEach { queue ->
            if (queue.cancel(taskId)) cancelled = true
        }
        concurrentQueues.values.forEach { queue ->
            if (queue.cancel(taskId)) cancelled = true
        }
        return cancelled
    }

    /**
     * 取消所有任务
     * 
     * 取消所有队列中的所有任务。
     */
    fun cancelAll() {
        serialQueues.values.forEach { it.cancelAll() }
        concurrentQueues.values.forEach { it.cancelAll() }
    }

    /**
     * 关闭任务管理器
     * 
     * 取消所有任务，清空所有队列，并取消内部协程作用域。
     * 关闭后不应再使用此管理器。
     */
    fun shutdown() {
        cancelAll()
        serialQueues.clear()
        concurrentQueues.clear()
        scope.cancel()
    }

    /**
     * 获取队列统计信息
     * 
     * 获取所有队列的运行状态统计。
     * 
     * @return 队列名称到统计信息的映射
     */
    fun getQueueStats(): Map<String, QueueStats> {
        val stats = mutableMapOf<String, QueueStats>()
        serialQueues.forEach { (name, queue) ->
            stats["serial_$name"] = QueueStats(
                name = name,
                type = QueueType.SERIAL,
                pendingCount = queue.pendingCount,
                runningCount = if (queue.state.value is SerialTaskQueue.QueueState.Running) 1 else 0
            )
        }
        concurrentQueues.forEach { (name, queue) ->
            stats["concurrent_$name"] = QueueStats(
                name = name,
                type = QueueType.CONCURRENT,
                pendingCount = queue.pendingTaskCount,
                runningCount = queue.runningTaskCount
            )
        }
        return stats
    }

    /**
     * 队列统计信息
     * 
     * @param name 队列名称
     * @param type 队列类型
     * @param pendingCount 待执行任务数量
     * @param runningCount 正在执行任务数量
     */
    data class QueueStats(
        val name: String,
        val type: QueueType,
        val pendingCount: Int,
        val runningCount: Int
    )

    /**
     * 队列类型
     */
    enum class QueueType {
        /**
         * 有序队列
         */
        SERIAL,

        /**
         * 并发队列
         */
        CONCURRENT
    }
}
