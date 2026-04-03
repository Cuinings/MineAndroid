package com.cn.core.task

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * 并发任务队列
 * 
 * 基于协程实现的并发任务队列，支持同时执行多个任务。
 * 使用 [Semaphore] 控制最大并发数，确保系统资源合理分配。
 * 
 * 特性：
 * - 可配置最大并发数
 * - 任务按优先级排序
 * - 支持任务状态回调（等待中、执行中、完成）
 * - 支持背压控制（队列容量限制和拒绝策略）
 * - 线程安全
 * - 不阻塞主线程
 * - 支持优雅关闭
 * - 支持任务取消
 * 
 * 使用示例：
 * ```kotlin
 * // 无背压控制
 * val queue = ConcurrentTaskQueue(scope, maxConcurrency = 4)
 * 
 * // 带背压控制
 * val queue = ConcurrentTaskQueue(
 *     scope = scope,
 *     maxConcurrency = 4,
 *     backpressureConfig = BackpressureConfig(
 *         maxCapacity = 100,
 *         policy = RejectionPolicy.DISCARD_OLDEST
 *     )
 * )
 * 
 * // 入队并检查结果
 * val result = queue.enqueueWithResult(task)
 * when (result) {
 *     is EnqueueResult.Success -> println("入队成功")
 *     is EnqueueResult.Rejected -> println("被拒绝: ${result.reason}")
 *     else -> {}
 * }
 * ```
 * 
 * @param scope 协程作用域，用于启动任务执行的协程
 * @param maxConcurrency 最大并发数，默认为 [DEFAULT_CONCURRENCY]
 * @param dispatcher 协程调度器，默认为 [Dispatchers.Default]
 * @param backpressureConfig 背压配置，默认无限制
 * 
 * @see Task
 * @see TaskPriority
 * @see SerialTaskQueue
 * @see TaskState
 * @see BackpressureConfig
 * @see RejectionPolicy
 */
