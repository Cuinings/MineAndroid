package com.cn.other.test

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.cn.core.utils.Debounce.debounce
import com.cn.core.utils.isNetworkAvailable
import com.cn.core.utils.networkAvailable
import com.cn.other.test.spinner.Spinner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.Permissions

class MainActivity : AppCompatActivity() {

    var networkText: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val spinner = findViewById<Spinner>(R.id.spinner)
        spinner.setData(mutableListOf<String>().apply {
            repeat(20) {
                add("value -> $it")
            }
        })
        requestPermissions(arrayOf(
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
        ), 0)
        networkText = findViewById<TextView>(R.id.networkResult)
        debounce(10_000, { spinner.id }, lifecycleScope)
        IDefaultNetworkCallback(this@MainActivity, lifecycleScope)
    }

    fun finishActivity(view: View) {
    }

    fun network1(view: View) {

        lifecycleScope.launch {
            this@MainActivity.networkAvailable().collect {
//                if (it) finish()
                Log.d(MainActivity::class.simpleName, "network1: $it")
            }
        }
    }
    fun network2(view: View) {
        networkText?.text = "检测中..."
        lifecycleScope.launch {
            view.isEnabled = false
            this@MainActivity.isNetworkAvailable().collect {
//                if (it) finish()
                networkText?.text = StringBuilder("网络状态：").append(if (it) "正常" else "异常").toString()
                Log.d(MainActivity::class.simpleName, "network2: $it")
                delay(1000)
                networkText?.text = "等待开始检测网络状态"
                view.isEnabled = true
            }
        }
    }
}