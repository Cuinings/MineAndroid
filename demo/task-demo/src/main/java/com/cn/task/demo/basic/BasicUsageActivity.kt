package com.cn.task.demo.basic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cn.core.task.Task
import com.cn.core.task.TaskManager
import com.cn.core.task.TaskPriority
import com.cn.core.task.TaskQueueScope
import com.cn.task.demo.databinding.ActivityBasicUsageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.suspend

class BasicUsageActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBasicUsageBinding
    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBasicUsageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSubmitSerial.setOnClickListener {
            submitSerialTask()
        }
        
        binding.btnSubmitConcurrent.setOnClickListener {
            submitConcurrentTask()
        }
        
        binding.btnUseScope.setOnClickListener {
            useTaskQueueScope()
        }
        
        binding.btnUseDsl.setOnClickListener {
            useDslBuilder()
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

    private fun submitSerialTask() {
        log("=== 提交有序任务到 TaskManager ===")
        
        val task = Task(
            name = "SerialTask",
            priority = TaskPriority.NORMAL,
            block = {
                log("有序任务开始执行...")
                /*withContext(Dispatchers.IO) {
                    delay(1000)
                    return@withContext 0
                }*/
                log("有序任务执行完成")
                "SerialTask Result"
            },
            onSuccess = { result ->
                log("成功回调: $result")
            },
            onError = { error ->
                log("错误回调: ${error.message}")
            }
        )
        
        TaskManager.submitSerial(task)?.let {

        }
        log("任务已提交，ID: ${task.id}")
    }

    private fun submitConcurrentTask() {
        log("=== 提交并发任务到 TaskManager ===")
        
        TaskManager.submitConcurrent(
            priority = TaskPriority.HIGH,
            block = suspend {
                log("并发任务开始执行...")
                withContext(Dispatchers.IO) {
                    delay(800)
                }
                log("并发任务执行完成")
                "ConcurrentTask Result"
            },
            onSuccess = { result ->
                log("成功回调: $result")
            },
            onError = { error ->
                log("错误回调: ${error.message}")
            }
        )
    }

    private fun useTaskQueueScope() {
        log("=== 使用 TaskQueueScope 创建独立队列 ===")
        
        val serialScope = TaskQueueScope.serial("demo_serial")
        
        serialScope.enqueue(
            priority = TaskPriority.NORMAL,
            block = suspend {
                log("TaskQueueScope 任务1开始...")
                delay(500)
                log("TaskQueueScope 任务1完成")
                "Result 1"
            },
            onSuccess = { log("任务1结果: $it") }
        )
        
        serialScope.enqueue(
            priority = TaskPriority.HIGH,
            block = suspend {
                log("TaskQueueScope 任务2开始 (高优先级)...")
                delay(300)
                log("TaskQueueScope 任务2完成")
                "Result 2"
            },
            onSuccess = { log("任务2结果: $it") }
        )
        
        log("两个任务已加入队列，高优先级任务会先执行")
    }

    private fun useDslBuilder() {
        log("=== 使用 DSL 构建任务 ===")
        
        val task = com.cn.core.task.task<String> {
            name = "DslTask"
            priority = TaskPriority.HIGH
            block (suspend {
                log("DSL 任务开始执行...")
                withContext(Dispatchers.IO) {
                    delay(600)
                }
                log("DSL 任务执行完成")
                "DSL Result"
            })
            onSuccess { result ->
                log("DSL 成功: $result")
            }
            onError { error ->
                log("DSL 错误: ${error.message}")
            }
            onComplete {
                log("DSL 完成（无论成功失败）")
            }
        }
        
        TaskManager.submitSerial(task)
        log("DSL 任务已提交，ID: ${task.id}")
    }
}
