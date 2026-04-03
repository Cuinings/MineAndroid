//package com.cn.core.ui.view.globalblur
//
//import android.graphics.Color
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.Toast
//import com.cn.core.ui.fragment.BasicFragment
//
//class GlobalBlurFragmentExample : BasicFragment() {
//
//    private lateinit var blurHelper: GlobalBlurHelper
//    private var isBlurWindowVisible = false
//    private var isInitialized = false
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return createContentView()
//    }
//
//    private fun createContentView(): View {
//        val layout = android.widget.LinearLayout(requireContext()).apply {
//            orientation = android.widget.LinearLayout.VERTICAL
//            setPadding(16, 16, 16, 16)
//        }
//
//        val createButton = Button(requireContext()).apply {
//            text = "创建模糊窗口"
//            setOnClickListener {
//                if (isInitialized && blurHelper.isReady()) {
//                    createBlurWindow()
//                } else {
//                    Toast.makeText(
//                        requireContext(),
//                        "请等待初始化完成或授权权限",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//
//        val updateButton = Button(requireContext()).apply {
//            text = "更新模糊效果"
//            setOnClickListener {
//                updateBlurEffect()
//            }
//        }
//
//        val moveButton = Button(requireContext()).apply {
//            text = "移动窗口"
//            setOnClickListener {
//                moveBlurWindow()
//            }
//        }
//
//        val toggleButton = Button(requireContext()).apply {
//            text = "显示/隐藏"
//            setOnClickListener {
//                toggleBlurWindow()
//            }
//        }
//
//        val removeButton = Button(requireContext()).apply {
//            text = "移除窗口"
//            setOnClickListener {
//                removeBlurWindow()
//            }
//        }
//
//        layout.addView(createButton)
//        layout.addView(updateButton)
//        layout.addView(moveButton)
//        layout.addView(toggleButton)
//        layout.addView(removeButton)
//
//        return layout
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        initializeBlurHelper()
//    }
//
//    private fun initializeBlurHelper() {
//        activity?.let { activity ->
//            blurHelper = GlobalBlurHelper(activity)
//            blurHelper.initialize()
//            isInitialized = true
//        }
//    }
//
//    fun handleActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
//        if (::blurHelper.isInitialized) {
//            blurHelper.handleActivityResult(requestCode, resultCode, data)
//
//            if (blurHelper.isReady()) {
//                Toast.makeText(requireContext(), "全局模糊服务已就绪", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun createBlurWindow() {
//        val windowId = "fragment_blur_window"
//
//        if (blurHelper.hasBlurWindow(windowId)) {
//            Toast.makeText(requireContext(), "模糊窗口已存在", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val screenWidth = resources.displayMetrics.widthPixels
//        val screenHeight = resources.displayMetrics.heightPixels
//
//        val success = blurHelper.createBlurWindow(
//            id = windowId,
//            x = screenWidth / 6,
//            y = screenHeight / 4,
//            width = screenWidth * 2 / 3,
//            height = 250,
//            blurRadius = 18f,
//            cornerRadius = 20f,
//            overlayColor = Color.parseColor("#60000000"),
//            autoUpdate = true,
//            updateInterval = 150L
//        )
//
//        if (success) {
//            isBlurWindowVisible = true
//            Toast.makeText(requireContext(), "模糊窗口创建成功", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(requireContext(), "模糊窗口创建失败", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun updateBlurEffect() {
//        val windowId = "fragment_blur_window"
//
//        if (!blurHelper.hasBlurWindow(windowId)) {
//            Toast.makeText(requireContext(), "请先创建模糊窗口", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        blurHelper.updateBlurRadius(windowId, 22f)
//        blurHelper.updateCornerRadius(windowId, 28f)
//        blurHelper.updateOverlayColor(windowId, Color.parseColor("#50000000"))
//
//        Toast.makeText(requireContext(), "模糊效果已更新", Toast.LENGTH_SHORT).show()
//    }
//
//    private fun moveBlurWindow() {
//        val windowId = "fragment_blur_window"
//
//        if (!blurHelper.hasBlurWindow(windowId)) {
//            Toast.makeText(requireContext(), "请先创建模糊窗口", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val screenWidth = resources.displayMetrics.widthPixels
//        val screenHeight = resources.displayMetrics.heightPixels
//
//        val newX = (Math.random() * (screenWidth - 200)).toInt()
//        val newY = (Math.random() * (screenHeight - 250)).toInt()
//
//        blurHelper.updateBlurWindowPosition(windowId, newX, newY)
//
//        Toast.makeText(requireContext(), "窗口已移动到新位置", Toast.LENGTH_SHORT).show()
//    }
//
//    private fun toggleBlurWindow() {
//        val windowId = "fragment_blur_window"
//
//        if (!blurHelper.hasBlurWindow(windowId)) {
//            Toast.makeText(requireContext(), "请先创建模糊窗口", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        if (isBlurWindowVisible) {
//            blurHelper.hideBlurWindow(windowId)
//            isBlurWindowVisible = false
//            Toast.makeText(requireContext(), "窗口已隐藏", Toast.LENGTH_SHORT).show()
//        } else {
//            blurHelper.showBlurWindow(windowId)
//            isBlurWindowVisible = true
//            Toast.makeText(requireContext(), "窗口已显示", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun removeBlurWindow() {
//        val windowId = "fragment_blur_window"
//
//        if (blurHelper.hasBlurWindow(windowId)) {
//            blurHelper.removeBlurWindow(windowId)
//            isBlurWindowVisible = false
//            Toast.makeText(requireContext(), "窗口已移除", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(requireContext(), "窗口不存在", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        if (::blurHelper.isInitialized && blurHelper.hasBlurWindow("fragment_blur_window")) {
//            blurHelper.hideBlurWindow("fragment_blur_window")
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (::blurHelper.isInitialized && blurHelper.hasBlurWindow("fragment_blur_window") && isBlurWindowVisible) {
//            blurHelper.showBlurWindow("fragment_blur_window")
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        if (::blurHelper.isInitialized) {
//            blurHelper.removeBlurWindow("fragment_blur_window")
//        }
//    }
//
//    companion object {
//        fun newInstance(): GlobalBlurFragmentExample {
//            return GlobalBlurFragmentExample()
//        }
//    }
//}
