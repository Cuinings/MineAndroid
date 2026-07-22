package com.cn.task.demo.concurrent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cn.core.task.ConcurrentTaskQueue
import com.cn.core.task.ProgressReporter
import com.cn.core.task.Task
import com.cn.core.task.TaskPriority
import com.cn.core.task.TaskState
import com.cn.task.demo.databinding.ActivityConcurrentDownloadBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class ConcurrentDownloadActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityConcurrentDownloadBinding
    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private lateinit var downloadQueue: ConcurrentTaskQueue
    private val maxConcurrency = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConcurrentDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupQueue()
        setupClickListeners()
        startStatsUpdater()
    }

    private fun setupQueue() {
        val scope = lifecycleScope
        downloadQueue = ConcurrentTaskQueue(
            CoroutineScope(kotlinx.coroutines.SupervisorJob()),
            maxConcurrency = maxConcurrency
        )
    }

    private fun setupClickListeners() {
        binding.btnStartDownload.setOnClickListener {
            startDownloads()
        }
        
        binding.btnCancelAll.setOnClickListener {
            downloadQueue.cancelAll()
            log("已取消所有下载任务")
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

    private fun startStatsUpdater() {
        lifecycleScope.launch {
            while (true) {
                delay(100)
                updateStats()
            }
        }
    }

    private fun updateStats() {
        binding.tvActive.text = downloadQueue.runningTaskCount.toString()
        binding.tvPending.text = downloadQueue.pendingTaskCount.toString()
    }

    private fun startDownloads() {
        log("=== 开始并发下载 10 个文件 ===")
        log("最大并发数: $maxConcurrency")
        log("---")
        
        val fileNames = listOf(
            "image_001.jpg",
            "image_002.jpg",
            "image_003.jpg",
            "video_001.mp4",
            "video_002.mp4",
            "document_001.pdf",
            "document_002.pdf",
            "document_003.pdf",
            "audio_001.mp3",
            "archive_001.zip"
        )
        
        fileNames.forEachIndexed { index, fileName ->
            val task = Task(
                name = "Download_$fileName",
                priority = if (index < 3) TaskPriority.HIGH else TaskPriority.NORMAL,
                block = { progress: ProgressReporter ->
                    simulateDownload(fileName, progress)
                },
                onSuccess = { result ->
                    log("✓ 完成: $fileName -> $result")
                },
                onError = { error ->
                    log("✗ 失败: $fileName -> ${error.message}")
                },
                onStateChange = { state ->
                    when (state) {
                        is TaskState.Running -> {
                            if (state.progress == 0f) {
                                log("▶ 开始: $fileName")
                            }
                        }
                        is TaskState.Cancelled -> {
                            log("⊗ 取消: $fileName")
                        }
                        else -> {}
                    }
                }
            )
            
            downloadQueue.enqueue(task)
        }
        
        log("所有任务已加入队列")
    }

    private suspend fun simulateDownload(fileName: String, progress: ProgressReporter): String {
        return withContext(Dispatchers.IO) {
            val downloadTime = Random.nextLong(500, 2000)
            val steps = 10
            val stepTime = downloadTime / steps
            
            for (i in 1..steps) {
                delay(stepTime)
                progress.report(i / steps.toFloat(), "下载 $fileName: ${i * 10}%")
            }
            
            val fileSize = Random.nextLong(100, 10000)
            "${fileSize}KB"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadQueue.cancelAll()
    }
}
