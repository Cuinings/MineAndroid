# Core Task

基于 Kotlin 协程的任务队列管理模块，支持有序队列和并发队列，专为 Android 平台设计。

## 功能特性

- **有序任务队列** - 任务按优先级顺序依次执行
- **并发任务队列** - 支持配置最大并发数，多任务同时执行
- **优先级控制** - 支持 LOW、NORMAL、HIGH、IMMEDIATE 四种优先级
- **任务状态回调** - 支持等待中、执行中（含进度）、完成状态回调
- **进度报告** - 任务执行过程中可报告进度
- **超时控制** - 支持任务超时自动取消，防止阻塞后续任务
- **背压控制** - 支持队列容量限制和多种拒绝策略
- **生命周期管理** - 支持暂停、恢复、取消、关闭等操作
- **线程安全** - 所有操作均为线程安全
- **非阻塞主线程** - 所有任务在后台线程执行

## 模块结构

```
com.cn.core.task/
├── Task.kt                  # 任务封装类与构建器（含超时控制）
├── TaskPriority.kt          # 任务优先级枚举
├── TaskState.kt             # 任务状态与进度报告器
├── RejectionPolicy.kt       # 背压拒绝策略枚举
├── EnqueueResult.kt         # 入队结果密封类
├── BackpressureConfig.kt    # 背压配置类
├── SerialTaskQueue.kt       # 有序任务队列
├── ConcurrentTaskQueue.kt   # 并发任务队列
├── TaskManager.kt           # 全局任务管理器
├── TaskExecutors.kt         # 线程调度器封装
└── TaskQueueScope.kt        # 队列作用域
```

## 快速开始

### 添加依赖

```kotlin
implementation("com.cn.core:core-task:1.0.0")
```

### 基本使用

#### 1. 使用 TaskManager 提交任务

```kotlin
// 提交有序任务（带状态回调）
TaskManager.submitSerial(
    priority = TaskPriority.HIGH,
    block = { progress -> fetchData(progress) },
    onSuccess = { result -> handleResult(result) },
    onError = { error -> showError(error) },
    onStateChange = { state -> handleStateChange(state) }
)

// 提交并发任务
TaskManager.submitConcurrent(
    priority = TaskPriority.NORMAL,
    block = { progress -> downloadImage(url, progress) },
    onSuccess = { bitmap -> displayImage(bitmap) }
)
```

#### 2. 使用 TaskQueueScope 创建独立队列

```kotlin
// 创建有序队列
val serialScope = TaskQueueScope.serial("my_serial")
serialScope.enqueue(
    block = { progress -> task1(progress) },
    onStateChange = { state -> handleState(state) }
)

// 创建并发队列
val concurrentScope = TaskQueueScope.concurrent("my_concurrent", maxConcurrency = 8)
concurrentScope.enqueue(
    block = { progress -> downloadImage(url1, progress) },
    onStateChange = { state -> handleState(state) }
)

// 使用完毕后关闭
serialScope.shutdown()
concurrentScope.shutdown()
```

#### 3. 使用 DSL 构建任务

```kotlin
val task = task<String> {
    name = "LoadData"
    priority = TaskPriority.HIGH
    block { progress -> fetchDataFromNetwork(progress) }
    onSuccess { data -> updateUI(data) }
    onError { error -> showError(error) }
    onStateChange { state -> handleStateChange(state) }
}
TaskManager.submitSerial(task)
```

## 核心类说明

### TaskState - 任务状态

任务状态密封类，用于监控任务执行进度：

| 状态 | 说明 |
|------|------|
| `Pending` | 等待中，包含队列位置和队列大小 |
| `Running` | 执行中，包含进度（0.0~1.0）和消息 |
| `Completed` | 已完成，包含成功/失败状态和错误信息 |
| `Cancelled` | 已取消 |

```kotlin
task.onStateChange { state ->
    when (state) {
        is TaskState.Pending -> {
            println("等待中，位置: ${state.queuePosition}/${state.queueSize}")
        }
        is TaskState.Running -> {
            println("执行中，进度: ${(state.progress * 100).toInt()}%")
            state.message?.let { println("消息: $it") }
        }
        is TaskState.Completed -> {
            if (state.success) {
                println("执行成功")
            } else {
                println("执行失败: ${state.error?.message}")
            }
        }
        is TaskState.Cancelled -> {
            println("已取消")
        }
    }
}
```

### ProgressReporter - 进度报告器

在任务执行过程中报告进度：

```kotlin
val task = task<Unit> {
    name = "DownloadFile"
    block { progress ->
        for (i in 0..100) {
            progress.report(i / 100f, "下载中 $i%")
            Thread.sleep(50)
        }
    }
    onStateChange { state ->
        if (state is TaskState.Running) {
            updateProgressBar(state.progress)
        }
    }
}
```

