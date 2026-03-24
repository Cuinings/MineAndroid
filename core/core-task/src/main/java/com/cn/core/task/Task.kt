package com.cn.core.task

import kotlinx.coroutines.CancellationException

/**
 * 任务封装类
 * 
 * 表示一个可执行的任务单元，包含任务执行逻辑、优先级、回调等信息。
 * 支持泛型返回值类型，可在任务完成后通过回调获取执行结果。
 * 
 * 任务特性：
 * - 支持优先级排序
 * - 支持成功、失败、完成回调
 * - 支持取消操作
 * - 线程安全
 * 
 * 使用示例：
 * ```kotlin
 * val task = Task(
 *     name = "LoadData",
 *     priority = TaskPriority.HIGH,
 *     block = { fetchDataFromNetwork() },
 *     onSuccess = { data -> updateUI(data) },
 *     onError = { error -> showError(error) }
 * )
 * ```
 * 
 * @param T 任务执行结果的类型
 * @param id 任务唯一标识符，默认自动生成UUID
 * @param name 任务名称，用于调试和日志记录
 * @param priority 任务优先级，默认为 [TaskPriority.NORMAL]
 * @param block 任务执行的挂起函数，返回类型为 T
 * @param onSuccess 任务成功回调，接收执行结果
 * @param onError 任务失败回调，接收异常信息
 * @param onComplete 任务完成回调，无论成功或失败都会调用
 * 
 * @see TaskPriority
 * @see TaskBuilder
 * @see task
 */
data class Task<T>(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Task-$id",
    val priority: TaskPriority = TaskPriority.NORMAL,
    val block: suspend () -> T,
    private val onSuccess: ((T) -> Unit)? = null,
    private val onError: ((Throwable) -> Unit)? = null,
    private val onComplete: (() -> Unit)? = null
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
     * 执行任务
     * 
     * 内部方法，由任务队列调用执行实际的任务逻辑。
     * 执行流程：
     * 1. 检查任务是否已取消
     * 2. 执行 [block] 获取结果
     * 3. 根据执行结果调用相应的回调
     * 
     * @return 执行结果，包含成功值或失败异常
     */
    internal suspend fun execute(): Result<T> {
        return try {
            if (isCancelled) {
                Result.failure(CancellationException("Task $name was cancelled"))
            } else {
                val result = block()
                onSuccess?.invoke(result)
                Result.success(result)
            }
        } catch (e: CancellationException) {
            isCancelled = true
            Result.failure(e)
        } catch (e: Exception) {
            onError?.invoke(e)
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
 * 任务构建器
 * 
 * 使用 Builder 模式构建 [Task] 实例，提供更灵活的任务配置方式。
 * 
 * 使用示例：
 * ```kotlin
 * val task = TaskBuilder<String>()
 *     .name("LoadData")
 *     .priority(TaskPriority.HIGH)
 *     .block { fetchData() }
 *     .onSuccess { println(it) }
 *     .build()
 * ```
 * 
 * @param T 任务执行结果的类型
 * 
 * @see Task
 * @see task
 */
class TaskBuilder<T> {
    private var id: String = java.util.UUID.randomUUID().toString()
    private var name: String = "Task-$id"
    private var priority: TaskPriority = TaskPriority.NORMAL
    private var block: (suspend () -> T)? = null
    private var onSuccess: ((T) -> Unit)? = null
    private var onError: ((Throwable) -> Unit)? = null
    private var onComplete: (() -> Unit)? = null

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
     * 设置任务执行逻辑
     * 
     * @param block 挂起函数，包含任务的实际执行逻辑
     * @return 构建器实例，支持链式调用
     */
    fun block(block: suspend () -> T) = apply { this.block = block }

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
            block = block!!,
            onSuccess = onSuccess,
            onError = onError,
            onComplete = onComplete
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
 *     block { fetchData() }
 *     onSuccess { println(it) }
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