class ConcurrentTaskQueue(
    private val scope: CoroutineScope,
    private val maxConcurrency: Int = DEFAULT_CONCURRENCY,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val backpressureConfig: BackpressureConfig = BackpressureConfig.unbounded()
) {
    private val semaphore = Semaphore(maxConcurrency)
    private val runningJobs = ConcurrentHashMap<String, Job>()
    private val pendingTasks = ConcurrentHashMap<String, Task<*>>()
    private val isShutdown = AtomicBoolean(false)
    private val activeCount = AtomicInteger(0)
    private val _state = MutableStateFlow<ConcurrentState>(ConcurrentState.Idle)
    private val queueLock = ReentrantLock()

    /**
     * 队列状态流
     * 
     * 可观察的任务队列状态，用于监控队列的运行情况。
     */
    val state: StateFlow<ConcurrentState> = _state.asStateFlow()

    /**
     * 并发队列状态
     * 
     * 表示并发任务队列的当前状态。
     */
    sealed class ConcurrentState {
        /**
         * 空闲状态
         * 
         * 队列中没有正在执行的任务，也没有待执行的任务。
         */
        data object Idle : ConcurrentState()

        /**
         * 运行状态
         * 
         * @param activeCount 正在执行的任务数量
         * @param pendingCount 待执行的任务数量
         */
        data class Running(val activeCount: Int, val pendingCount: Int) : ConcurrentState()

        /**
         * 已关闭状态
         * 
         * 队列已调用 [shutdown] 方法，不再接受新任务。
         */
        data object Shutdown : ConcurrentState()
        
        /**
         * 队列满状态
         * 
         * @param activeCount 正在执行的任务数量
         * @param pendingCount 待执行的任务数量
         * @param maxCapacity 最大容量
         */
        data class Full(
            val activeCount: Int,
            val pendingCount: Int,
            val maxCapacity: Int
        ) : ConcurrentState()
    }

    companion object {
        /**
         * 默认并发数
         */
        const val DEFAULT_CONCURRENCY = 4

        /**
         * 无限制并发数
         * 
         * 当设置为此值时，不限制并发任务数量（受限于系统资源）。
         */
        const val UNLIMITED_CONCURRENCY = Int.MAX_VALUE
    }

    /**
     * 正在执行的任务数量
     * 
     * @return 当前正在执行的任务数量
     */
    val runningTaskCount: Int
        get() = activeCount.get()

    /**
     * 待执行任务数量
     * 
     * @return 当前队列中等待执行的任务数量
     */
    val pendingTaskCount: Int
        get() = pendingTasks.size

    /**
     * 队列最大容量
     * 
     * @return 队列最大容量，[BackpressureConfig.UNBOUNDED] 表示无限制
     */
    val maxCapacity: Int
        get() = backpressureConfig.maxCapacity

    /**
     * 队列是否已满
     * 
     * @return 如果队列已满则返回 true
     */
    val isFull: Boolean
        get() = backpressureConfig.isBackpressureEnabled && pendingTaskCount >= maxCapacity

    /**
     * 队列是否空闲
     * 
     * 当队列中没有正在执行任务且没有待执行任务时返回 true。
     * 
     * @return 如果队列为空闲状态则返回 true
     */
    val isIdle: Boolean
        get() = runningTaskCount == 0 && pendingTaskCount == 0

    /**
     * 将任务加入队列（带结果返回）
     * 
     * 将指定的任务添加到队列中，任务将根据并发限制执行。
     * 返回入队结果，用于处理背压场景。
     * 
     * @param T 任务返回值类型
     * @param task 要执行的任务
     * @return 入队结果
     */
    fun <T> enqueueWithResult(task: Task<T>): EnqueueResult {
        if (isShutdown.get()) {
            task.cancel()
            task.updateState(TaskState.Cancelled)
            return EnqueueResult.Rejected(
                task = task,
                reason = "Queue is shutdown",
                queueSize = pendingTaskCount,
                maxCapacity = maxCapacity
            )
        }
        
        return queueLock.withLock {
            val currentSize = pendingTasks.size
            
            if (backpressureConfig.isBackpressureEnabled && currentSize >= backpressureConfig.maxCapacity) {
                handleBackpressure(task, currentSize)
            } else {
                doEnqueue(task)
                checkThreshold(currentSize + 1)
                EnqueueResult.Success(task)
            }
        }
    }

    /**
     * 将任务加入队列
     * 
     * 将指定的任务添加到队列中，任务将根据并发限制执行。
     * 任务入队后会立即触发 [TaskState.Pending] 状态回调。
     * 如果队列已关闭，任务将被立即取消。
     * 
     * 注意：如果配置了 [RejectionPolicy.THROW_EXCEPTION] 且队列满，会抛出异常。
     * 建议使用 [enqueueWithResult] 方法处理背压场景。
     * 
     * @param T 任务返回值类型
     * @param task 要执行的任务
     * @return 加入队列的任务实例
     * @throws TaskQueueFullException 如果队列满且策略为 [RejectionPolicy.THROW_EXCEPTION]
     */
    fun <T> enqueue(task: Task<T>): Task<T> {
        val result = enqueueWithResult(task)
        return when (result) {
            is EnqueueResult.Success -> result.task as Task<T>
            is EnqueueResult.Rejected -> {
                if (backpressureConfig.policy == RejectionPolicy.THROW_EXCEPTION) {
                    throw TaskQueueFullException(
                        queueName = "ConcurrentTaskQueue",
                        queueSize = result.queueSize,
                        maxCapacity = result.maxCapacity
                    )
                }
                task
            }
            is EnqueueResult.Discarded -> result.task as Task<T>
            is EnqueueResult.CallerExecuted -> result.task as Task<T>
        }
    }

    /**
     * 创建并加入任务（带结果返回）
     * 
     * 便捷方法，创建一个新任务并加入队列。
     * 
     * @param T 任务返回值类型
     * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
     * @param block 任务执行的挂起函数
     * @param onSuccess 成功回调，可选
     * @param onError 失败回调，可选
     * @param onStateChange 状态变化回调，可选
     * @return 入队结果
     */
    fun <T> enqueueWithResult(
        priority: TaskPriority = TaskPriority.NORMAL,
        block: suspend (ProgressReporter) -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        onStateChange: ((TaskState) -> Unit)? = null
    ): EnqueueResult {
        val task = Task(
            priority = priority,
            block = block,
            onSuccess = onSuccess,
            onError = onError,
            onStateChange = onStateChange
        )
        return enqueueWithResult(task)
    }

    /**
     * 创建并加入任务
     * 
     * 便捷方法，创建一个新任务并加入队列。
     * 
     * @param T 任务返回值类型
     * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
     * @param block 任务执行的挂起函数
     * @param onSuccess 成功回调，可选
     * @param onError 失败回调，可选
     * @param onStateChange 状态变化回调，可选
     * @return 创建并加入队列的任务实例
     */
    fun <T> enqueue(
        priority: TaskPriority = TaskPriority.NORMAL,
        block: suspend (ProgressReporter) -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        onStateChange: ((TaskState) -> Unit)? = null
    ): Task<T> {
        val task = Task(
            priority = priority,
            block = block,
            onSuccess = onSuccess,
            onError = onError,
            onStateChange = onStateChange
        )
        return enqueue(task)
    }

    /**
     * 创建并加入任务（简化版本，无进度报告）
     * 
     * @param T 任务返回值类型
     * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
     * @param block 任务执行的挂起函数
     * @param onSuccess 成功回调，可选
     * @param onError 失败回调，可选
     * @param onStateChange 状态变化回调，可选
     * @return 创建并加入队列的任务实例
     */
    fun <T> enqueue(
        priority: TaskPriority = TaskPriority.NORMAL,
        block: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        onStateChange: ((TaskState) -> Unit)? = null
    ): Task<T> {
        return enqueue(
            priority = priority,
            block = { _: ProgressReporter -> block() },
            onSuccess = onSuccess,
            onError = onError,
            onStateChange = onStateChange
        )
    }

    /**
     * 创建并加入任务（带超时控制）
     * 
     * 便捷方法，创建一个带超时限制的任务并加入队列。
     * 当任务执行时间超过 timeout 时，会自动取消任务并触发 onTimeout 回调。
     * 
     * @param T 任务返回值类型
     * @param timeout 超时时间
     * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
     * @param block 任务执行的挂起函数
     * @param onSuccess 成功回调，可选
     * @param onError 失败回调，可选
     * @param onTimeout 超时回调，可选
     * @param onStateChange 状态变化回调，可选
     * @return 创建并加入队列的任务实例
     */
    fun <T> enqueue(
        timeout: Duration,
        priority: TaskPriority = TaskPriority.NORMAL,
        block: suspend (ProgressReporter) -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        onTimeout: (() -> Unit)? = null,
        onStateChange: ((TaskState) -> Unit)? = null
    ): Task<T> {
        val task = Task(
            priority = priority,
            timeout = timeout,
            block = block,
            onSuccess = onSuccess,
            onError = onError,
            onTimeout = onTimeout,
            onStateChange = onStateChange
        )
        return enqueue(task)
    }

    /**
     * 创建并加入任务（简化版本，带超时控制，无进度报告）
     * 
     * @param T 任务返回值类型
     * @param timeout 超时时间
     * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
     * @param block 任务执行的挂起函数
     * @param onSuccess 成功回调，可选
     * @param onError 失败回调，可选
     * @param onTimeout 超时回调，可选
     * @param onStateChange 状态变化回调，可选
     * @return 创建并加入队列的任务实例
     */
    fun <T> enqueue(
        timeout: Duration,
        priority: TaskPriority = TaskPriority.NORMAL,
        block: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        onTimeout: (() -> Unit)? = null,
        onStateChange: ((TaskState) -> Unit)? = null
    ): Task<T> {
        return enqueue(
            timeout = timeout,
            priority = priority,
            block = { _: ProgressReporter -> block() },
            onSuccess = onSuccess,
            onError = onError,
            onTimeout = onTimeout,
            onStateChange = onStateChange
        )
    }

    /**
     * 处理背压
     * 
     * 根据配置的拒绝策略处理队列满的情况。
     */
    private fun <T> handleBackpressure(task: Task<T>, currentSize: Int): EnqueueResult {
        val reason = "Queue is full (size: $currentSize, max: ${backpressureConfig.maxCapacity})"
        
        backpressureConfig.onRejected?.invoke(task, reason)
        
        return when (backpressureConfig.policy) {
            RejectionPolicy.THROW_EXCEPTION -> {
                EnqueueResult.Rejected(
                    task = task,
                    reason = reason,
                    queueSize = currentSize,
                    maxCapacity = backpressureConfig.maxCapacity
                )
            }
            
            RejectionPolicy.DISCARD -> {
                task.cancel()
                task.updateState(TaskState.Cancelled)
                EnqueueResult.Rejected(
                    task = task,
                    reason = "Discarded by policy",
                    queueSize = currentSize,
                    maxCapacity = backpressureConfig.maxCapacity
                )
            }
            
            RejectionPolicy.DISCARD_OLDEST -> {
                val discardedTask = findLowestPriorityTask()
                if (discardedTask != null) {
                    pendingTasks.remove(discardedTask.id)
                    discardedTask.cancel()
                    discardedTask.updateState(TaskState.Cancelled)
                    backpressureConfig.onDiscarded?.invoke(discardedTask, task)
                    doEnqueue(task)
                    EnqueueResult.Discarded(task, discardedTask)
                } else {
                    doEnqueue(task)
                    EnqueueResult.Success(task)
                }
            }
            
            RejectionPolicy.CALLER_RUNS -> {
                try {
                    runBlocking {
                        task.execute()
                    }
                    EnqueueResult.CallerExecuted(task, success = true)
                } catch (e: Exception) {
                    EnqueueResult.CallerExecuted(task, success = false, error = e)
                }
            }
            
            RejectionPolicy.BLOCK -> {
                doEnqueue(task)
                EnqueueResult.Success(task)
            }
        }
    }

    /**
     * 查找优先级最低的任务
     */
    private fun findLowestPriorityTask(): Task<*>? {
        return pendingTasks.values.minByOrNull { it.priority.ordinal }
    }

    /**
     * 执行入队操作
     */
    private fun <T> doEnqueue(task: Task<T>) {
        pendingTasks[task.id] = task
        notifyPendingState(task)
        executeTask(task)
    }

    /**
     * 检查阈值并触发回调
     */
    private fun checkThreshold(currentSize: Int) {
        if (backpressureConfig.isThresholdReached(currentSize)) {
            val usagePercent = backpressureConfig.calculateUsagePercent(currentSize)
            backpressureConfig.onThresholdReached?.invoke(currentSize, backpressureConfig.maxCapacity, usagePercent)
        }
    }

    /**
     * 通知任务进入等待状态
     */
    private fun notifyPendingState(task: Task<*>) {
        val position = calculateQueuePosition(task)
        val queueSize = pendingTasks.size
        task.updateState(TaskState.Pending(queuePosition = position, queueSize = queueSize))
    }

    /**
     * 计算任务在队列中的位置
     */
    private fun calculateQueuePosition(task: Task<*>): Int {
        var position = 1
        pendingTasks.keys.forEach { id ->
            if (id == task.id) {
                return position
            }
            position++
        }
        return -1
    }

    /**
     * 更新所有等待任务的位置
     */
    private fun updatePendingPositions() {
        val tasks = pendingTasks.values.toList()
        var position = 1
        tasks.forEach { task ->
            task.updateState(TaskState.Pending(queuePosition = position, queueSize = tasks.size))
            position++
        }
    }

    /**
     * 执行任务
     * 
     * 内部方法，使用信号量控制并发执行任务。
     * 任务会在获取信号量许可后执行。
     * 
     * @param T 任务返回值类型
     * @param task 要执行的任务
     */
    private fun <T> executeTask(task: Task<T>) {
        val job = scope.launch {
            try {
                semaphore.withPermit {
                    pendingTasks.remove(task.id)
                    updatePendingPositions()
                    if (task.isCancelled || isShutdown.get()) {
                        task.updateState(TaskState.Cancelled)
                        return@withPermit
                    }
                    activeCount.incrementAndGet()
                    updateState()
                    try {
                        withContext(dispatcher) {
                            task.execute()
                        }
                    } catch (e: CancellationException) {
                        task.isCancelled = true
                        task.updateState(TaskState.Cancelled)
                    } finally {
                        activeCount.decrementAndGet()
                        updateState()
                    }
                }
            } finally {
                runningJobs.remove(task.id)
            }
        }
        runningJobs[task.id] = job
    }

    /**
     * 更新队列状态
     * 
     * 内部方法，根据当前活跃任务数和待执行任务数更新状态流。
     */
    private fun updateState() {
        val active = activeCount.get()
        val pending = pendingTasks.size
        _state.value = if (active == 0 && pending == 0) {
            ConcurrentState.Idle
        } else if (backpressureConfig.isBackpressureEnabled && pending >= backpressureConfig.maxCapacity) {
            ConcurrentState.Full(active, pending, backpressureConfig.maxCapacity)
        } else {
            ConcurrentState.Running(active, pending)
        }
    }

    /**
     * 取消指定任务
     * 
     * 取消指定ID的任务，包括待执行和正在执行的任务。
     * 
     * @param taskId 要取消的任务ID
     * @return 如果成功取消任务则返回 true，否则返回 false
     */
    fun cancel(taskId: String): Boolean {
        pendingTasks[taskId]?.let { task ->
            task.cancel()
            task.updateState(TaskState.Cancelled)
            pendingTasks.remove(taskId)
            updatePendingPositions()
        }
        val job = runningJobs.remove(taskId)
        job?.cancel()
        return job != null || pendingTasks.containsKey(taskId)
    }

    /**
     * 取消所有任务
     * 
     * 取消队列中所有待执行的任务和正在执行的任务。
     * 队列状态将重置为空闲。
     */
    fun cancelAll() {
        pendingTasks.values.forEach { 
            it.cancel()
            it.updateState(TaskState.Cancelled)
        }
        pendingTasks.clear()
        runningJobs.values.forEach { it.cancel() }
        runningJobs.clear()
        _state.value = ConcurrentState.Idle
    }

    /**
     * 关闭队列
     * 
     * 关闭队列，不再接受新任务，并取消所有现有任务。
     * 关闭后的队列无法重新打开。
     */
    fun shutdown() {
        isShutdown.set(true)
        cancelAll()
        _state.value = ConcurrentState.Shutdown
    }

    /**
     * 等待所有任务完成
     * 
     * 异步等待队列中所有任务执行完成，然后调用回调函数。
     * 
     * @param onComplete 所有任务完成后的回调函数
     */
    fun awaitCompletion(onComplete: () -> Unit) {
        scope.launch {
            while (!isIdle) {
                kotlinx.coroutines.delay(50)
            }
            onComplete()
        }
    }
}
