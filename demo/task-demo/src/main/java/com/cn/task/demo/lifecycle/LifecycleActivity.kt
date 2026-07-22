package com.cn.task.demo.lifecycle

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cn.core.task.Task
import com.cn.core.task.TaskPriority
import com.cn.core.task.TaskQueueScope
import com.cn.core.task.TaskState
import com.cn.task.demo.R
import com.cn.task.demo.databinding.ActivityLifecycleBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LifecycleActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLifecycleBinding
    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private lateinit var taskScope: TaskQueueScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLifecycleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        taskScope = TaskQueueScope.serial("lifecycle_demo")
        
        setupClickListeners()
        updateLifecycleStatus("已创建")
    }

    private fun setupClickListeners() {
        binding.btnStartLongTask.setOnClickListener {
            startLongTask()
        }
        
        binding.btnStartPeriodicTask.setOnClickListener {
            startPeriodicTask()
        }
        
        binding.btnOpenNewActivity.setOnClickListener {
            openNewActivity()
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

    private fun updateLifecycleStatus(status: String) {
        binding.tvLifecycleStatus.text = "Activity 状态: $status"
        binding.tvLifecycleStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
    }

    private fun startLongTask() {
        log("=== 启动长时间任务（10秒）===")
        
        val task = Task(
            name = "LongTask",
            priority = TaskPriority.NORMAL,
            block = {
                withContext(Dispatchers.IO) {
                    for (i in 1..10) {
                        delay(1000)
                        log("任务执行中... $i/10 秒")
                    }
                }
                "LongTask Completed"
            },
            onSuccess = { result ->
                log("✓ 任务完成: $result")
            },
            onError = { error ->
                log("✗ 任务错误: ${error.message}")
            },
            onComplete = {
                log("任务结束（onComplete 回调）")
            },
            onStateChange = { state ->
                when (state) {
                    is TaskState.Running -> {
                        if (state.progress == 0f) log("任务开始执行")
                    }
                    is TaskState.Cancelled -> {
                        log("⊗ 任务已取消")
                    }
                    else -> {}
                }
            }
        )
        
        taskScope.enqueue(task)
        log("任务已提交，ID: ${task.id}")
        log("提示：按返回键退出页面，任务将被取消")
    }

    private fun startPeriodicTask() {
        log("=== 启动周期性任务 ===")
        
        var counter = 0
        val task = Task(
            name = "PeriodicTask",
            priority = TaskPriority.LOW,
            block = {
                withContext(Dispatchers.IO) {
                    repeat(20) {
                        delay(500)
                        counter++
                        log("周期任务: 第 $counter 次执行")
                    }
                }
                "PeriodicTask Completed ($counter times)"
            },
            onSuccess = { result ->
                log("✓ 周期任务完成: $result")
            },
            onStateChange = { state ->
                when (state) {
                    is TaskState.Running -> {
                        if (state.progress == 0f) log("周期任务开始")
                    }
                    is TaskState.Cancelled -> {
                        log("⊗ 周期任务已取消（执行了 $counter 次）")
                    }
                    else -> {}
                }
            }
        )
        
        taskScope.enqueue(task)
        log("周期任务已提交")
    }

    private fun openNewActivity() {
        log("=== 打开新 Activity ===")
        log("当前任务将继续在后台执行...")
        log("返回时会取消所有任务")
        
        val intent = Intent(this, LifecycleTestActivity::class.java)
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        updateLifecycleStatus("已启动")
        log("Activity onStart")
    }

    override fun onResume() {
        super.onResume()
        updateLifecycleStatus("已恢复")
        log("Activity onResume")
    }

    override fun onPause() {
        super.onPause()
        updateLifecycleStatus("已暂停")
        log("Activity onPause")
    }

    override fun onStop() {
        super.onStop()
        updateLifecycleStatus("已停止")
        log("Activity onStop")
    }

    override fun onDestroy() {
        log("Activity onDestroy - 取消所有任务")
        taskScope.cancelAll()
        taskScope.shutdown()
        super.onDestroy()
    }
}

class LifecycleTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "测试页面 - 按返回键返回"
    }
}
