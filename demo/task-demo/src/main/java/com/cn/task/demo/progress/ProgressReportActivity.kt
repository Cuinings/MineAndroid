package com.cn.task.demo.progress

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cn.core.task.ProgressReporter
import com.cn.core.task.Task
import com.cn.core.task.TaskPriority
import com.cn.core.task.TaskState
import com.cn.task.demo.databinding.ActivityProgressReportBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressReportActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProgressReportBinding
    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private var currentTask: Task<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnStartDownload.setOnClickListener {
            simulateFileDownload()
        }
        
        binding.btnStartProcess.setOnClickListener {
            simulateDataProcessing()
        }
        
        binding.btnCancel.setOnClickListener {
            cancelTask()
        }
        
        binding.btnClear.setOnClickListener {
            logBuilder.clear()
            binding.tvLog.text = ""
            resetProgress()
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

    private fun updateProgress(progress: Float, message: String?) {
        runOnUiThread {
            binding.progressBar.progress = (progress * 100).toInt()
            binding.tvProgress.text = "${(progress * 100).toInt()}%"
            binding.tvMessage.text = message ?: ""
        }
    }

    private fun resetProgress() {
        binding.progressBar.progress = 0
        binding.tvProgress.text = "0%"
        binding.tvMessage.text = ""
    }

    private fun simulateFileDownload() {
        log("=== 开始模拟文件下载 ===")
        resetProgress()
        
        currentTask = Task(
            name = "FileDownload",
            priority = TaskPriority.HIGH,
            block = { progress: ProgressReporter ->
                withContext(Dispatchers.IO) {
                    for (i in 1..100) {
                        delay(30)
                        progress.report(i / 100f, "下载中... $i%")
                    }
                }
                "/path/to/downloaded/file.zip"
            },
            onSuccess = { result ->
                log("下载完成: $result")
                updateProgress(1f, "下载完成")
            },
            onError = { error ->
                log("下载失败: ${error.message}")
            },
            onStateChange = { state ->
                when (state) {
                    is TaskState.Running -> {
                        updateProgress(state.progress, state.message)
                        if (state.progress == 0f) {
                            log("开始下载...")
                        }
                    }
                    is TaskState.Cancelled -> {
                        log("下载已取消")
                        updateProgress(0f, "已取消")
                    }
                    else -> {}
                }
            }
        )
        
        com.cn.core.task.TaskManager.submitSerial(currentTask!!)
    }

    private fun simulateDataProcessing() {
        log("=== 开始模拟数据处理 ===")
        resetProgress()
        
        currentTask = com.cn.core.task.task {
            name = "DataProcessing"
            priority = TaskPriority.NORMAL
            block { progress ->
                withContext(Dispatchers.IO) {
                    val steps = listOf(
                        "读取数据" to 0.2f,
                        "解析数据" to 0.4f,
                        "转换格式" to 0.6f,
                        "验证数据" to 0.8f,
                        "保存结果" to 1.0f
                    )
                    
                    var currentProgress = 0f
                    for ((stepName, targetProgress) in steps) {
                        while (currentProgress < targetProgress) {
                            delay(50)
                            currentProgress += 0.02f
                            progress.report(currentProgress.coerceAtMost(1f), stepName)
                        }
                        log("完成: $stepName")
                    }
                }
                "Processed 1000 records"
            }
            onSuccess { result ->
                log("处理完成: $result")
                updateProgress(1f, "处理完成")
            }
            onError { error ->
                log("处理失败: ${error.message}")
            }
            onStateChange { state ->
                when (state) {
                    is TaskState.Running -> {
                        updateProgress(state.progress, state.message)
                    }
                    is TaskState.Cancelled -> {
                        log("处理已取消")
                        updateProgress(0f, "已取消")
                    }
                    else -> {}
                }
            }
        }
        
        com.cn.core.task.TaskManager.submitSerial(currentTask!!)
    }

    private fun cancelTask() {
        currentTask?.let { task ->
            val state = task.currentState
            if (state is TaskState.Running || state is TaskState.Pending) {
                task.cancel()
                log("已请求取消任务")
            } else {
                log("任务已结束")
            }
        } ?: log("没有正在执行的任务")
    }
}
