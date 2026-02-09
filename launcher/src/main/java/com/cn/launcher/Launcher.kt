package com.cn.launcher

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.DragEvent
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat

class Launcher : AppCompatActivity() {

    private lateinit var gridView: GridView
    private lateinit var appDrawerLayout: LinearLayout
    private lateinit var btnToggleDrawer: Button
    private lateinit var appList: MutableList<AppInfo>
    private lateinit var drawerAppList: MutableList<AppInfo>
    private lateinit var homeScreenPages: MutableList<MutableList<Any>> // 可以包含 AppInfo、FolderInfo、WidgetInfo 或 ShortcutInfo
    private var currentPage = 0
    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        gridView = findViewById(R.id.app_grid)
        appDrawerLayout = findViewById(R.id.app_drawer_layout)
        btnToggleDrawer = findViewById(R.id.btn_toggle_drawer)
        appList = mutableListOf()
        drawerAppList = mutableListOf()
        homeScreenPages = mutableListOf()

        // 初始化手势检测器
        gestureDetector = GestureDetectorCompat(this, GestureListener())

        // 初始化主屏幕页面
        homeScreenPages.add(mutableListOf())

        loadAppList()
        setupGridView()
        setupAppDrawer()
        setupToggleButton()
        setupDragAndDrop()
        setupWidgetSupport()
        setupShortcutSupport()
        setupWallpaperSupport()
        setupSearchSupport()
        setupGestureSupport()
        setupMultiScreenSupport()
        setupSettingsSupport()

        // 设置触摸监听器
        gridView.setOnTouchListener {
            _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun loadAppList() {
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
            
            // 分配应用类别
            assignAppCategory(appInfo)
            
            // 检查是否隐藏
            appInfo.isHidden = isAppHidden(appInfo.packageName)
            
            appList.add(appInfo)
            if (!appInfo.isHidden) {
                drawerAppList.add(appInfo)
            }
        }

        // 按应用名称排序
        sortAppsByOption()

        // 检查应用更新
        checkAppUpdates()

        // 添加一些应用到主屏幕
        if (appList.size > 0) {
            val firstPage = homeScreenPages[0]
            // 添加前 12 个非隐藏应用到主屏幕
            var count = 0
            for (appInfo in appList) {
                if (!appInfo.isHidden && count < 12) {
                    firstPage.add(appInfo)
                    count++
                }
            }
        }
    }

    private fun sortAppsByOption() {
        // 根据设置的排序选项排序应用
        val sharedPreferences = getSharedPreferences("launcher_settings", MODE_PRIVATE)
        val sortOption = sharedPreferences.getInt("app_sort_option", 0) // 0-按名称，1-按类别，2-按安装时间

        when (sortOption) {
            0 -> drawerAppList.sortBy { it.appName }
            1 -> drawerAppList.sortBy { it.category }
            2 -> drawerAppList.sortBy { it.packageName } // 简化处理，实际应该按安装时间
        }
    }

