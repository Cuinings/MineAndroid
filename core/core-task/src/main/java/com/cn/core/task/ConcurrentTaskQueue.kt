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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 并发任务队列
 * 
 * 基于协程实现的并发任务队列，支持同时执行多个任务。
 * 使用 [Semaphore] 控制最大并发数，确保系统资源合理分配。
 * 
 * 特性：
 * - 可配置最大并发数
 * - 任务按优先级排序
 * - 线程安全
 * - 不阻塞主线程
 * - 支持优雅关闭
 * - 支持任务取消
 * 
 * 使用示例：
 * ```kotlin
 * val scope = CoroutineScope(SupervisorJob())
 * val queue = ConcurrentTaskQueue(scope, maxConcurrency = 4)
 * 
 * queue.enqueue(
 *     priority = TaskPriority.NORMAL,
 *     block = { downloadImage(url) },
 *     onSuccess = { bitmap -> displayImage(bitmap) }
 * )
 * ```
 * 
 * @param scope 协程作用域，用于启动任务执行的协程
 * @param maxConcurrency 最大并发数，默认为 [DEFAULT_CONCURRENCY]
 * @param dispatcher 协程调度器，默认为 [Dispatchers.Default]
 * 
 * @see Task
 * @see TaskPriority
 * @see SerialTaskQueue
 */
class ConcurrentTaskQueue(
    private val scope: CoroutineScope,
    private val maxConcurrency: Int = DEFAULT_CONCURRENCY,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val semaphore = Semaphore(maxConcurrency)
    private val runningJobs = ConcurrentHashMap<String, Job>()
    private val pendingTasks = ConcurrentHashMap<String, Task<*>>()
    private val isShutdown = AtomicBoolean(false)
    private val activeCount = AtomicInteger(0)
    private val _state = MutableStateFlow<ConcurrentState>(ConcurrentState.Idle)

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
     * 队列是否空闲
     * 
     * 当队列中没有正在执行任务且没有待执行任务时返回 true。
     * 
     * @return 如果队列为空闲状态则返回 true
     */
    val isIdle: Boolean
        get() = runningTaskCount == 0 && pendingTaskCount == 0

    /**
     * 将任务加入队列
     * 
     * 将指定的任务添加到队列中，任务将根据并发限制执行。
     * 如果队列已关闭，任务将被立即取消。
     * 
     * @param T 任务返回值类型
     * @param task 要执行的任务
     * @return 加入队列的任务实例
     */
    fun <T> enqueue(task: Task<T>): Task<T> {
        if (isShutdown.get()) {
            task.cancel()
            return task
        }
        pendingTasks[task.id] = task
        executeTask(task)
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
        return enqueue(task)
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
                    if (task.isCancelled || isShutdown.get()) {
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
        pendingTasks[taskId]?.cancel()
        pendingTasks.remove(taskId)
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
        pendingTasks.values.forEach { it.cancel() }
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
