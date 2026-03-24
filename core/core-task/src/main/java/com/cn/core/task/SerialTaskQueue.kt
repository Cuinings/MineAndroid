package com.cn.core.task

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 有序任务队列
 * 
 * 基于协程实现的串行任务队列，任务按照优先级顺序依次执行。
 * 高优先级任务会优先执行，同优先级任务按照先进先出(FIFO)顺序执行。
 * 
 * 特性：
 * - 任务按优先级排序执行
 * - 支持暂停和恢复
 * - 线程安全
 * - 不阻塞主线程
 * - 支持任务取消
 * 
 * 使用示例：
 * ```kotlin
 * val scope = CoroutineScope(SupervisorJob())
 * val queue = SerialTaskQueue(scope)
 * 
 * queue.enqueue(
 *     priority = TaskPriority.HIGH,
 *     block = { fetchData() },
 *     onSuccess = { result -> handleResult(result) }
 * )
 * ```
 * 
 * @param scope 协程作用域，用于启动任务执行的协程
 * @param dispatcher 协程调度器，默认为 [Dispatchers.Default]
 * 
 * @see Task
 * @see TaskPriority
 * @see ConcurrentTaskQueue
 */
class SerialTaskQueue(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val sequenceCounter = AtomicLong(0)
    private val taskQueue = PriorityBlockingQueue<Task<*>>(
        11,
        compareByDescending<Task<*>> { it.priority.ordinal }
            .thenBy { it.sequence }
    )
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
        get() = taskQueue.size

    /**
     * 队列是否空闲
     * 
     * 当队列中没有待执行任务且没有正在执行的任务时返回 true。
     * 
     * @return 如果队列为空闲状态则返回 true
     */
    val isIdle: Boolean
        get() = taskQueue.isEmpty() && !isRunning.get()

    /**
     * 将任务加入队列
     * 
     * 将指定的任务添加到队列中，任务将按照优先级顺序执行。
     * 
     * @param T 任务返回值类型
     * @param task 要执行的任务
     * @return 加入队列的任务实例
     */
    fun <T> enqueue(task: Task<T>): Task<T> {
        task.sequence = sequenceCounter.getAndIncrement()
        taskQueue.offer(task)
        processNext()
        return task
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
     * @return 创建并加入队列的任务实例
     */
    fun <T> enqueue(
        priority: TaskPriority = TaskPriority.NORMAL,
        block: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ): Task<T> {
        val task = Task(
            priority = priority,
            block = block,
            onSuccess = onSuccess,
            onError = onError
        )
        task.sequence = sequenceCounter.getAndIncrement()
        taskQueue.offer(task)
        processNext()
        return task
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
                        taskQueue.poll()
                    }
                    if (task == null) {
                        _state.value = QueueState.Idle
                        break
                    }
                    _state.value = QueueState.Running(task.name)
                    executeTask(task)
                }
            } finally {
                isRunning.set(false)
                if (taskQueue.isNotEmpty()) {
                    processNext()
                }
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
        val iterator = taskQueue.iterator()
        while (iterator.hasNext()) {
            val task = iterator.next()
            if (task.id == taskId) {
                task.cancel()
                taskQueue.remove(task)
                return true
            }
        }
        return false
    }

    /**
     * 取消所有任务
     * 
     * 取消队列中所有待执行的任务，并取消当前正在执行的任务。
     * 队列状态将重置为空闲。
     */
    fun cancelAll() {
        taskQueue.forEach { it.cancel() }
        taskQueue.clear()
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
        taskQueue.clear()
    }
}
