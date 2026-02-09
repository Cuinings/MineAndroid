package com.cn.launcher

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var iconSizeSeekBar: SeekBar
    private lateinit var fontSizeSeekBar: SeekBar
    private lateinit var gridSizeSeekBar: SeekBar
    private lateinit var iconSizeText: TextView
    private lateinit var fontSizeText: TextView
    private lateinit var gridSizeText: TextView

    private lateinit var sortOptionGroup: RadioGroup
    private lateinit var sortByName: RadioButton
    private lateinit var sortByCategory: RadioButton
    private lateinit var sortByTime: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("launcher_settings", MODE_PRIVATE)
        
        iconSizeSeekBar = findViewById(R.id.icon_size_seekbar)
        fontSizeSeekBar = findViewById(R.id.font_size_seekbar)
        gridSizeSeekBar = findViewById(R.id.grid_size_seekbar)
        iconSizeText = findViewById(R.id.icon_size_text)
        fontSizeText = findViewById(R.id.font_size_text)
        gridSizeText = findViewById(R.id.grid_size_text)
        sortOptionGroup = findViewById(R.id.sort_option_group)
        sortByName = findViewById(R.id.sort_by_name)
        sortByCategory = findViewById(R.id.sort_by_category)
        sortByTime = findViewById(R.id.sort_by_time)

        // 初始化滑块值
        val iconSize = sharedPreferences.getInt("icon_size", 50)
        val fontSize = sharedPreferences.getInt("font_size", 12)
        val gridSize = sharedPreferences.getInt("grid_size", 3)
        val sortOption = sharedPreferences.getInt("app_sort_option", 0)

        iconSizeSeekBar.progress = iconSize
        fontSizeSeekBar.progress = fontSize
        gridSizeSeekBar.progress = gridSize

        // 初始化排序选项
        when (sortOption) {
            0 -> sortByName.isChecked = true
            1 -> sortByCategory.isChecked = true
            2 -> sortByTime.isChecked = true
        }

        updateTextValues(iconSize, fontSize, gridSize)

        // 设置滑块监听器
        iconSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateTextValues(progress, fontSizeSeekBar.progress, gridSizeSeekBar.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 0
                sharedPreferences.edit().putInt("icon_size", progress).apply()
            }
        })

        fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateTextValues(iconSizeSeekBar.progress, progress, gridSizeSeekBar.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 0
                sharedPreferences.edit().putInt("font_size", progress).apply()
            }
        })

        gridSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateTextValues(iconSizeSeekBar.progress, fontSizeSeekBar.progress, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 0
                sharedPreferences.edit().putInt("grid_size", progress).apply()
            }
        })

        // 设置排序选项监听器
        sortOptionGroup.setOnCheckedChangeListener {
            _, checkedId ->
            val sortOption = when (checkedId) {
                R.id.sort_by_name -> 0
                R.id.sort_by_category -> 1
                R.id.sort_by_time -> 2
                else -> 0
            }
            sharedPreferences.edit().putInt("app_sort_option", sortOption).apply()
        }
    }

    private fun updateTextValues(iconSize: Int, fontSize: Int, gridSize: Int) {
        iconSizeText.text = "图标大小: ${iconSize}dp"
        fontSizeText.text = "字体大小: ${fontSize}sp"
        gridSizeText.text = "网格大小: ${gridSize + 2}x${gridSize + 2}"
    }
}