### Task

任务封装类，包含执行逻辑、优先级和回调：

```kotlin
val task = Task(
    id = "task_001",
    name = "MyTask",
    priority = TaskPriority.HIGH,
    block = { progress -> doSomething(progress) },
    onSuccess = { result -> handleResult(result) },
    onError = { error -> handleError(error) },
    onComplete = { cleanup() },
    onStateChange = { state -> handleState(state) }
)

// 获取当前状态
val currentState = task.currentState
```

### TaskPriority

任务优先级枚举：

| 优先级 | 说明 |
|--------|------|
| LOW | 低优先级，适用于后台任务 |
| NORMAL | 普通优先级，默认值 |
| HIGH | 高优先级，适用于需要快速响应的任务 |
| IMMEDIATE | 最高优先级，立即执行 |

## 超时控制

当某个任务可能阻塞时，可以设置超时时间，确保后续任务能够继续执行。

### 设置任务超时

```kotlin
// 方式一：使用 DSL 构建带超时的任务
val task = task<String> {
    name = "NetworkRequest"
    priority = TaskPriority.HIGH
    timeout = 30.seconds  // 设置 30 秒超时
    block { progress -> 
        fetchDataFromNetwork(progress)
    }
    onSuccess { data -> handleData(data) }
    onTimeout { 
        Log.w("Task", "任务超时，已自动取消")
        showTimeoutError()
    }
    onError { error -> 
        if (error is TaskTimeoutException) {
            Log.w("Task", "任务超时: ${error.taskName}, 耗时: ${error.timeout}")
        }
    }
}

// 方式二：直接使用 enqueue 方法
queue.enqueue(
    timeout = 10.seconds,
    priority = TaskPriority.NORMAL,
    block = { progress -> downloadFile(url, progress) },
    onSuccess = { file -> handleFile(file) },
    onTimeout = { showTimeoutError() }
)

// 方式三：使用 TaskBuilder
val task = TaskBuilder<String>()
    .name("ApiCall")
    .timeout(15.seconds)  // 或 timeout(15000) 毫秒
    .block { apiService.getData() }
    .onSuccess { data -> processData(data) }
    .onTimeout { retryOrFallback() }
    .build()
```

### 超时处理流程

```
任务开始执行
    ↓
执行时间 < timeout → 正常完成 → onSuccess
    ↓
执行时间 ≥ timeout → 超时取消 → onTimeout → onError(TaskTimeoutException)
    ↓
后续任务继续执行
```

### 检查任务超时状态

```kotlin
val task = queue.enqueue(
    timeout = 5.seconds,
    block = { blockingOperation() }
)

// 检查是否超时
if (task.isTimeout) {
    println("任务因超时被取消")
}

// 检查当前状态
when (task.currentState) {
    is TaskState.Completed -> {
        val state = task.currentState as TaskState.Completed
        if (!state.success && state.error is TaskTimeoutException) {
            println("任务超时")
        }
    }
    else -> {}
}
```

### 超时与阻塞任务

```kotlin
// 问题场景：某个任务可能无限阻塞
val queue = SerialTaskQueue(scope)

// 任务1：可能阻塞的任务（设置超时）
queue.enqueue(
    timeout = 10.seconds,  // 最多等待 10 秒
    block = { 
        // 可能阻塞的网络请求或 I/O 操作
        potentiallyBlockingOperation()
    },
    onTimeout = {
        Log.w("Queue", "任务超时，自动取消")
    }
)

// 任务2：后续任务会继续执行（不受任务1阻塞影响）
queue.enqueue(
    block = { normalOperation() },
    onSuccess = { println("任务2正常执行") }
)
```

## 背压控制

当任务生产速度超过消费速度时，需要使用背压控制来保护系统稳定性。

### 拒绝策略

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `THROW_EXCEPTION` | 抛出异常 | 必须确保任务入队的场景 |
| `DISCARD` | 静默丢弃 | 非关键、可丢弃的任务 |
| `DISCARD_OLDEST` | 丢弃最旧任务 | 希望保留最新任务的场景 |
| `CALLER_RUNS` | 调用者执行 | 不希望丢失任务但可接受阻塞 |
| `BLOCK` | 阻塞等待 | 后台线程，需要保证入队成功 |

### 配置背压

