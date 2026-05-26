package com.cn.board.meet.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.cn.board.wallpaper.WallpaperSettingsActivity
import com.cn.core.ui.view.AnimatedScalableConstraintLayout
import com.cn.core.ui.view.frosted.StatefulGlowView
import com.cn.core.ui.view.media.MediaSurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var wallpaperCrop: View? = null

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

    private var wallpaperBitmap: Bitmap? = null
    private var wallpaperBlurBitmap: Bitmap? = null
    var wallpaperCropper: WallpaperCropper? = null
    var isDragging: Boolean = false
    var lastX: Float = 0.0f
    var lastY: Float = 0.0f
    private var lastUpdateTime = 0L
    private val updateInterval = 16L // ~60fps

    var appListFragment: AppListFragment? = null

    /**
     * 创建 Activity
     * 
     * @param savedInstanceState 保存的实例状态
     */
    @SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
//        findViewById<MediaSurfaceView>(R.id.media_surface_view)?.setVideo(wallpaperPath)
        wallpaperCropper = WallpaperCropper(this@HomeActivity)

        mWindowBackgroundDrawable = getDrawable(R.drawable.window_background);
//        window.setBackgroundDrawable(mWindowBackgroundDrawable);

        if (buildIsAtLeastS()) {
            // Enable blur behind. This can also be done in xml with R.attr#windowBlurBehindEnabled
//            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            // Register a listener to adjust window UI whenever window blurs are enabled/disabled
//            setupWindowBlurListener();
        } else {
            // Window blurs are not available prior to Android S
//            updateWindowForBlurs(false /* blursEnabled */);
        }
//        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        checkStoragePermission()
        mainView = findViewById<View>(R.id.main)
//        fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
        wallpaperSetting = findViewById<Button>(R.id.wallpaper_setting)
        wallpaperCrop = findViewById<Button>(R.id.wallpaper_crop)
        /*findViewById<FrostedGlassView>(R.id.glass_blur_view)
        .apply {
            this.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = true
                        lastX = event.rawX
                        lastY = event.rawY
                        setDragging(true)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isDragging) {
                            val deltaX = event.rawX - lastX
                            val deltaY = event.rawY - lastY

                            translationX += deltaX
                            translationY += deltaY

                            lastX = event.rawX
                            lastY = event.rawY

                            refreshScreenPosition()
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                        setDragging(false)
                    }
                }
                true
            }
        }.showGlassBlurViews(this)*/

        ViewCompat.setOnApplyWindowInsetsListener(mainView!!) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            appListFragment = AppListFragment()
            appListFragment?.let {
//                supportFragmentManager.beginTransaction()
//                    .replace(R.id.fragment_container, it)
//                    .commit()
            }
        }

        wallpaperBitmap = WallpaperManager.getInstance(this).drawable?.toBitmap()
        wallpaperBlurBitmap = BitmapBlurUtils.blurBitmap(this@HomeActivity, wallpaperBitmap!!, 5f, 0.1f)
        
        // 初始化裁剪工作协程
        initCropWorker()
        wallpaperSetting?.setOnClickListener {
            startActivity(Intent(this@HomeActivity, WallpaperSettingsActivity::class.java))
        }
        wallpaperCrop?.setOnClickListener {
            cropBlur()
        }

        // Set up touch listener for fragment_container to enable dragging
        fragmentContainer?.apply {
            viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    viewTreeObserver.removeOnPreDrawListener(this)
                    cropBlur()
                    return true
                }
            })
        }
        findViewById<StatefulGlowView>(R.id.statefulGlowView)?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                    lastX = event.rawX
                    lastY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val deltaX = event.rawX - lastX
                        val deltaY = event.rawY - lastY
                        
                        // Get current layout params
                        val params = v.layoutParams as RelativeLayout.LayoutParams
                        
                        // Calculate new position
                        val newLeft = params.leftMargin + deltaX.toInt()
                        val newTop = params.topMargin + deltaY.toInt()
                        
                        // Update layout params
                        params.leftMargin = newLeft
                        params.topMargin = newTop
                        v.layoutParams = params
                        
                        // Update last touch position
                        lastX = event.rawX
                        lastY = event.rawY
                        
                        // 优化：使用硬件加速的方式更新背景
                        // 仅在拖动速度适中时更新，避免过于频繁的更新
                        val velocity = Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())
                        if (velocity < 50) { // 当拖动速度较小时，更新背景
//                            updateFragmentBackground()
                        } else if (velocity < 100) { // 当拖动速度中等时，降低更新频率
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime > 8) { // ~120fps
                                lastUpdateTime = currentTime
//                                updateFragmentBackground()
                            }
                        } else { // 当拖动速度较快时，进一步降低更新频率
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime > 16) { // ~60fps
                                lastUpdateTime = currentTime
//                                updateFragmentBackground()
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    // 确保最后位置的背景被更新
//                    updateFragmentBackground()
                }
            }
            true
        }

