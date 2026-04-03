package com.cn.core.task

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 任务封装类
 * 
 * 表示一个可执行的任务单元，包含任务执行逻辑、优先级、回调等信息。
 * 支持泛型返回值类型，可在任务完成后通过回调获取执行结果。
 * 
 * 任务特性：
 * - 支持优先级排序
 * - 支持成功、失败、完成回调
 * - 支持状态变化回调（等待中、执行中、完成）
 * - 支持进度报告
 * - 支持取消操作
 * - 支持超时控制
 * - 线程安全
 * 
 * 使用示例：
 * ```kotlin
 * val task = Task(
 *     name = "LoadData",
 *     priority = TaskPriority.HIGH,
 *     timeout = 30.seconds,
 *     block = { progress -> fetchDataFromNetwork(progress) },
 *     onSuccess = { data -> updateUI(data) },
 *     onError = { error -> showError(error) },
 *     onStateChange = { state -> handleStateChange(state) }
 * )
 * ```
 * 
 * @param T 任务执行结果的类型
 * @param id 任务唯一标识符，默认自动生成UUID
 * @param name 任务名称，用于调试和日志记录
 * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
 * @param timeout 任务超时时间，默认为 [Duration.INFINITE] 表示无限制
 * @param block 任务执行的挂起函数，接收 [ProgressReporter] 参数，返回类型为 T
 * @param onSuccess 任务成功回调，接收执行结果
 * @param onError 任务失败回调，接收异常信息
 * @param onComplete 任务完成回调，无论成功或失败都会调用
 * @param onStateChange 状态变化回调，接收 [TaskState]
 * @param onTimeout 超时回调，任务超时时触发
 * 
 * @see TaskPriority
 * @see TaskBuilder
 * @see task
 * @see TaskState
 */
data class Task<T>(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Task-$id",
    val priority: TaskPriority = TaskPriority.NORMAL,
    val timeout: Duration = Duration.INFINITE,
    val block: suspend (ProgressReporter) -> T,
    val onSuccess: ((T) -> Unit)? = null,
    val onError: ((Throwable) -> Unit)? = null,
    val onComplete: (() -> Unit)? = null,
    val onStateChange: ((TaskState) -> Unit)? = null,
    val onTimeout: (() -> Unit)? = null
) {
    /**
     * 任务序列号
     * 
     * 用于同优先级任务按 FIFO 顺序执行。
     * 由任务队列自动设置，无需手动设置。
     */
    var sequence: Long = 0L
        internal set

    /**
     * 任务是否已被取消
     * 
     * 当任务被取消时，此属性会被设置为 true。
     * 取消后的任务不会执行其 [block] 逻辑。
     */
    var isCancelled: Boolean = false
        internal set

    /**
     * 任务是否已超时
     * 
     * 当任务执行时间超过 [timeout] 时，此属性会被设置为 true。
     */
    var isTimeout: Boolean = false
        internal set

    private val stateLock = Any()
    private var _currentState: TaskState = TaskState.Pending()

    /**
     * 当前任务状态
     */
    val currentState: TaskState
        get() = synchronized(stateLock) { _currentState }

    /**
     * 是否设置了超时
     */
    val hasTimeout: Boolean
        get() = timeout != Duration.INFINITE && timeout.isPositive()

    /**
     * 内部进度报告器实现
     */
    private val progressReporter = object : ProgressReporter {
        override fun report(progress: Float, message: String?) {
            updateState(TaskState.Running(progress.coerceIn(0f, 1f), message))
        }
    }

    /**
     * 更新任务状态
     * 
     * 内部方法，由任务队列调用更新状态。
     * 
     * @param newState 新的任务状态
     */
    internal fun updateState(newState: TaskState) {
        synchronized(stateLock) {
            _currentState = newState
        }
        onStateChange?.invoke(newState)
    }

    /**
     * 执行任务
     * 
     * 内部方法，由任务队列调用执行实际的任务逻辑。
     * 执行流程：
     * 1. 检查任务是否已取消
     * 2. 更新状态为 Running
     * 3. 执行 [block] 获取结果（支持超时控制）
     * 4. 根据执行结果调用相应的回调
     * 5. 更新状态为 Completed
     * 
     * @return 执行结果，包含成功值或失败异常
     */
    internal suspend fun execute(): Result<T> {
        return try {
            if (isCancelled) {
                updateState(TaskState.Cancelled)
                Result.failure(CancellationException("Task $name was cancelled"))
            } else {
                updateState(TaskState.Running())
                val result = if (hasTimeout) {
                    withTimeout(timeout) {
                        block(progressReporter)
                    }
                } else {
                    block(progressReporter)
                }
                onSuccess?.invoke(result)
                updateState(TaskState.Completed(success = true))
                Result.success(result)
            }
        } catch (e: TimeoutCancellationException) {
            isTimeout = true
            onTimeout?.invoke()
            val timeoutError = TaskTimeoutException(name, timeout)
            onError?.invoke(timeoutError)
            updateState(TaskState.Completed(success = false, error = timeoutError))
            Result.failure(timeoutError)
        } catch (e: CancellationException) {
            isCancelled = true
            updateState(TaskState.Cancelled)
            Result.failure(e)
        } catch (e: Exception) {
            onError?.invoke(e)
            updateState(TaskState.Completed(success = false, error = e))
            Result.failure(e)
        } finally {
            onComplete?.invoke()
        }
    }

    /**
     * 取消任务
     * 
     * 将任务标记为已取消状态。
     * 已取消的任务在执行时会跳过 [block] 逻辑并返回失败结果。
     */
    fun cancel() {
        isCancelled = true
    }

    /**
     * 判断两个任务是否相等
     * 
     * 基于 [id] 进行相等性比较。
     * 
     * @param other 另一个对象
     * @return 如果 [id] 相同则返回 true
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Task<*>) return false
        return id == other.id
    }

    /**
     * 获取任务的哈希码
     * 
     * 基于 [id] 计算哈希值。
     * 
     * @return 任务的哈希码
     */
    override fun hashCode(): Int = id.hashCode()
}

