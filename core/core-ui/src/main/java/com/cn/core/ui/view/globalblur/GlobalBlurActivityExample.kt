//package com.cn.core.ui.view.globalblur
//
//import android.content.Intent
//import android.graphics.Color
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import android.widget.Toast
//import com.cn.core.ui.activity.BasicActivity
//
//class GlobalBlurActivityExample : BasicActivity() {
//
//    private lateinit var blurHelper: GlobalBlurHelper
//    private var isBlurWindowVisible = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        setupButtons()
//        initializeBlurHelper()
//    }
//
//    private fun setupButtons() {
//        val createButton = Button(this).apply {
//            text = "创建模糊窗口"
//            setOnClickListener {
//                if (blurHelper.isReady()) {
//                    createBlurWindow()
//                } else {
//                    Toast.makeText(
//                        this@GlobalBlurActivityExample,
//                        "请先授权权限",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//
//        val updateButton = Button(this).apply {
//            text = "更新模糊效果"
//            setOnClickListener {
//                updateBlurEffect()
//            }
//        }
//
//        val moveButton = Button(this).apply {
//            text = "移动窗口"
//            setOnClickListener {
//                moveBlurWindow()
//            }
//        }
//
//        val toggleButton = Button(this).apply {
//            text = "显示/隐藏"
//            setOnClickListener {
//                toggleBlurWindow()
//            }
//        }
//
//        val removeButton = Button(this).apply {
//            text = "移除窗口"
//            setOnClickListener {
//                removeBlurWindow()
//            }
//        }
//    }
//
//    private fun initializeBlurHelper() {
//        blurHelper = GlobalBlurHelper(this)
//        blurHelper.initialize()
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        blurHelper.handleActivityResult(requestCode, resultCode, data)
//
//        if (blurHelper.isReady()) {
//            Toast.makeText(this, "全局模糊服务已就绪", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun createBlurWindow() {
//        val windowId = "activity_blur_window"
//
//        if (blurHelper.hasBlurWindow(windowId)) {
//            Toast.makeText(this, "模糊窗口已存在", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val screenWidth = resources.displayMetrics.widthPixels
//        val screenHeight = resources.displayMetrics.heightPixels
//
//        val success = blurHelper.createBlurWindow(
//            id = windowId,
//            x = screenWidth / 4,
//            y = screenHeight / 3,
//            width = screenWidth / 2,
//            height = 300,
//            blurRadius = 20f,
//            cornerRadius = 24f,
//            overlayColor = Color.parseColor("#80000000"),
//            autoUpdate = true,
//            updateInterval = 100L
//        )
//
//        if (success) {
//            isBlurWindowVisible = true
//            Toast.makeText(this, "模糊窗口创建成功", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(this, "模糊窗口创建失败", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun updateBlurEffect() {
//        val windowId = "activity_blur_window"
//
//        if (!blurHelper.hasBlurWindow(windowId)) {
//            Toast.makeText(this, "请先创建模糊窗口", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        blurHelper.updateBlurRadius(windowId, 25f)
//        blurHelper.updateCornerRadius(windowId, 32f)
//        blurHelper.updateOverlayColor(windowId, Color.parseColor("#40000000"))
//
//        Toast.makeText(this, "模糊效果已更新", Toast.LENGTH_SHORT).show()
//    }
//
//    private fun moveBlurWindow() {
//        val windowId = "activity_blur_window"
//
//        if (!blurHelper.hasBlurWindow(windowId)) {
//            Toast.makeText(this, "请先创建模糊窗口", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val screenWidth = resources.displayMetrics.widthPixels
//        val screenHeight = resources.displayMetrics.heightPixels
//
//        val newX = (Math.random() * (screenWidth - 300)).toInt()
//        val newY = (Math.random() * (screenHeight - 300)).toInt()
//
//        blurHelper.updateBlurWindowPosition(windowId, newX, newY)
//
//        Toast.makeText(this, "窗口已移动到新位置", Toast.LENGTH_SHORT).show()
//    }
//
//    private fun toggleBlurWindow() {
//        val windowId = "activity_blur_window"
//
//        if (!blurHelper.hasBlurWindow(windowId)) {
//            Toast.makeText(this, "请先创建模糊窗口", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        if (isBlurWindowVisible) {
//            blurHelper.hideBlurWindow(windowId)
//            isBlurWindowVisible = false
//            Toast.makeText(this, "窗口已隐藏", Toast.LENGTH_SHORT).show()
//        } else {
//            blurHelper.showBlurWindow(windowId)
//            isBlurWindowVisible = true
//            Toast.makeText(this, "窗口已显示", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun removeBlurWindow() {
//        val windowId = "activity_blur_window"
//
//        if (blurHelper.hasBlurWindow(windowId)) {
//            blurHelper.removeBlurWindow(windowId)
//            isBlurWindowVisible = false
//            Toast.makeText(this, "窗口已移除", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(this, "窗口不存在", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        if (::blurHelper.isInitialized && blurHelper.hasBlurWindow("activity_blur_window")) {
//            blurHelper.hideBlurWindow("activity_blur_window")
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (::blurHelper.isInitialized && blurHelper.hasBlurWindow("activity_blur_window") && isBlurWindowVisible) {
//            blurHelper.showBlurWindow("activity_blur_window")
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (::blurHelper.isInitialized) {
//            blurHelper.release()
//        }
//    }
//
//    companion object {
//        fun start(context: android.content.Context) {
//            val intent = Intent(context, GlobalBlurActivityExample::class.java)
//            context.startActivity(intent)
//        }
//    }
//}
