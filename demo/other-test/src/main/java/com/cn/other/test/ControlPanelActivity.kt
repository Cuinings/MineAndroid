package com.cn.other.test

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ControlPanelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_control_panel)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.control_panel)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}