//        updateWindowForBlurs(true)
    }

    private fun cropBlur() {
        val cropper = wallpaperCropper ?: return
        val bitmap = wallpaperBlurBitmap ?: return
        val container = fragmentContainer ?: return

        // Get view position on main thread
        val viewRect = Rect()
        val location = IntArray(2)
        container.getLocationOnScreen(location)
        viewRect.set(
            location[0],
            location[1],
            location[0] + container.width,
            location[1] + container.height
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val cropResult =
                cropper.cropWallpaperForView(bitmap, viewRect, WallpaperCropper.CENTER_CROP)
            withContext(Dispatchers.Main) {
//                cropResult?.let { fragmentContainer?.background = it.toDrawable(resources) }
            }
        }
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

    // 用于跟踪当前的更新请求 ID，确保只有最新的更新请求会更新 UI
    private var updateRequestId = 0
    // 用于存储最新的裁剪请求，使用有缓冲的通道避免发送方阻塞
    private val cropRequestChannel = Channel<Triple<WallpaperCropper, Bitmap, Rect>>(capacity = 10)
    // 后台裁剪协程
    private var cropWorkerJob: kotlinx.coroutines.Job? = null
    // 用于跟踪上次的容器位置，避免不必要的裁剪操作
    private var lastContainerLeft = Int.MIN_VALUE
    private var lastContainerTop = Int.MIN_VALUE

    /**
     * 初始化裁剪工作协程
     */
    private fun initCropWorker() {
        cropWorkerJob = lifecycleScope.launch(Dispatchers.Default) {
            var lastProcessedRequestId = 0
            
            for (request in cropRequestChannel) {
                val (cropper, bitmap, viewRect) = request
                val currentRequestId = ++updateRequestId
                lastProcessedRequestId = currentRequestId
                
                try {
                    // 快速执行裁剪操作
                    val cropResult = cropper.cropWallpaperForView(bitmap, viewRect, WallpaperCropper.FIT_CENTER)
                    
                    // 检查是否是最新的请求
                    if (currentRequestId != lastProcessedRequestId) {
                        // 如果不是最新的请求，跳过处理
                        continue
                    }
                    
                    // Update UI on main thread
                    withContext(Dispatchers.Main) {
                        // 只有当当前请求是最新的时，才更新 UI
                        if (currentRequestId == updateRequestId && cropResult != null && !cropResult.isRecycled) {
                            appListFragment?.updateBackground(cropResult.toDrawable(resources))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Update fragment_container background based on current position
     */
    private fun updateFragmentBackground() {
        // Add null checks to prevent crashes
        val cropper = wallpaperCropper ?: return
        val bitmap = wallpaperBlurBitmap ?: return
        val container = fragmentContainer ?: return
        
        // Get view position on main thread before starting background work
        val location = IntArray(2)
        container.getLocationOnScreen(location)
        val currentLeft = location[0]
        val currentTop = location[1]
        
        // 检查容器位置是否真正变化，避免不必要的裁剪操作
        if (currentLeft == lastContainerLeft && currentTop == lastContainerTop) {
            return
        }
        
        // 更新上次位置
        lastContainerLeft = currentLeft
        lastContainerTop = currentTop
        
        val viewRect = Rect()
        viewRect.set(currentLeft, currentTop, currentLeft + container.width, currentTop + container.height)
        
        // 发送裁剪请求到通道
        lifecycleScope.launch(Dispatchers.Main) {
            // 尝试发送请求，如果通道已满则跳过
            runCatching {
                cropRequestChannel.send(Triple(cropper, bitmap, viewRect))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 关闭通道并取消协程
        lifecycleScope.launch(Dispatchers.Main) {
            cropRequestChannel.close()
        }
        cropWorkerJob?.cancel()
        // 清理缓存
        wallpaperCropper?.clearCache()
    }
}