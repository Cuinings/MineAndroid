package com.cn.board.meet.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cn.board.wallpaper.WallpaperSettingsActivity
import com.cn.core.task.TaskManager

/**
 * HomeActivity
 */
class HomeActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "HomeActivity"
        private const val REQUEST_STORAGE_PERMISSION = 1001
    }
    
    private var fragmentContainer: View? = null
    private var mainView: View? = null
    private var wallpaperSetting: View? = null

    private val wallpaperPath = "/storage/emulated/0/Download/wallpaper_dy_1.mp4"

    /**
     * 创建 Activity
     * 
     * @param savedInstanceState 保存的实例状态
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        mainView = findViewById<View>(R.id.main)
        fragmentContainer = findViewById<View>(R.id.fragment_container)
        wallpaperSetting = findViewById<Button>(R.id.wallpaper_setting)

        ViewCompat.setOnApplyWindowInsetsListener(mainView!!) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AppListFragment())
                .commit()
        }

        checkStoragePermission()

        wallpaperSetting?.setOnClickListener {
            startActivity(Intent(this@HomeActivity, WallpaperSettingsActivity::class.java))
        }
    }
    
    /**
     * 检查存储权限
     */
    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }
    
    /**
     * 处理权限请求结果
     * 
     * @param requestCode 请求代码
     * @param permissions 权限数组
     * @param grantResults 授权结果数组
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage permission granted")
            } else {
                Log.w(TAG, "Storage permission denied")
            }
        }
    }

    /**
     * 销毁 Activity
     */
    override fun onDestroy() {
        super.onDestroy()
    }
}