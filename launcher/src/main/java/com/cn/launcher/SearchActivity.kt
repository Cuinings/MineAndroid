package com.cn.launcher

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var voiceSearchButton: Button
    private lateinit var recentAppsListView: ListView
    private lateinit var searchResultsListView: ListView

    private lateinit var recentAppsAdapter: ArrayAdapter<String>
    private lateinit var searchResultsAdapter: ArrayAdapter<AppInfo>

    private val recentApps = mutableListOf<String>()
    private val searchResults = mutableListOf<AppInfo>()
    private val ALL_APPS = mutableListOf<AppInfo>()

    private val REQUEST_VOICE_SEARCH = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // 初始化视图
        searchEditText = findViewById(R.id.search_edit_text)
        searchButton = findViewById(R.id.search_button)
        voiceSearchButton = findViewById(R.id.voice_search_button)
        recentAppsListView = findViewById(R.id.recent_apps_list)
        searchResultsListView = findViewById(R.id.search_results_list)

        // 加载所有应用
        loadAllApps()

        // 加载最近应用
        loadRecentApps()

        // 初始化适配器
        recentAppsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, recentApps)
        recentAppsListView.adapter = recentAppsAdapter

        searchResultsAdapter = AppAdapter(this, searchResults)
        searchResultsListView.adapter = searchResultsAdapter

        // 设置按钮点击事件
        searchButton.setOnClickListener {
            performSearch()
        }

        voiceSearchButton.setOnClickListener {
            startVoiceSearch()
        }

        // 设置列表项点击事件
        recentAppsListView.setOnItemClickListener { _, _, position, _ ->
            val appName = recentApps[position]
            // 启动对应的应用
            launchAppFromName(appName)
        }

        searchResultsListView.setOnItemClickListener { _, _, position, _ ->
            val appInfo = searchResults[position]
            // 启动应用并添加到最近应用
            launchApp(appInfo)
            addToRecentApps(appInfo.appName)
        }
    }

    private fun loadAllApps() {
        val packageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val activities = packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in activities) {
            val appInfo = AppInfo(
                resolveInfo.loadLabel(packageManager).toString(),
                resolveInfo.activityInfo.packageName,
                resolveInfo.activityInfo.name,
                resolveInfo.loadIcon(packageManager)
            )
            ALL_APPS.add(appInfo)
        }
    }

    private fun loadRecentApps() {
        // 这里应该从存储中加载最近应用，这里使用模拟数据
        recentApps.add("设置")
        recentApps.add("相机")
        recentApps.add("浏览器")
    }

    private fun performSearch() {
        val query = searchEditText.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show()
            return
        }

        // 清空之前的搜索结果
        searchResults.clear()

        // 搜索应用
        for (appInfo in ALL_APPS) {
            if (appInfo.appName.lowercase().contains(query.lowercase()) ||
                appInfo.packageName.lowercase().contains(query.lowercase())) {
                searchResults.add(appInfo)
            }
        }

        // 更新搜索结果
        searchResultsAdapter.notifyDataSetChanged()

        // 如果有搜索结果，滚动到搜索结果列表
        if (searchResults.isNotEmpty()) {
            searchResultsListView.visibility = View.VISIBLE
            recentAppsListView.visibility = View.GONE
        }
    }

    private fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出应用名称")

        try {
            startActivityForResult(intent, REQUEST_VOICE_SEARCH)
        } catch (e: Exception) {
            Toast.makeText(this, "语音搜索不可用: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(appInfo: AppInfo) {
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

    private fun launchAppFromName(appName: String) {
        for (appInfo in ALL_APPS) {
            if (appInfo.appName == appName) {
                launchApp(appInfo)
                return
            }
        }
        Toast.makeText(this, "应用未找到", Toast.LENGTH_SHORT).show()
    }

    private fun addToRecentApps(appName: String) {
        // 移除已存在的相同应用
        recentApps.remove(appName)
        // 添加到列表开头
        recentApps.add(0, appName)
        // 限制最近应用数量
        if (recentApps.size > 5) {
            recentApps.removeAt(recentApps.size - 1)
        }
        // 更新适配器
        recentAppsAdapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VOICE_SEARCH && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                searchEditText.setText(spokenText)
                performSearch()
            }
        }
    }
}