/**
 * 任务超时异常
 * 
 * 当任务执行时间超过设定的超时时间时抛出。
 * 
 * @param taskName 任务名称
 * @param timeout 超时时间
 */
class TaskTimeoutException(
    val taskName: String,
    val timeout: Duration
) : Exception("Task '$taskName' timed out after $timeout")

/**
 * 任务构建器
 * 
 * 使用 Builder 模式构建 [Task] 实例，提供更灵活的任务配置方式。
 * 
 * 使用示例：
 * ```kotlin
 * val task = TaskBuilder<String>()
 *     .name("LoadData")
 *     .priority(TaskPriority.HIGH)
 *     .timeout(30.seconds)
 *     .block { progress -> fetchData(progress) }
 *     .onSuccess { println(it) }
 *     .onStateChange { state -> println(state) }
 *     .build()
 * ```
 * 
 * @param T 任务执行结果的类型
 * 
 * @see Task
 * @see task
 */
class TaskBuilder<T> {
    var id: String = java.util.UUID.randomUUID().toString()
    var name: String = "Task-$id"
    var priority: TaskPriority = TaskPriority.NORMAL
    var timeout: Duration = Duration.INFINITE
    var block: (suspend (ProgressReporter) -> T)? = null
    var onSuccess: ((T) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null
    var onComplete: (() -> Unit)? = null
    var onStateChange: ((TaskState) -> Unit)? = null
    var onTimeout: (() -> Unit)? = null

    /**
     * 设置任务ID
     * 
     * @param id 任务唯一标识符
     * @return 构建器实例，支持链式调用
     */
    fun id(id: String) = apply { this.id = id }

    /**
     * 设置任务名称
     * 
     * @param name 任务名称，用于调试和日志记录
     * @return 构建器实例，支持链式调用
     */
    fun name(name: String) = apply { this.name = name }

    /**
     * 设置任务优先级
     * 
     * @param priority 任务优先级
     * @return 构建器实例，支持链式调用
     */
    fun priority(priority: TaskPriority) = apply { this.priority = priority }

    /**
     * 设置任务超时时间
     * 
     * 当任务执行时间超过设定值时，会自动取消任务并触发 [onTimeout] 回调。
     * 
     * @param timeout 超时时间
     * @return 构建器实例，支持链式调用
     */
    fun timeout(timeout: Duration) = apply { this.timeout = timeout }

    /**
     * 设置任务超时时间（毫秒）
     * 
     * @param timeoutMillis 超时时间（毫秒）
     * @return 构建器实例，支持链式调用
     */
    fun timeout(timeoutMillis: Long) = apply { this.timeout = timeoutMillis.milliseconds }

    /**
     * 设置任务执行逻辑（带进度报告）
     * 
     * @param block 挂起函数，接收进度报告器，包含任务的实际执行逻辑
     * @return 构建器实例，支持链式调用
     */
    fun block(block: suspend (ProgressReporter) -> T) = apply { this.block = block }

    /**
     * 设置任务执行逻辑（无进度报告）
     * 
     * @param block 挂起函数，包含任务的实际执行逻辑
     * @return 构建器实例，支持链式调用
     */
    fun block(block: suspend () -> T) = apply { 
        this.block = { _: ProgressReporter -> block() }
    }

    /**
     * 设置成功回调
     * 
     * @param callback 任务成功时调用的回调函数
     * @return 构建器实例，支持链式调用
     */
    fun onSuccess(callback: (T) -> Unit) = apply { this.onSuccess = callback }

    /**
     * 设置失败回调
     * 
     * @param callback 任务失败时调用的回调函数
     * @return 构建器实例，支持链式调用
     */
    fun onError(callback: (Throwable) -> Unit) = apply { this.onError = callback }

    /**
     * 设置完成回调
     * 
     * @param callback 任务完成时调用的回调函数（无论成功或失败）
     * @return 构建器实例，支持链式调用
     */
    fun onComplete(callback: () -> Unit) = apply { this.onComplete = callback }

    /**
     * 设置状态变化回调
     * 
     * @param callback 任务状态变化时调用的回调函数
     * @return 构建器实例，支持链式调用
     */
    fun onStateChange(callback: (TaskState) -> Unit) = apply { this.onStateChange = callback }

    /**
     * 设置超时回调
     * 
     * @param callback 任务超时时调用的回调函数
     * @return 构建器实例，支持链式调用
     */
    fun onTimeout(callback: () -> Unit) = apply { this.onTimeout = callback }

    /**
     * 构建 Task 实例
     * 
     * 根据配置的参数创建 [Task] 实例。
     * 必须先调用 [block] 方法设置执行逻辑，否则会抛出异常。
     * 
     * @return 配置完成的 [Task] 实例
     * @throws IllegalArgumentException 如果未设置 [block]
     */
    fun build(): Task<T> {
        requireNotNull(block) { "Task block must not be null" }
        return Task(
            id = id,
            name = name,
            priority = priority,
            timeout = timeout,
            block = block!!,
            onSuccess = onSuccess,
            onError = onError,
            onComplete = onComplete,
            onStateChange = onStateChange,
            onTimeout = onTimeout
        )
    }
}

/**
 * DSL 风格的任务构建函数
 * 
 * 提供简洁的 DSL 语法来创建 [Task] 实例。
 * 
 * 使用示例：
 * ```kotlin
 * val task = task<String> {
 *     name = "LoadData"
 *     priority = TaskPriority.HIGH
 *     timeout = 30.seconds
 *     block { progress -> fetchData(progress) }
 *     onSuccess { println(it) }
 *     onTimeout { println("任务超时") }
 * }
 * ```
 * 
 * @param T 任务执行结果的类型
 * @param block 配置 [TaskBuilder] 的 lambda 表达式
 * @return 构建完成的 [Task] 实例
 * 
 * @see Task
 * @see TaskBuilder
 */
inline fun <reified T> task(block: TaskBuilder<T>.() -> Unit): Task<T> {
    return TaskBuilder<T>().apply(block).build()
}
