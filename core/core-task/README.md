# Core Task

基于 Kotlin 协程的任务队列管理模块，支持有序队列和并发队列，专为 Android 平台设计。

## 功能特性

- **有序任务队列** - 任务按优先级顺序依次执行
- **并发任务队列** - 支持配置最大并发数，多任务同时执行
- **优先级控制** - 支持 LOW、NORMAL、HIGH、IMMEDIATE 四种优先级
- **生命周期管理** - 支持暂停、恢复、取消、关闭等操作
- **线程安全** - 所有操作均为线程安全
- **非阻塞主线程** - 所有任务在后台线程执行

## 模块结构

```
com.cn.core.task/
├── Task.kt                  # 任务封装类与构建器
├── TaskPriority.kt          # 任务优先级枚举
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
// 提交有序任务
TaskManager.submitSerial(
    priority = TaskPriority.HIGH,
    block = { fetchData() },
    onSuccess = { result -> handleResult(result) },
    onError = { error -> showError(error) }
)

// 提交并发任务
TaskManager.submitConcurrent(
    priority = TaskPriority.NORMAL,
    block = { downloadImage(url) },
    onSuccess = { bitmap -> displayImage(bitmap) }
)
```

#### 2. 使用 TaskQueueScope 创建独立队列

```kotlin
// 创建有序队列
val serialScope = TaskQueueScope.serial("my_serial")
serialScope.enqueue { task1() }
serialScope.enqueue { task2() }

// 创建并发队列
val concurrentScope = TaskQueueScope.concurrent("my_concurrent", maxConcurrency = 8)
concurrentScope.enqueue { downloadImage(url1) }
concurrentScope.enqueue { downloadImage(url2) }

// 使用完毕后关闭
serialScope.shutdown()
concurrentScope.shutdown()
```

#### 3. 使用 DSL 构建任务

```kotlin
val task = task<String> {
    name = "LoadData"
    priority = TaskPriority.HIGH
    block { fetchDataFromNetwork() }
    onSuccess { data -> updateUI(data) }
    onError { error -> showError(error) }
}
TaskManager.submitSerial(task)
```

## 核心类说明

### Task

任务封装类，包含执行逻辑、优先级和回调。

```kotlin
val task = Task(
    id = "task_001",
    name = "MyTask",
    priority = TaskPriority.HIGH,
    block = { doSomething() },
    onSuccess = { result -> handleResult(result) },
    onError = { error -> handleError(error) },
    onComplete = { cleanup() }
)
```

### TaskPriority

任务优先级枚举：

| 优先级 | 说明 |
|--------|------|
| LOW | 低优先级，适用于后台任务 |
| NORMAL | 普通优先级，默认值 |
| HIGH | 高优先级，适用于需要快速响应的任务 |
| IMMEDIATE | 最高优先级，立即执行 |

### SerialTaskQueue

有序任务队列，任务按优先级顺序依次执行。

```kotlin
val queue = SerialTaskQueue(scope)

// 入队任务
queue.enqueue(task)

// 暂停/恢复
queue.pause()
queue.resume()

// 取消任务
queue.cancel(taskId)
queue.cancelAll()
```

### ConcurrentTaskQueue

并发任务队列，支持同时执行多个任务。

```kotlin
val queue = ConcurrentTaskQueue(scope, maxConcurrency = 4)

// 入队任务
queue.enqueue(task)

// 等待所有任务完成
queue.awaitCompletion {
    println("All tasks completed")
}

// 关闭队列
queue.shutdown()
```

### TaskManager

全局任务管理器，统一管理所有队列。

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

线程调度器封装，简化线程切换。

```kotlin
// IO 线程执行
val data = TaskExecutors.runOnIO { fetchData() }

// 计算线程执行
val result = TaskExecutors.runOnComputation { processData() }

// 主线程执行
TaskExecutors.runOnMain { updateUI(result) }
```

## 使用场景

### 网络请求队列

```kotlin
val networkQueue = TaskQueueScope.serial("network")

fun fetchUserData(userId: String) {
    networkQueue.enqueue(
        priority = TaskPriority.HIGH,
        block = { apiService.getUser(userId) },
        onSuccess = { user -> updateUserUI(user) }
    )
}
```

### 图片下载并发

```kotlin
val imageQueue = TaskQueueScope.concurrent("image", maxConcurrency = 4)

fun downloadImages(urls: List<String>) {
    urls.forEach { url ->
        imageQueue.enqueue(
            block = { imageLoader.load(url) },
            onSuccess = { bitmap -> cacheImage(url, bitmap) }
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
        block = { database.insert(data) },
        onError = { error -> logError(error) }
    )
}
```

## 注意事项

1. **生命周期管理** - 在 Activity/Fragment 销毁时调用 `cancelAll()` 或 `shutdown()` 避免内存泄漏
2. **异常处理** - 建议始终设置 `onError` 回调处理异常
3. **并发数配置** - 根据设备性能和任务类型合理配置并发数
4. **优先级使用** - 避免滥用高优先级，确保关键任务优先执行

## License

MIT License
