package com.cn.board.meet.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentContainerView
import com.cn.board.wallpaper.WallpaperSettingsActivity
import java.lang.ref.WeakReference
import java.util.function.Consumer


/**
 * HomeActivity
 */
class HomeActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "HomeActivity"
        private const val REQUEST_STORAGE_PERMISSION = 1001
    }

    private var mainView: View? = null
    private var fragmentContainer: FrameLayout? = null
    private var wallpaperSetting: View? = null

    private val wallpaperPath = "/storage/emulated/0/Download/wallpaper_dy_1.mp4"

    private val mBackgroundBlurRadius = 10;
    private val mBlurBehindRadius = 10;

    // We set a different dim amount depending on whether window blur is enabled or disabled
    private val mDimAmountWithBlur = 0.1f
    private val mDimAmountNoBlur = 0.4f

    // We set a different alpha depending on whether window blur is enabled or disabled
    private val mWindowBackgroundAlphaWithBlur = 170
    private val mWindowBackgroundAlphaNoBlur = 255

    // Use a rectangular shape drawable for the window background. The outline of this drawable
    // dictates the shape and rounded corners for the window background blur area.
    private var mWindowBackgroundDrawable: Drawable? = null

    /**
     * 创建 Activity
     * 
     * @param savedInstanceState 保存的实例状态
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        mWindowBackgroundDrawable = getDrawable(R.drawable.window_background);
//        window.setBackgroundDrawable(mWindowBackgroundDrawable);

        if (buildIsAtLeastS()) {
            // Enable blur behind. This can also be done in xml with R.attr#windowBlurBehindEnabled
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

            // Register a listener to adjust window UI whenever window blurs are enabled/disabled
//            setupWindowBlurListener();
        } else {
            // Window blurs are not available prior to Android S
//            updateWindowForBlurs(false /* blursEnabled */);
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        checkStoragePermission()
        mainView = findViewById<View>(R.id.main)
        fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
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

        wallpaperSetting?.setOnClickListener {
            startActivity(Intent(this@HomeActivity, WallpaperSettingsActivity::class.java))
        }
        updateWindowForBlurs(true)
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private fun setupWindowBlurListener() {
        val windowBlurEnabledListener: Consumer<Boolean?> = Consumer { enabled: Boolean? -> updateWindowForBlurs(enabled == true) }
        window.decorView.addOnAttachStateChangeListener(
            object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    windowManager.addCrossWindowBlurEnabledListener(windowBlurEnabledListener)
                }

                override fun onViewDetachedFromWindow(v: View) {
                    windowManager.removeCrossWindowBlurEnabledListener(windowBlurEnabledListener)
                }
            })
    }

    private fun updateWindowForBlurs(blursEnabled: Boolean) {
        mWindowBackgroundDrawable?.alpha = if (blursEnabled && mBackgroundBlurRadius > 0) mWindowBackgroundAlphaWithBlur else mWindowBackgroundAlphaNoBlur
        window.setDimAmount(if (blursEnabled && mBlurBehindRadius > 0) mDimAmountWithBlur else mDimAmountNoBlur)

        if (buildIsAtLeastS()) {
            // Set the window background blur and blur behind radii
            window.setBackgroundBlurRadius(mBackgroundBlurRadius)
            window.attributes.blurBehindRadius = mBlurBehindRadius
            window.setAttributes(window.attributes)
        }
    }

    private fun buildIsAtLeastS(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
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
        grantResults: IntArray,
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