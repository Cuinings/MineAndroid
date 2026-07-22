package com.cn.task.demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cn.task.demo.basic.BasicUsageActivity
import com.cn.task.demo.concurrent.ConcurrentDownloadActivity
import com.cn.task.demo.databinding.ActivityMainBinding
import com.cn.task.demo.lifecycle.LifecycleActivity
import com.cn.task.demo.priority.PriorityQueueActivity
import com.cn.task.demo.progress.ProgressReportActivity
import com.cn.task.demo.state.StateCallbackActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBasic.setOnClickListener {
            startActivity(Intent(this, BasicUsageActivity::class.java))
        }
        
        binding.btnState.setOnClickListener {
            startActivity(Intent(this, StateCallbackActivity::class.java))
        }
        
        binding.btnProgress.setOnClickListener {
            startActivity(Intent(this, ProgressReportActivity::class.java))
        }
        
        binding.btnPriority.setOnClickListener {
            startActivity(Intent(this, PriorityQueueActivity::class.java))
        }
        
        binding.btnConcurrent.setOnClickListener {
            startActivity(Intent(this, ConcurrentDownloadActivity::class.java))
        }
        
        binding.btnLifecycle.setOnClickListener {
            startActivity(Intent(this, LifecycleActivity::class.java))
        }
    }
}
