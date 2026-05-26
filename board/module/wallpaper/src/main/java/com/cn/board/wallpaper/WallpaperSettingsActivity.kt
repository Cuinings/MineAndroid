package com.cn.board.wallpaper

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WallpaperSettingsActivity : AppCompatActivity() {

    private lateinit var pathEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private val REQUEST_STORAGE_PERMISSION = 1001

    private val wallpaperPath = "/storage/emulated/0/Download/wallpaper_dy_1.mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).let {
            Log.d("WallpaperSettingsActivity", "onCreate: getDownloadCacheDirectory:${it.path}")
        }
        setContentView(R.layout.activity_wallpaper_settings)

        pathEditText = findViewById(R.id.pathEditText)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        // 检查并请求存储权限
        checkStoragePermission()

        // 加载当前壁纸路径
        pathEditText.setText(wallpaperPath)

        saveButton.setOnClickListener {
            BoardWallpaperService.wallpaperPath = pathEditText.text.toString().trim()
            // 方法1：通过 Intent 调用系统设置
            // 指定你的 WallpaperService
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, BoardWallpaperService::class.java)
            )
            // 可选：传递额外参数
//            val params = Bundle()
//            params.putString("theme", "dark")
//            intent.putExtras(params)
            startActivity(intent)
            finish()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要特定的媒体权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                ), REQUEST_STORAGE_PERMISSION)
            }
        } else {
            // Android 12 及以下需要存储权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), REQUEST_STORAGE_PERMISSION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("WallpaperSettingsActivity", "Storage permission granted")
            } else {
                Toast.makeText(this, "Storage permission is required to load wallpaper", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
