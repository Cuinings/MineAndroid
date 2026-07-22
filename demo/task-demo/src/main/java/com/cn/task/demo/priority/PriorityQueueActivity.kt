package com.cn.task.demo.priority

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cn.core.task.Task
import com.cn.core.task.TaskPriority
import com.cn.core.task.TaskState
import com.cn.task.demo.databinding.ActivityPriorityQueueBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PriorityQueueActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPriorityQueueBinding
    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private var taskCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPriorityQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSubmitAll.setOnClickListener {
            submitTasksWithDifferentPriorities()
        }
        
        binding.btnSubmitHigh.setOnClickListener {
            submitTaskWithPriority(TaskPriority.HIGH)
        }
        
        binding.btnSubmitImmediate.setOnClickListener {
            submitTaskWithPriority(TaskPriority.IMMEDIATE)
        }
        
        binding.btnCancelAll.setOnClickListener {
            com.cn.core.task.TaskManager.cancelAll()
            log("已取消所有任务")
        }
        
        binding.btnClear.setOnClickListener {
            logBuilder.clear()
            binding.tvLog.text = ""
        }
    }

    private fun log(message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] $message\n"
        logBuilder.append(logMessage)
        runOnUiThread {
            binding.tvLog.text = logBuilder.toString()
        }
    }

    private fun submitTasksWithDifferentPriorities() {
        log("=== 提交不同优先级的任务 ===")
        log("提交顺序: LOW -> NORMAL -> HIGH -> IMMEDIATE")
        log("预期执行顺序: IMMEDIATE -> HIGH -> NORMAL -> LOW")
        log("---")
        
        submitTaskWithPriority(TaskPriority.LOW, "LOW-1")
        submitTaskWithPriority(TaskPriority.NORMAL, "NORMAL-1")
        submitTaskWithPriority(TaskPriority.HIGH, "HIGH-1")
        submitTaskWithPriority(TaskPriority.IMMEDIATE, "IMMEDIATE-1")
    }

    private fun submitTaskWithPriority(priority: TaskPriority, customName: String? = null) {
        taskCounter++
        val taskName = customName ?: "${priority.name}-$taskCounter"
        
        val task = Task(
            name = taskName,
            priority = priority,
            block = {
                withContext(Dispatchers.IO) {
                    delay(500)
                }
                "$taskName Result"
            },
            onSuccess = { result ->
                log("✓ 完成: $result")
            },
            onStateChange = { state ->
                when (state) {
                    is TaskState.Running -> {
                        if (state.progress == 0f) {
                            log("▶ 执行: $taskName (${getPriorityLabel(priority)})")
                        }
                    }
                    is TaskState.Pending -> {
                        log("⏳ 等待: $taskName (位置: ${state.queuePosition})")
                    }
                    else -> {}
                }
            }
        )
        
        com.cn.core.task.TaskManager.submitSerial(task)
        log("📤 提交: $taskName (${getPriorityLabel(priority)})")
    }

    private fun getPriorityLabel(priority: TaskPriority): String {
        return when (priority) {
            TaskPriority.LOW -> "低优先级"
            TaskPriority.NORMAL -> "普通优先级"
            TaskPriority.HIGH -> "高优先级"
            TaskPriority.IMMEDIATE -> "立即执行"
        }
    }
}
