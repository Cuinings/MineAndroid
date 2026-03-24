package com.cn.core.task

/**
 * 任务优先级枚举
 * 
 * 用于控制任务在队列中的执行顺序，优先级越高的任务越先执行。
 * 
 * 优先级从低到高依次为：
 * - [LOW]: 低优先级，适用于后台任务或非紧急操作
 * - [NORMAL]: 普通优先级，默认优先级，适用于大多数常规任务
 * - [HIGH]: 高优先级，适用于需要快速响应的任务
 * - [IMMEDIATE]: 最高优先级，适用于需要立即执行的紧急任务
 * 
 * @see Task
 * @see SerialTaskQueue
 * @see ConcurrentTaskQueue
 */
enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    IMMEDIATE
}
