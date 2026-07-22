package com.cn.task.demo.state

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cn.core.task.Task
import com.cn.core.task.TaskManager
import com.cn.core.task.TaskPriority
import com.cn.core.task.TaskState
import com.cn.core.task.task
import com.cn.task.demo.R
import com.cn.task.demo.databinding.ActivityStateCallbackBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StateCallbackActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityStateCallbackBinding
    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private var currentTask: Task<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStateCallbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSubmitTask.setOnClickListener {
            submitTaskWithStateCallback()
        }
        
        binding.btnSubmitMultiple.setOnClickListener {
            submitMultipleTasks()
        }
        
        binding.btnCancelTask.setOnClickListener {
            cancelCurrentTask()
        }
        
        binding.btnClear.setOnClickListener {
            logBuilder.clear()
            binding.tvLog.text = ""
        }
    }

    private fun log(message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] $message\n"
        runOnUiThread {
            logBuilder.append(logMessage)
            binding.tvLog.text = logBuilder.toString()
        }
    }

    private fun updateStateDisplay(state: TaskState) {
        when (state) {
            is TaskState.Pending -> {
                binding.tvState.text = "等待中"
                binding.tvState.setTextColor(ContextCompat.getColor(this, R.color.orange))
                binding.tvQueueInfo.text = "队列位置: ${state.queuePosition}/${state.queueSize}"
                binding.tvQueueInfo.visibility = View.VISIBLE
            }
            is TaskState.Running -> {
                binding.tvState.text = "执行中"
                binding.tvState.setTextColor(ContextCompat.getColor(this, R.color.blue))
                binding.tvQueueInfo.text = if (state.message != null) {
                    "进度: ${(state.progress * 100).toInt()}% - ${state.message}"
                } else {
                    "进度: ${(state.progress * 100).toInt()}%"
                }
                binding.tvQueueInfo.visibility = View.VISIBLE
            }
            is TaskState.Completed -> {
                binding.tvState.text = if (state.success) "已完成" else "失败"
                binding.tvState.setTextColor(
                    if (state.success) ContextCompat.getColor(this, R.color.green)
                    else ContextCompat.getColor(this, R.color.red)
                )
                binding.tvQueueInfo.text = if (state.error != null) {
                    "错误: ${state.error?.message?:"NULL"}"
                } else {
                    "执行成功"
                }
                binding.tvQueueInfo.visibility = View.VISIBLE
            }
            is TaskState.Cancelled -> {
                binding.tvState.text = "已取消"
                binding.tvState.setTextColor(ContextCompat.getColor(this, R.color.gray))
                binding.tvQueueInfo.text = "任务被取消"
                binding.tvQueueInfo.visibility = View.VISIBLE
            }
        }
    }

    private fun submitTaskWithStateCallback() {
        log("=== 提交带状态回调的任务 ===")
        
        currentTask = task {
            name = "StateCallbackTask"
            priority = TaskPriority.NORMAL
            block(suspend {
                log("任务开始执行")
                withContext(Dispatchers.IO) {
                    for (i in 1..5) {
                        delay(500)
                        log("任务执行中... 步骤 $i/5")
                    }
                }
                log("任务执行完毕")
                "Task Completed"
            })
            onSuccess { result ->
                log("成功: $result")
            }
            onError { error ->
                log("错误: ${error.message}")
                Log.e(StateCallbackActivity::class.simpleName, "submitTaskWithStateCallback: ${error.message}")
            }
            onStateChange { state ->
                log("状态变化: ${getStateName(state)}")
                runOnUiThread { updateStateDisplay(state) }
            }
        }
        
        TaskManager.submitSerial(currentTask!!)
        log("任务已提交，ID: ${currentTask!!.id}")
    }

    private fun submitMultipleTasks() {
        log("=== 提交多个任务观察队列位置 ===")
        
        for (i in 1..3) {
            val task = Task(
                name = "Task_$i",
                priority = TaskPriority.NORMAL,
                block = {
                    withContext(Dispatchers.IO) {
                        delay(1000)
                    }
                    "Task_$i Result"
                },
                onSuccess = { result ->
                    log("$result 完成")
                },
                onStateChange = { state ->
                    if (state is TaskState.Pending) {
                        log("Task_$i 等待中，位置: ${state.queuePosition}/${state.queueSize}")
                    } else if (state is TaskState.Running && state.progress == 0f) {
                        log("Task_$i 开始执行")
                    }
                }
            )
            TaskManager.submitSerial(task)
            log("Task_$i 已提交")
        }
    }

    private fun cancelCurrentTask() {
        currentTask?.let { task ->
            if (task.currentState !is TaskState.Completed && task.currentState !is TaskState.Cancelled) {
                task.cancel()
                log("已请求取消任务: ${task.id}")
            } else {
                log("任务已结束，无法取消")
            }
        } ?: log("没有正在执行的任务")
    }

    private fun getStateName(state: TaskState): String {
        return when (state) {
            is TaskState.Pending -> "Pending (位置: ${state.queuePosition}/${state.queueSize})"
            is TaskState.Running -> "Running (进度: ${(state.progress * 100).toInt()}%)"
            is TaskState.Completed -> "Completed (成功: ${state.success})"
            is TaskState.Cancelled -> "Cancelled"
        }
    }
}
