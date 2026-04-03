package com.cn.core.task

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * 有序任务队列
 * 
 * 基于协程实现的串行任务队列，任务按照优先级顺序依次执行。
 * 高优先级任务会优先执行，同优先级任务按照先进先出(FIFO)顺序执行。
 * 
 * 特性：
 * - 任务按优先级排序执行
 * - 支持暂停和恢复
 * - 支持任务状态回调（等待中、执行中、完成）
 * - 支持背压控制（队列容量限制和拒绝策略）
 * - 线程安全
 * - 不阻塞主线程
 * - 支持任务取消
 * 
 * 使用示例：
 * ```kotlin
 * // 无背压控制
 * val queue = SerialTaskQueue(scope)
 * 
 * // 带背压控制
 * val queue = SerialTaskQueue(
 *     scope = scope,
 *     backpressureConfig = BackpressureConfig(
 *         maxCapacity = 100,
 *         policy = RejectionPolicy.DISCARD_OLDEST,
 *         onRejected = { task, reason -> println("拒绝: ${task.name}") }
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
 * @param dispatcher 协程调度器，默认为 [Dispatchers.Default]
 * @param backpressureConfig 背压配置，默认无限制
 * 
 * @see Task
 * @see TaskPriority
 * @see ConcurrentTaskQueue
 * @see TaskState
 * @see BackpressureConfig
 * @see RejectionPolicy
 */
class SerialTaskQueue(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val backpressureConfig: BackpressureConfig = BackpressureConfig.unbounded()
) {
    private val sequenceCounter = AtomicLong(0)
    private val taskQueue: PriorityQueue<Task<*>> = PriorityQueue(
        11,
        compareByDescending<Task<*>> { it.priority.ordinal }
            .thenBy { it.sequence }
    )
    private val queueLock = ReentrantLock()
    private val isRunning = AtomicBoolean(false)
    private val currentJob = AtomicReference<Job?>(null)
    private val _state = MutableStateFlow<QueueState>(QueueState.Idle)
    private val mutex = Mutex()

    /**
     * 队列状态
     * 
     * 表示任务队列的当前状态，可通过 [state] 属性观察状态变化。
     */
    sealed class QueueState {
        /**
         * 空闲状态
         * 
         * 队列中没有正在执行的任务，也没有待执行的任务。
         */
        data object Idle : QueueState()

        /**
         * 运行状态
         * 
         * @param taskName 正在执行的任务名称
         */
        data class Running(val taskName: String) : QueueState()

        /**
         * 暂停状态
         * 
         * @param pendingCount 待执行的任务数量
         */
        data class Paused(val pendingCount: Int) : QueueState()
        
        /**
         * 队列满状态
         * 
         * @param pendingCount 待执行的任务数量
         * @param maxCapacity 最大容量
         */
        data class Full(val pendingCount: Int, val maxCapacity: Int) : QueueState()
    }

    /**
     * 队列状态流
     * 
     * 可观察的任务队列状态，用于监控队列的运行情况。
     */
    val state: StateFlow<QueueState> = _state.asStateFlow()

    /**
     * 待执行任务数量
     * 
     * @return 当前队列中等待执行的任务数量
     */
    val pendingCount: Int
        get() = queueLock.withLock { taskQueue.size }

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
        get() = backpressureConfig.isBackpressureEnabled && pendingCount >= maxCapacity

    /**
     * 队列是否空闲
     * 
     * 当队列中没有待执行任务且没有正在执行的任务时返回 true。
     * 
     * @return 如果队列为空闲状态则返回 true
     */
    val isIdle: Boolean
        get() = pendingCount == 0 && !isRunning.get()

    /**
     * 将任务加入队列（带结果返回）
     * 
     * 将指定的任务添加到队列中，任务将按照优先级顺序执行。
     * 返回入队结果，用于处理背压场景。
     * 
     * @param T 任务返回值类型
     * @param task 要执行的任务
     * @return 入队结果
     */
    fun <T> enqueueWithResult(task: Task<T>): EnqueueResult {
        return queueLock.withLock {
            val currentSize = taskQueue.size
            
            if (backpressureConfig.isBackpressureEnabled && currentSize >= backpressureConfig.maxCapacity) {
                handleBackpressure(task, currentSize)
            } else {
                doEnqueue(task)
                checkThreshold(currentSize + 1)
                EnqueueResult.Success(task)
            }
        }.also { result ->
            if (result is EnqueueResult.Success) {
                processNext()
            }
        }
    }

    /**
     * 将任务加入队列
     * 
     * 将指定的任务添加到队列中，任务将按照优先级顺序执行。
     * 任务入队后会立即触发 [TaskState.Pending] 状态回调。
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
        Log.d("SerialTaskQueue", "enqueue: $result")
        return when (result) {
            is EnqueueResult.Success -> result.task as Task<T>
            is EnqueueResult.Rejected -> {
                if (backpressureConfig.policy == RejectionPolicy.THROW_EXCEPTION) {
                    throw TaskQueueFullException(
                        queueName = "SerialTaskQueue",
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
                    taskQueue.remove(discardedTask)
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
        return taskQueue.minByOrNull { it.priority.ordinal }
    }

    /**
     * 执行入队操作
     */
    private fun <T> doEnqueue(task: Task<T>) {
        task.sequence = sequenceCounter.getAndIncrement()
        taskQueue.offer(task)
        notifyPendingState(task)
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
        val queueSize = taskQueue.size
        task.updateState(TaskState.Pending(queuePosition = position, queueSize = queueSize))
    }

    /**
     * 计算任务在队列中的位置
     */
    private fun calculateQueuePosition(task: Task<*>): Int {
        var position = 1
        val iterator = taskQueue.iterator()
        while (iterator.hasNext()) {
            val t = iterator.next()
            if (t.id == task.id) {
                return position
            }
            position++
        }
        return -1
    }

    /**
     * 处理下一个任务
     * 
     * 内部方法，从队列中取出优先级最高的任务并执行。
     * 如果队列中有任务，会持续执行直到队列为空。
     */
    private fun processNext() {
        if (!isRunning.compareAndSet(false, true)) {
            return
        }
        scope.launch {
            try {
                while (scope.isActive) {
                    val task = mutex.withLock {
                        queueLock.withLock { taskQueue.poll() }
                    }
                    if (task == null) {
                        _state.value = QueueState.Idle
                        break
                    }
                    updatePendingPositions()
                    _state.value = QueueState.Running(task.name)
                    executeTask(task)
                }
            } finally {
                isRunning.set(false)
                queueLock.withLock {
                    if (taskQueue.isNotEmpty()) {
                        processNext()
                    }
                }
            }
        }
    }

    /**
     * 更新队列中所有等待任务的位置
     */
    private fun updatePendingPositions() {
        queueLock.withLock {
            val tasks = taskQueue.toList()
            var position = 1
            tasks.forEach { task ->
                task.updateState(TaskState.Pending(queuePosition = position, queueSize = tasks.size))
                position++
            }
        }
    }

    /**
     * 执行单个任务
     * 
     * 在指定的调度器上执行任务，并处理取消异常。
     * 
     * @param task 要执行的任务
     */
    private suspend fun executeTask(task: Task<*>) {
        val job = scope.launch {
            try {
                withContext(dispatcher) {
                    task.execute()
                }
            } catch (e: CancellationException) {
                task.isCancelled = true
                task.updateState(TaskState.Cancelled)
            }
        }
        currentJob.set(job)
        job.join()
        currentJob.set(null)
    }

    /**
     * 暂停队列
     * 
     * 取消当前正在执行的任务，并将队列状态设置为暂停。
     * 暂停后可以通过 [resume] 方法恢复执行。
     */
    fun pause() {
        currentJob.get()?.cancel()
        _state.value = QueueState.Paused(pendingCount)
    }

    /**
     * 恢复队列
     * 
     * 如果队列处于暂停状态，恢复任务执行。
     * 队列会继续执行待执行的任务。
     */
    fun resume() {
        if (_state.value is QueueState.Paused) {
            processNext()
        }
    }

    /**
     * 取消指定任务
     * 
     * 从队列中移除并取消指定ID的任务。
     * 如果任务正在执行，则无法取消。
     * 
     * @param taskId 要取消的任务ID
     * @return 如果成功取消任务则返回 true，否则返回 false
     */
    fun cancel(taskId: String): Boolean {
        return queueLock.withLock {
            val iterator = taskQueue.iterator()
            while (iterator.hasNext()) {
                val task = iterator.next()
                if (task.id == taskId) {
                    task.cancel()
                    task.updateState(TaskState.Cancelled)
                    taskQueue.remove(task)
                    updatePendingPositions()
                    return@withLock true
                }
            }
            false
        }
    }

    /**
     * 取消所有任务
     * 
     * 取消队列中所有待执行的任务，并取消当前正在执行的任务。
     * 队列状态将重置为空闲。
     */
    fun cancelAll() {
        queueLock.withLock {
            taskQueue.forEach { 
                it.cancel()
                it.updateState(TaskState.Cancelled)
            }
            taskQueue.clear()
        }
        currentJob.get()?.cancel()
        currentJob.set(null)
        _state.value = QueueState.Idle
    }

    /**
     * 清空队列
     * 
     * 清除队列中所有待执行的任务，但不取消正在执行的任务。
     */
    fun clear() {
        queueLock.withLock {
            taskQueue.clear()
        }
    }
}
