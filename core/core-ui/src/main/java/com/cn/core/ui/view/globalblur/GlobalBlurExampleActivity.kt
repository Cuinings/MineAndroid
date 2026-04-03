package com.cn.core.ui.view.globalblur

import android.graphics.Color
import android.os.Bundle
import com.cn.core.ui.activity.BasicActivity

class GlobalBlurExampleActivity : BasicActivity() {
    
    private lateinit var blurHelper: GlobalBlurHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        blurHelper = GlobalBlurHelper(this)
        blurHelper.initialize()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        blurHelper.handleActivityResult(requestCode, resultCode, data)
        
        if (blurHelper.isReady()) {
            showExampleBlurWindow()
        }
    }
    
    private fun showExampleBlurWindow() {
        val windowId = "example_blur"
        
        val success = blurHelper.createBlurWindow(
            id = windowId,
            x = 100,
            y = 200,
            width = 300,
            height = 200,
            blurRadius = 20f,
            cornerRadius = 16f,
            overlayColor = Color.parseColor("#80000000"),
            autoUpdate = true,
            updateInterval = 100L
        )
        
        if (success) {
            android.widget.Toast.makeText(this, "模糊窗口创建成功", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateBlurWindow() {
        blurHelper.updateBlurRadius("example_blur", 25f)
        blurHelper.updateCornerRadius("example_blur", 24f)
        blurHelper.updateOverlayColor("example_blur", Color.parseColor("#40000000"))
    }
    
    private fun moveBlurWindow() {
        blurHelper.updateBlurWindowPosition("example_blur", 200, 300)
    }
    
    private fun resizeBlurWindow() {
        blurHelper.updateBlurWindowSize("example_blur", 400, 300)
    }
    
    private fun hideBlurWindow() {
        blurHelper.hideBlurWindow("example_blur")
    }
    
    private fun showBlurWindow() {
        blurHelper.showBlurWindow("example_blur")
    }
    
    private fun removeBlurWindow() {
        blurHelper.removeBlurWindow("example_blur")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        blurHelper.release()
    }
}