```kotlin
// 创建带背压控制的队列
val queue = SerialTaskQueue(
    scope = scope,
    backpressureConfig = BackpressureConfig(
        maxCapacity = 100,                    // 最大容量
        policy = RejectionPolicy.DISCARD_OLDEST,  // 拒绝策略
        warnThreshold = 80,                   // 警告阈值（80%）
        onRejected = { task, reason ->        // 拒绝回调
            Log.w("Queue", "任务被拒绝: ${task.name}, $reason")
        },
        onDiscarded = { oldTask, newTask ->   // 丢弃回调
            Log.w("Queue", "丢弃旧任务: ${oldTask.name}")
        },
        onThresholdReached = { size, max, percent ->  // 阈值回调
            Log.w("Queue", "队列使用率: $percent%")
        }
    )
)

// 使用便捷方法创建配置
val config = BackpressureConfig.discardOldest(maxCapacity = 50)
val config = BackpressureConfig.throwException(maxCapacity = 100)
val config = BackpressureConfig.callerRuns(maxCapacity = 200)
```

### 入队结果处理

```kotlin
// 使用 enqueueWithResult 处理背压
val result = queue.enqueueWithResult(task)

when (result) {
    is EnqueueResult.Success -> {
        println("任务入队成功: ${result.task.id}")
    }
    is EnqueueResult.Rejected -> {
        println("任务被拒绝: ${result.reason}")
        // 处理拒绝逻辑，如重试、记录等
    }
    is EnqueueResult.Discarded -> {
        println("丢弃旧任务: ${result.discardedTask.name}")
    }
    is EnqueueResult.CallerExecuted -> {
        println("任务已在调用者线程执行，成功: ${result.success}")
    }
}

// 判断结果
if (result.isSuccess) {
    println("任务已处理")
}
if (result.isRejected) {
    println("任务被拒绝")
}
```

### 队列状态监控

```kotlin
// 检查队列状态
if (queue.isFull) {
    println("队列已满")
}

// 获取队列信息
println("待执行: ${queue.pendingCount}")
println("最大容量: ${queue.maxCapacity}")

// 监听队列状态流
queue.state.collect { state ->
    when (state) {
        is SerialTaskQueue.QueueState.Full -> {
            println("队列满: ${state.pendingCount}/${state.maxCapacity}")
        }
        // ...
    }
}
```

### SerialTaskQueue

有序任务队列，任务按优先级顺序依次执行：

```kotlin
val queue = SerialTaskQueue(scope)

// 入队任务（带状态回调）
queue.enqueue(
    priority = TaskPriority.HIGH,
    block = { progress -> fetchData(progress) },
    onSuccess = { result -> handleResult(result) },
    onStateChange = { state -> handleState(state) }
)

// 暂停/恢复
queue.pause()
queue.resume()

// 取消任务
queue.cancel(taskId)
queue.cancelAll()
```

### ConcurrentTaskQueue

并发任务队列，支持同时执行多个任务：

```kotlin
val queue = ConcurrentTaskQueue(scope, maxConcurrency = 4)

// 入队任务
queue.enqueue(
    block = { progress -> downloadImage(url, progress) },
    onStateChange = { state -> handleState(state) }
)

// 等待所有任务完成
queue.awaitCompletion {
    println("All tasks completed")
}

// 关闭队列
queue.shutdown()
```

### TaskManager

全局任务管理器，统一管理所有队列：

```kotlin
// 创建自定义队列
val customQueue = TaskManager.createSerialQueue("custom")

// 获取队列
val queue = TaskManager.getSerialQueue("custom")

// 移除队列
TaskManager.removeQueue("custom")

// 获取统计信息
val stats = TaskManager.getQueueStats()

// 全局取消
TaskManager.cancelAll()

// 关闭管理器
TaskManager.shutdown()
```

### TaskExecutors

线程调度器封装，简化线程切换：

```kotlin
// IO 线程执行
val data = TaskExecutors.runOnIO { fetchData() }

// 计算线程执行
val result = TaskExecutors.runOnComputation { processData() }

// 主线程执行
TaskExecutors.runOnMain { updateUI(result) }
```

## 使用场景

### 网络请求队列（带状态监控）

```kotlin
val networkQueue = TaskQueueScope.serial("network")

fun fetchUserData(userId: String) {
    networkQueue.enqueue(
        priority = TaskPriority.HIGH,
        block = { progress -> 
            progress.report(0f, "开始请求")
            val result = apiService.getUser(userId)
            progress.report(1f, "请求完成")
            result
        },
        onSuccess = { user -> updateUserUI(user) },
        onStateChange = { state ->
            when (state) {
                is TaskState.Pending -> showWaiting(state.queuePosition)
                is TaskState.Running -> showProgress(state.progress)
                is TaskState.Completed -> hideProgress()
                else -> {}
            }
        }
    )
}
```

### 图片下载并发（带进度）