    private fun checkAppUpdates() {
        // 检查应用更新
        // 简化处理，只是模拟检查更新
        Thread {
            try {
                // 模拟网络请求延迟
                Thread.sleep(1000)
                // 检查完成后在主线程显示结果
                runOnUiThread {
                    Toast.makeText(this, "应用更新检查完成", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun setAppSortOption(option: Int) {
        // 设置应用排序选项
        val sharedPreferences = getSharedPreferences("launcher_settings", MODE_PRIVATE)
        sharedPreferences.edit().putInt("app_sort_option", option).apply()
        // 重新排序应用
        reloadAppList()
        Toast.makeText(this, "应用排序方式已更新", Toast.LENGTH_SHORT).show()
    }

    private fun assignAppCategory(appInfo: AppInfo) {
        // 根据包名分配应用类别
        val packageName = appInfo.packageName
        when {
            packageName.contains("camera") || packageName.contains("photo") || packageName.contains("gallery") -> {
                appInfo.category = "相机"
            }
            packageName.contains("browser") || packageName.contains("chrome") || packageName.contains("firefox") -> {
                appInfo.category = "浏览器"
            }
            packageName.contains("mail") || packageName.contains("email") -> {
                appInfo.category = "邮件"
            }
            packageName.contains("music") || packageName.contains("player") -> {
                appInfo.category = "音乐"
            }
            packageName.contains("video") || packageName.contains("player") -> {
                appInfo.category = "视频"
            }
            packageName.contains("game") || packageName.contains("gaming") -> {
                appInfo.category = "游戏"
            }
            packageName.contains("social") || packageName.contains("wechat") || packageName.contains("qq") || packageName.contains("facebook") || packageName.contains("twitter") -> {
                appInfo.category = "社交"
            }
            packageName.contains("map") || packageName.contains("navigation") -> {
                appInfo.category = "地图"
            }
            packageName.contains("launcher") -> {
                appInfo.category = "启动器"
            }
            else -> {
                appInfo.category = "其他"
            }
        }
    }

    private fun isAppHidden(packageName: String): Boolean {
        // 检查应用是否被隐藏
        val sharedPreferences = getSharedPreferences("launcher_settings", MODE_PRIVATE)
        return sharedPreferences.getBoolean("hidden_$packageName", false)
    }

    private fun setAppHidden(packageName: String, hidden: Boolean) {
        // 设置应用是否隐藏
        val sharedPreferences = getSharedPreferences("launcher_settings", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("hidden_$packageName", hidden).apply()
        // 重新加载应用列表
        reloadAppList()
    }

    private fun reloadAppList() {
        // 重新加载应用列表
        appList.clear()
        drawerAppList.clear()
        loadAppList()
        setupGridView()
        setupAppDrawer()
    }

    private fun setupGridView() {
        applyGridSizeSetting()
        updateHomeScreen()

        gridView.onItemClickListener = AdapterView.OnItemClickListener {
            _, _, position, _ ->
            val currentPageItems = homeScreenPages[currentPage]
            if (position < currentPageItems.size) {
                val item = currentPageItems[position]
                when (item) {
                    is AppInfo -> launchApp(item)
                    is FolderInfo -> openFolder(item)
                    is WidgetInfo -> Toast.makeText(this, "点击小部件", Toast.LENGTH_SHORT).show()
                    is ShortcutInfo -> launchShortcut(item)
                }
            }
        }

        // 长按应用图标显示快捷操作
        gridView.onItemLongClickListener = AdapterView.OnItemLongClickListener {
            _, view, position, _ ->
            val currentPageItems = homeScreenPages[currentPage]
            if (position < currentPageItems.size) {
                val item = currentPageItems[position]
                when (item) {
                    is AppInfo -> {
                        // 显示应用信息面板
                        showAppInfo(item)
                    }
                    else -> {
                        // 开始拖拽
                        view.startDragAndDrop(null, View.DragShadowBuilder(view), position, 0)
                    }
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupAppDrawer() {
        val drawerGridView = findViewById<GridView>(R.id.drawer_app_grid)
        applyGridSizeSetting(drawerGridView)
        val drawerAdapter = AppAdapter(this, drawerAppList)
        drawerGridView.adapter = drawerAdapter

        drawerGridView.onItemClickListener = AdapterView.OnItemClickListener {
            _, _, position, _ ->
            val appInfo = drawerAppList[position]
            launchApp(appInfo)
            // 启动应用后关闭应用抽屉
            toggleAppDrawer()
        }

        // 长按应用图标添加到主屏幕
        drawerGridView.onItemLongClickListener = AdapterView.OnItemLongClickListener {
            _, view, position, _ ->
            val appInfo = drawerAppList[position]
            // 添加应用到当前主屏幕页面
            addAppToHomeScreen(appInfo)
            // 关闭应用抽屉
            toggleAppDrawer()
            true
        }
    }

    private fun applyGridSizeSetting() {
        val sharedPreferences = getSharedPreferences("launcher_settings", MODE_PRIVATE)
        val gridSize = sharedPreferences.getInt("grid_size", 3) + 2 // 范围：2-7
        gridView.numColumns = gridSize
    }

    private fun applyGridSizeSetting(gridView: GridView) {
        val sharedPreferences = getSharedPreferences("launcher_settings", MODE_PRIVATE)
        val gridSize = sharedPreferences.getInt("grid_size", 3) + 2 // 范围：2-7
        gridView.numColumns = gridSize
    }

    private fun showAppInfo(appInfo: AppInfo) {
        val intent = Intent(this, AppInfoActivity::class.java)
        intent.putExtra("appInfo", appInfo)
        startActivity(intent)
    }

    private fun setupToggleButton() {
        btnToggleDrawer.setOnClickListener {
            toggleAppDrawer()
        }

        // 关闭应用抽屉按钮
        val btnCloseDrawer = findViewById<Button>(R.id.btn_close_drawer)
        btnCloseDrawer.setOnClickListener {
            toggleAppDrawer()
        }
    }

    private fun setupDragAndDrop() {
        gridView.setOnDragListener {
            view, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    view.setBackgroundResource(android.R.drawable.list_selector_background)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    view.setBackgroundResource(android.R.color.transparent)
                    true
                }
                DragEvent.ACTION_DROP -> {
                    val draggedPosition = event.localState as Int
                    val currentPageItems = homeScreenPages[currentPage]
                    if (draggedPosition < currentPageItems.size) {
                        val draggedItem = currentPageItems[draggedPosition]
                        // 处理应用图标拖拽
                        handleDrop(draggedItem, draggedPosition)
                    }
                    view.setBackgroundResource(android.R.color.transparent)
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    view.setBackgroundResource(android.R.color.transparent)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun setupWidgetSupport() {
        // 添加小部件按钮
        val btnAddWidget = findViewById<Button>(R.id.btn_add_widget)
        btnAddWidget.setOnClickListener {
            showWidgetPicker()
        }
    }

    private fun setupShortcutSupport() {
        // 添加快捷方式按钮
        val btnAddShortcut = findViewById<Button>(R.id.btn_add_shortcut)
        btnAddShortcut.setOnClickListener {
            showShortcutPicker()
        }
    }

    private fun setupWallpaperSupport() {
        // 添加壁纸设置按钮
        val btnSetWallpaper = findViewById<Button>(R.id.btn_set_wallpaper)
        btnSetWallpaper.setOnClickListener {
            showWallpaperPicker()
        }
    }

    private fun setupSearchSupport() {
        // 添加搜索按钮
        val btnSearch = findViewById<Button>(R.id.btn_search)
        btnSearch.setOnClickListener {
            showSearchBar()
        }
    }

    private fun setupGestureSupport() {
        // 手势支持设置
        // 这里可以添加手势识别逻辑
        Toast.makeText(this, "手势支持已启用", Toast.LENGTH_SHORT).show()
    }

    private fun setupMultiScreenSupport() {
        // 多屏幕支持设置
        // 添加页面切换按钮
        val btnPrevPage = findViewById<Button>(R.id.btn_prev_page)
        val btnNextPage = findViewById<Button>(R.id.btn_next_page)

        btnPrevPage.setOnClickListener {
            switchToPreviousPage()
        }

        btnNextPage.setOnClickListener {
            switchToNextPage()
        }

        // 添加屏幕预览按钮
        val btnScreenPreview = findViewById<Button>(R.id.btn_screen_preview)
        btnScreenPreview.setOnClickListener {
            showScreenPreview()
        }
    }

    private fun showScreenPreview() {
        // 显示屏幕预览
        Toast.makeText(this, "显示屏幕预览", Toast.LENGTH_SHORT).show()
        // 这里可以实现屏幕预览的逻辑，显示所有主屏幕页面的缩略图
        // 简化处理，只是显示当前屏幕数量
        Toast.makeText(this, "当前有 ${homeScreenPages.size} 个屏幕", Toast.LENGTH_SHORT).show()
        // 显示屏幕管理选项
        Toast.makeText(this, "可执行操作: 添加屏幕、删除屏幕、重命名屏幕", Toast.LENGTH_SHORT).show()
    }

    private fun addScreen() {
        // 添加新屏幕
        homeScreenPages.add(mutableListOf())
        currentPage = homeScreenPages.size - 1
        updateHomeScreen()
        Toast.makeText(this, "已添加新屏幕", Toast.LENGTH_SHORT).show()
    }

    private fun removeScreen(pageIndex: Int) {
        // 删除屏幕
        if (homeScreenPages.size > 1) { // 至少保留一个屏幕
            homeScreenPages.removeAt(pageIndex)
            if (currentPage >= homeScreenPages.size) {
                currentPage = homeScreenPages.size - 1
            }
            updateHomeScreen()
            Toast.makeText(this, "已删除屏幕", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "至少需要保留一个屏幕", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveScreen(fromIndex: Int, toIndex: Int) {
        // 移动屏幕位置
        if (fromIndex in 0 until homeScreenPages.size && toIndex in 0 until homeScreenPages.size) {
            val screen = homeScreenPages.removeAt(fromIndex)
            homeScreenPages.add(toIndex, screen)
            currentPage = toIndex
            updateHomeScreen()
            Toast.makeText(this, "屏幕位置已调整", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSettingsSupport() {
        // 添加设置按钮
        val btnSettings = findViewById<Button>(R.id.btn_settings)
        btnSettings.setOnClickListener {
            showSettings()
        }
    }

    private fun showSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun handleDrop(draggedItem: Any, draggedPosition: Int) {
        // 这里可以实现更复杂的拖拽逻辑，比如创建文件夹
        when (draggedItem) {
            is AppInfo -> {
                Toast.makeText(this, "拖拽应用: ${draggedItem.appName}", Toast.LENGTH_SHORT).show()
                // 可以实现应用图标的重新排序
                val currentPageItems = homeScreenPages[currentPage]
                currentPageItems.removeAt(draggedPosition)
                currentPageItems.add(draggedPosition, draggedItem)
                updateHomeScreen()
            }
            is FolderInfo -> {
                Toast.makeText(this, "拖拽文件夹: ${draggedItem.folderName}", Toast.LENGTH_SHORT).show()
            }
            is WidgetInfo -> {
                Toast.makeText(this, "拖拽小部件: ${draggedItem.widgetName}", Toast.LENGTH_SHORT).show()
            }
            is ShortcutInfo -> {
                Toast.makeText(this, "拖拽快捷方式: ${draggedItem.shortcutName}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addAppToHomeScreen(appInfo: AppInfo) {
        val currentPageItems = homeScreenPages[currentPage]
        if (currentPageItems.size < 12) { // 4x3 网格
            currentPageItems.add(appInfo)
            updateHomeScreen()
            Toast.makeText(this, "已添加到主屏幕", Toast.LENGTH_SHORT).show()
        } else {
            // 如果当前页面已满，创建新页面
            val newPage = mutableListOf<Any>(appInfo)
            homeScreenPages.add(newPage)
            currentPage = homeScreenPages.size - 1
            updateHomeScreen()
            Toast.makeText(this, "已添加到新页面", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addWidgetToHomeScreen(widgetInfo: WidgetInfo) {
        val currentPageItems = homeScreenPages[currentPage]
        if (currentPageItems.size < 12) { // 4x3 网格
            currentPageItems.add(widgetInfo)
            updateHomeScreen()
            Toast.makeText(this, "已添加小部件到主屏幕", Toast.LENGTH_SHORT).show()
        } else {
            // 如果当前页面已满，创建新页面
            val newPage = mutableListOf<Any>(widgetInfo)
            homeScreenPages.add(newPage)
            currentPage = homeScreenPages.size - 1
            updateHomeScreen()
            Toast.makeText(this, "已添加小部件到新页面", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addShortcutToHomeScreen(shortcutInfo: ShortcutInfo) {
        val currentPageItems = homeScreenPages[currentPage]
        if (currentPageItems.size < 12) { // 4x3 网格
            currentPageItems.add(shortcutInfo)
            updateHomeScreen()
            Toast.makeText(this, "已添加快捷方式到主屏幕", Toast.LENGTH_SHORT).show()
        } else {
            // 如果当前页面已满，创建新页面
            val newPage = mutableListOf<Any>(shortcutInfo)
            homeScreenPages.add(newPage)
            currentPage = homeScreenPages.size - 1
            updateHomeScreen()
            Toast.makeText(this, "已添加快捷方式到新页面", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateHomeScreen() {
        val currentPageItems = homeScreenPages[currentPage]
        val adapter = HomeScreenAdapter(this, currentPageItems)
        gridView.adapter = adapter
    }

    private fun toggleAppDrawer() {
        if (appDrawerLayout.visibility == View.VISIBLE) {
            appDrawerLayout.visibility = View.GONE
        } else {
            appDrawerLayout.visibility = View.VISIBLE
        }
    }

    private fun launchApp(appInfo: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName(appInfo.packageName, appInfo.activityName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法启动应用: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchShortcut(shortcutInfo: ShortcutInfo) {
        // 启动快捷方式
        Toast.makeText(this, "启动快捷方式: ${shortcutInfo.shortcutName}", Toast.LENGTH_SHORT).show()
        // 这里可以实现快捷方式的启动逻辑
    }

    private fun openFolder(folderInfo: FolderInfo) {
        // 打开文件夹的逻辑
        Toast.makeText(this, "打开文件夹: ${folderInfo.folderName}", Toast.LENGTH_SHORT).show()
        // 这里可以实现文件夹打开的逻辑，显示文件夹中的应用
        // 简化处理，只是显示文件夹中的应用数量
        Toast.makeText(this, "文件夹包含 ${folderInfo.apps.size} 个应用", Toast.LENGTH_SHORT).show()
    }

    private fun createFolder(folderName: String): FolderInfo {
        // 创建新文件夹
        val folderInfo = FolderInfo(folderName, mutableListOf())
        return folderInfo
    }

    private fun renameFolder(folderInfo: FolderInfo, newName: String) {
        // 重命名文件夹
        folderInfo.rename(newName)
        updateHomeScreen()
        Toast.makeText(this, "文件夹已重命名为: $newName", Toast.LENGTH_SHORT).show()
    }

    private fun setFolderIcon(folderInfo: FolderInfo, icon: Drawable) {
        // 设置文件夹图标
        folderInfo.setIcon(icon)
        updateHomeScreen()
        Toast.makeText(this, "文件夹图标已更新", Toast.LENGTH_SHORT).show()
    }

    private fun sortFolderApps(folderInfo: FolderInfo, sortType: Int) {
        // 排序文件夹内的应用
        folderInfo.sortType = sortType
        folderInfo.sortApps()
        Toast.makeText(this, "文件夹应用已排序", Toast.LENGTH_SHORT).show()
    }

    private fun addAppToFolder(folderInfo: FolderInfo, appInfo: AppInfo) {
        // 添加应用到文件夹
        folderInfo.addApp(appInfo)
        updateHomeScreen()
        Toast.makeText(this, "应用已添加到文件夹", Toast.LENGTH_SHORT).show()
    }

    private fun removeAppFromFolder(folderInfo: FolderInfo, appInfo: AppInfo) {
        // 从文件夹中移除应用
        folderInfo.removeApp(appInfo)
        updateHomeScreen()
        Toast.makeText(this, "应用已从文件夹中移除", Toast.LENGTH_SHORT).show()
    }

    private fun showWidgetPicker() {
        // 显示小部件选择器
        Toast.makeText(this, "显示小部件选择器", Toast.LENGTH_SHORT).show()
        // 这里可以实现小部件选择器的逻辑
        // 模拟添加一个小部件
        val widgetInfo = WidgetInfo("时钟小部件", "com.android.deskclock", 1)
        addWidgetToHomeScreen(widgetInfo)
    }

    private fun resizeWidget(widgetInfo: WidgetInfo, newSize: Pair<Int, Int>) {
        // 调整小部件大小
        Toast.makeText(this, "调整小部件大小为 ${newSize.first}x${newSize.second}", Toast.LENGTH_SHORT).show()
        // 这里可以实现小部件大小调整的逻辑
    }

    private fun removeWidget(widgetInfo: WidgetInfo) {
        // 删除小部件
        for (page in homeScreenPages) {
            if (page.contains(widgetInfo)) {
                page.remove(widgetInfo)
                updateHomeScreen()
                Toast.makeText(this, "小部件已删除", Toast.LENGTH_SHORT).show()
                break
            }
        }
    }

    private fun configureWidget(widgetInfo: WidgetInfo) {
        // 配置小部件
        Toast.makeText(this, "配置小部件: ${widgetInfo.widgetName}", Toast.LENGTH_SHORT).show()
        // 这里可以实现小部件配置的逻辑
    }

    private fun showShortcutPicker() {
        // 显示快捷方式选择器
        Toast.makeText(this, "显示快捷方式选择器", Toast.LENGTH_SHORT).show()
        // 这里可以实现快捷方式选择器的逻辑
        // 模拟添加一个快捷方式
        val shortcutInfo =
            ShortcutInfo("相机快捷方式", "com.android.camera", "com.android.camera.CameraActivity")
        addShortcutToHomeScreen(shortcutInfo)
    }

    private fun showWallpaperPicker() {
        // 显示壁纸选择器
        Toast.makeText(this, "显示壁纸选择器", Toast.LENGTH_SHORT).show()
        // 这里可以实现壁纸选择器的逻辑
    }

    private fun showSearchBar() {
        // 启动搜索活动
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
    }

    private fun switchToPreviousPage() {
        if (currentPage > 0) {
            currentPage--
            updateHomeScreen()
            Toast.makeText(this, "切换到页面 ${currentPage + 1}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchToNextPage() {
        currentPage++
        if (currentPage >= homeScreenPages.size) {
            // 如果没有下一页，创建新页面
            homeScreenPages.add(mutableListOf())
        }
        updateHomeScreen()
        Toast.makeText(this, "切换到页面 ${currentPage + 1}", Toast.LENGTH_SHORT).show()
    }

    private fun minOf(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            var result = false
            try {
                val diffY = e2.y - (e1?.y ?: 0f)
                val diffX = e2.x - (e1?.x ?: 0f)
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // 向右滑动
                            onSwipeRight()
                        } else {
                            // 向左滑动
                            onSwipeLeft()
                        }
                        result = true
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        // 向下滑动
                        onSwipeDown()
                    } else {
                        // 向上滑动
                        onSwipeUp()
                    }
                    result = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTapDetected()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onLongPressDetected()
        }
    }

    private fun onSwipeRight() {
        // 向右滑动，切换到上一页
        switchToPreviousPage()
    }

    private fun onSwipeLeft() {
        // 向左滑动，切换到下一页
        switchToNextPage()
    }

    private fun onSwipeUp() {
        // 向上滑动，打开应用抽屉
        if (appDrawerLayout.visibility == View.GONE) {
            appDrawerLayout.visibility = View.VISIBLE
        }
    }

    private fun onSwipeDown() {
        // 向下滑动，关闭应用抽屉
        if (appDrawerLayout.visibility == View.VISIBLE) {
            appDrawerLayout.visibility = View.GONE
        }
    }

    private fun onDoubleTapDetected() {
        // 双击，显示设置界面
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun onLongPressDetected() {
        // 长按，显示快捷操作菜单
        Toast.makeText(this, "长按检测到", Toast.LENGTH_SHORT).show()
    }
}
