package com.cn.board.wallpaper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WallpaperSettingsActivity : AppCompatActivity() {

    private lateinit var videoPathEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_settings)

        videoPathEditText = findViewById(R.id.videoPathEditText)
        saveButton = findViewById(R.id.saveButton)
        findViewById<Button>(R.id.cancelButton).setOnClickListener { finish() }

        val saved = WallpaperConfigStore.load(this)
        videoPathEditText.setText(saved.videoPath ?: "")
        checkStoragePermission()
        saveButton.setOnClickListener { onSave() }
    }

    private fun onSave() {
        val path = videoPathEditText.text.toString().trim()
        if (path.isEmpty()) { Toast.makeText(this, "请填写视频路径", Toast.LENGTH_SHORT).show(); return }
        val config = WallpaperConfig(videoPath = WallpaperConfigStore.copyToInternal(this, path))
        WallpaperConfigStore.save(this, config)
        SystemWallpaperApplier.applyAsSystemWallpaper(this@WallpaperSettingsActivity)
        finish()
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO), 1001)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
            }
        }
    }
}
