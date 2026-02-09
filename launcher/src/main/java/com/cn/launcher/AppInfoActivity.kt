package com.cn.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AppInfoActivity : AppCompatActivity() {

    private lateinit var appIcon: ImageView
    private lateinit var appName: TextView
    private lateinit var packageName: TextView
    private lateinit var versionName: TextView
    private lateinit var versionCode: TextView
    private lateinit var btnOpen: Button
    private lateinit var btnUninstall: Button
    private lateinit var btnAppSettings: Button
    private lateinit var btnClose: Button

    private lateinit var appInfo: AppInfo

    private lateinit var btnHide: Button
    private lateinit var category: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_info)

        // 初始化视图
        appIcon = findViewById(R.id.app_icon)
        appName = findViewById(R.id.app_name)
        packageName = findViewById(R.id.package_name)
        versionName = findViewById(R.id.version_name)
        versionCode = findViewById(R.id.version_code)
        category = findViewById(R.id.category)
        btnOpen = findViewById(R.id.btn_open)
        btnUninstall = findViewById(R.id.btn_uninstall)
        btnAppSettings = findViewById(R.id.btn_app_settings)
        btnHide = findViewById(R.id.btn_hide)
        btnClose = findViewById(R.id.btn_close)

        // 获取传递的应用信息
        appInfo = intent.getSerializableExtra("appInfo") as AppInfo

        // 设置应用信息
        // 通过包名重新加载应用图标，因为appIcon在序列化时被跳过了
        try {
            val pm = packageManager
            val applicationInfo = pm.getApplicationInfo(appInfo.packageName, 0)
            val icon = pm.getApplicationIcon(applicationInfo)
            appIcon.setImageDrawable(icon)
        } catch (e: Exception) {
            // 如果加载失败，使用默认图标
            appIcon.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        appName.text = appInfo.appName
        packageName.text = "包名: ${appInfo.packageName}"
        category.text = "类别: ${appInfo.category}"

        // 获取应用版本信息
        try {
            val pm = packageManager
            val applicationInfo = pm.getApplicationInfo(appInfo.packageName, 0)
            val packageInfo = pm.getPackageInfo(applicationInfo.packageName, 0)
            versionName.text = "版本: ${packageInfo.versionName}"
            versionCode.text = "版本号: ${packageInfo.versionCode}"
        } catch (e: Exception) {
            versionName.text = "版本: 未知"
            versionCode.text = "版本号: 未知"
        }

        // 设置隐藏按钮文本
        updateHideButtonText()

        // 设置按钮点击事件
        btnOpen.setOnClickListener {
            openApp()
        }

        btnUninstall.setOnClickListener {
            uninstallApp()
        }

        btnAppSettings.setOnClickListener {
            openAppSettings()
        }

        btnHide.setOnClickListener {
            toggleAppVisibility()
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun updateHideButtonText() {
        val sharedPreferences = getSharedPreferences("launcher_settings", MODE_PRIVATE)
        val isHidden = sharedPreferences.getBoolean("hidden_${appInfo.packageName}", false)
        btnHide.text = if (isHidden) "显示应用" else "隐藏应用"
    }

    private fun toggleAppVisibility() {
        val sharedPreferences = getSharedPreferences("launcher_settings", MODE_PRIVATE)
        val isHidden = sharedPreferences.getBoolean("hidden_${appInfo.packageName}", false)
        val newState = !isHidden
        
        sharedPreferences.edit().putBoolean("hidden_${appInfo.packageName}", newState).apply()
        updateHideButtonText()
        
        Toast.makeText(this, if (newState) "应用已隐藏" else "应用已显示", Toast.LENGTH_SHORT).show()
        
        // 关闭当前活动，返回启动器
        finish()
    }

    private fun openApp() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName(appInfo.packageName, appInfo.activityName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "无法启动应用: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallApp() {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:${appInfo.packageName}")
        startActivityForResult(intent, UNINSTALL_REQUEST_CODE)
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${appInfo.packageName}")
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UNINSTALL_REQUEST_CODE) {
            // 卸载完成后关闭活动
            finish()
        }
    }

    companion object {
        private const val UNINSTALL_REQUEST_CODE = 1001
    }
}