```kotlin
val imageQueue = TaskQueueScope.concurrent("image", maxConcurrency = 4)

fun downloadImages(urls: List<String>) {
    urls.forEach { url ->
        imageQueue.enqueue(
            block = { progress -> imageLoader.load(url, progress) },
            onSuccess = { bitmap -> cacheImage(url, bitmap) },
            onStateChange = { state ->
                if (state is TaskState.Running) {
                    updateDownloadProgress(url, state.progress)
                }
            }
        )
    }
}
```

### 数据库操作队列

```kotlin
val dbQueue = TaskQueueScope.serial("database")

fun saveData(data: Data) {
    dbQueue.enqueue(
        priority = TaskPriority.HIGH,
        block = { progress -> 
            progress.report(0f, "开始保存")
            database.insert(data)
            progress.report(1f, "保存完成")
        },
        onError = { error -> logError(error) },
        onStateChange = { state ->
            if (state is TaskState.Pending) {
                println("排队中，位置: ${state.queuePosition}")
            }
        }
    )
}
```

## 注意事项

1. **生命周期管理** - 在 Activity/Fragment 销毁时调用 `cancelAll()` 或 `shutdown()` 避免内存泄漏
2. **异常处理** - 建议始终设置 `onError` 回调处理异常
3. **并发数配置** - 根据设备性能和任务类型合理配置并发数
4. **优先级使用** - 避免滥用高优先级，确保关键任务优先执行
5. **状态回调** - 状态回调在后台线程执行，UI 更新需切换到主线程
6. **超时控制** - 对于可能阻塞的任务，务必设置超时时间，防止阻塞后续任务
7. **背压控制** - 对于高并发场景，建议配置背压控制避免内存溢出
8. **队列容量** - 根据任务执行时间和内存情况合理设置队列容量
9. **拒绝策略** - 根据业务场景选择合适的拒绝策略，关键任务建议使用 `THROW_EXCEPTION`

## 与 ThreadPoolExecutor 对比

| 功能 | ThreadPoolExecutor | core-task |
|------|-------------------|-----------|
| **底层实现** | Java 线程池 | Kotlin 协程 |
| **线程模型** | 固定线程，独占资源 | 轻量级协程，共享线程池 |
| **优先级支持** | ❌ 需自行实现 | ✅ 原生支持 4 级优先级 |
| **状态回调** | ❌ 需自行封装 | ✅ Pending/Running/Completed/Cancelled |
| **进度报告** | ❌ 不支持 | ✅ ProgressReporter |
| **超时控制** | ⚠️ 需自行实现 | ✅ 原生支持，自动取消阻塞任务 |
| **背压控制** | ✅ 队列大小 + 拒绝策略 | ✅ BackpressureConfig + 5种策略 |
| **取消支持** | ⚠️ 需检查 interrupted | ✅ 协程取消机制 |
| **暂停/恢复** | ❌ 不支持 | ✅ pause/resume |
| **生命周期** | ⚠️ 需手动 shutdown | ✅ 协程作用域自动管理 |
| **内存占用** | 较高（线程栈 ~1MB） | 低（协程轻量级） |

### 选择建议

| 场景 | 推荐 |
|------|------|
| 需要背压控制、守护线程、长时间阻塞任务 | ThreadPoolExecutor |
| 需要优先级、状态回调、进度报告、协程集成 | core-task |
| Android 项目已使用 Kotlin 协程 | core-task |
| 需要精确控制线程属性（守护线程、优先级） | ThreadPoolExecutor |

## 性能建议

### 队列容量配置

```kotlin
// 根据任务类型配置容量
val networkQueue = SerialTaskQueue(
    scope,
    BackpressureConfig.discardOldest(maxCapacity = 50)  // 网络请求
)

val downloadQueue = ConcurrentTaskQueue(
    scope,
    maxConcurrency = 4,
    BackpressureConfig.discardOldest(maxCapacity = 20)  // 文件下载
)

val dbQueue = SerialTaskQueue(
    scope,
    BackpressureConfig.throwException(maxCapacity = 100)  // 数据库操作，关键任务
)
```

### 并发数配置

```kotlin
// CPU 密集型任务：并发数 = CPU 核心数
val cpuBoundQueue = ConcurrentTaskQueue(scope, maxConcurrency = Runtime.getRuntime().availableProcessors())

// IO 密集型任务：并发数 = CPU 核心数 * 2
val ioBoundQueue = ConcurrentTaskQueue(scope, maxConcurrency = Runtime.getRuntime().availableProcessors() * 2)

// 网络请求：建议 4-8
val networkQueue = ConcurrentTaskQueue(scope, maxConcurrency = 4)

// 文件下载：建议 2-4
val downloadQueue = ConcurrentTaskQueue(scope, maxConcurrency = 2)
```

## License

MIT License
