package com.cn.tosetting

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SettingAdapter
    private lateinit var tvPhoneInfo: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SettingExecutor.init {
            showErrorToast = true
            onResult { setting, success ->
                // 可以在这里添加日志或统计
            }
        }

        tvPhoneInfo = findViewById(R.id.tv_phone_info)
        recyclerView = findViewById(R.id.recycler_view)

        showPhoneInfo()

        val settings = settings {
            // 方式1: 使用所有设置
            // 默认就是所有设置，不需要额外配置
        }

        adapter = SettingAdapter(settings) { setting ->
            openSetting(setting)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun showPhoneInfo() {
        val brand = SettingHelper.getPhoneBrand(context = this@MainActivity)
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val brandStr = Build.BRAND
        val device = Build.DEVICE
        val sdkVersion = Build.VERSION.SDK_INT

        val info = StringBuilder()
        info.append("手机品牌: ${brand}\n")
        info.append("制造商: $manufacturer\n")
        info.append("型号: $model\n")
        info.append("品牌: $brandStr\n")
        info.append("设备: $device\n")
        info.append("SDK版本: $sdkVersion")

        tvPhoneInfo.text = info.toString()
    }
}
