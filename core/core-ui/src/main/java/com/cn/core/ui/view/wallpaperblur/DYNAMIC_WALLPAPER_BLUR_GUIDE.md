# 动态壁纸模糊效果实现指南

在显示动态壁纸的Activity中实现View区域的实时模糊效果

## 📖 概述

本指南介绍如何在设置了动态壁纸的Activity中，对特定View区域实现实时模糊效果。动态壁纸会持续变化，因此需要特殊的处理方式来捕获和模糊壁纸内容。

## 🎯 核心原理

动态壁纸模糊效果的实现原理：

1. **MediaProjection** - 使用屏幕录制API捕获屏幕内容（包含动态壁纸）
2. **实时捕获** - 定期捕获屏幕帧，获取最新的壁纸状态
3. **区域计算** - 根据View位置计算对应的屏幕区域
4. **模糊处理** - 对捕获的区域进行模糊处理
5. **绘制显示** - 将模糊后的内容绘制到View上

## 🚀 快速开始

### 1. 设置Activity主题

首先，在 `styles.xml` 中创建显示壁纸的主题：

```xml
<style name="WallpaperTheme" parent="Theme.AppCompat.Light.NoActionBar">
    <item name="android:windowShowWallpaper">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
</style>
```

在 `AndroidManifest.xml` 中应用主题：

```xml
<activity
    android:name=".YourActivity"
    android:theme="@style/WallpaperTheme" />
```

### 2. 在布局中使用 DynamicWallpaperBlurView

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- 动态壁纸模糊View -->
    <com.cn.core.ui.view.wallpaperblur.DynamicWallpaperBlurView
        android:id="@+id/blurView"
        android:layout_width="match_parent"
        android:layout_height="300dp"
        android:layout_gravity="bottom" />
    
    <!-- 其他内容 -->
    
</FrameLayout>
```

### 3. 初始化并请求权限

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var blurView: DynamicWallpaperBlurView
    private val REQUEST_CODE_MEDIA_PROJECTION = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        blurView = findViewById(R.id.blurView)
        
        // 请求屏幕录制权限
        requestMediaProjection()
    }
    
    private fun requestMediaProjection() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, REQUEST_CODE_MEDIA_PROJECTION)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // 初始化模糊View
                initializeBlurView(resultCode, data)
            } else {
                Toast.makeText(this, "需要屏幕录制权限", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun initializeBlurView(resultCode: Int, data: Intent) {
        val success = blurView.initialize(this, resultCode, data)
        
        if (success) {
            // 配置模糊效果
            blurView.apply {
                setBlurRadius(20f)
                setCornerRadius(24f)
                setOverlayColor(Color.parseColor("#40000000"))
                setAutoUpdate(true, 100L)  // 每100ms更新一次
            }
        }
    }
}
```

## 📱 完整示例

### 示例1: 底部菜单模糊背景

```kotlin
class BottomMenuActivity : AppCompatActivity() {
    
    private lateinit var blurView: DynamicWallpaperBlurView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_menu)
        
        blurView = findViewById(R.id.blurView)
        requestMediaProjection()
    }
    
    private fun initializeBlurView(resultCode: Int, data: Intent) {
        blurView.initialize(this, resultCode, data)
        
        blurView.apply {
            setBlurRadius(25f)
            setCornerRadius(0f)
            setOverlayColor(Color.parseColor("#60000000"))
            setAutoUpdate(true, 100L)
        }
    }
}
```

布局文件：

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- 主内容 -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:text="主内容区域"
        android:textColor="@android:color/black" />
    
    <!-- 动态壁纸模糊背景 -->
    <com.cn.core.ui.view.wallpaperblur.DynamicWallpaperBlurView
        android:id="@+id/blurView"
        android:layout_width="match_parent"
        android:layout_height="200dp"
        android:layout_gravity="bottom" />
    
    <!-- 菜单内容 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="200dp"
        android:layout_gravity="bottom"
        android:orientation="vertical"
        android:padding="16dp">
        
        <Button
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="菜单项1" />
        
        <Button
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="菜单项2" />
        
    </LinearLayout>
    
</FrameLayout>
```

### 示例2: 卡片式模糊效果

```kotlin
class CardBlurActivity : AppCompatActivity() {
    
    private val blurViews = mutableListOf<DynamicWallpaperBlurView>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val rootLayout = FrameLayout(this)
        setContentView(rootLayout)
        
        requestMediaProjection()
    }
    
    private fun initializeBlurViews(resultCode: Int, data: Intent) {
        // 创建多个模糊卡片
        createBlurCard(50, 100, 300, 200, resultCode, data)
        createBlurCard(100, 350, 250, 150, resultCode, data)
        createBlurCard(50, 550, 350, 180, resultCode, data)
    }
    
    private fun createBlurCard(
        x: Int, y: Int,
        width: Int, height: Int,
        resultCode: Int, data: Intent
    ) {
        val blurView = DynamicWallpaperBlurView(this)
        blurView.initialize(this, resultCode, data)
        
        blurView.apply {
            setBlurRadius(20f)
            setCornerRadius(16f)
            setOverlayColor(Color.parseColor("#40000000"))
            setAutoUpdate(true, 150L)
        }
        
        val params = FrameLayout.LayoutParams(width, height).apply {
            leftMargin = x
            topMargin = y
        }
        
        (findViewById<FrameLayout>(android.R.id.content)).addView(blurView, params)
        blurViews.add(blurView)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        blurViews.forEach { it.release() }
    }
}
```

### 示例3: 动态调整模糊效果

```kotlin
class DynamicBlurActivity : AppCompatActivity() {
    
    private lateinit var blurView: DynamicWallpaperBlurView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dynamic)
        
        blurView = findViewById(R.id.blurView)
        requestMediaProjection()
    }
    
    private fun initializeBlurView(resultCode: Int, data: Intent) {
        blurView.initialize(this, resultCode, data)
        blurView.setAutoUpdate(true, 100L)
        
        // 动态调整模糊强度
        animateBlurRadius()
    }
    
    private fun animateBlurRadius() {
        val animator = ValueAnimator.ofFloat(5f, 25f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            
            addUpdateListener { animation ->
                val radius = animation.animatedValue as Float
                blurView.setBlurRadius(radius)
            }
        }
        animator.start()
    }
}
```

## 🔧 高级用法

### 1. 手动控制更新

```kotlin
// 禁用自动更新
blurView.setAutoUpdate(false)

// 手动触发更新
blurView.updateBlur()

// 在特定时机更新
override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
        blurView.updateBlur()
    }
}
```

### 2. 性能优化配置

```kotlin
// 高性能配置（适合低端设备）
blurView.apply {
    setBlurRadius(15f)          // 中等模糊
    setScaleFactor(0.2f)        // 较小缩放
    setAutoUpdate(true, 200L)   // 较低更新频率
}

// 高质量配置（适合高端设备）
blurView.apply {
    setBlurRadius(25f)          // 最大模糊
    setScaleFactor(0.3f)        // 较大缩放
    setAutoUpdate(true, 100L)   // 较高更新频率
}

// 平衡配置（推荐）
blurView.apply {
    setBlurRadius(20f)          // 适中模糊
    setScaleFactor(0.25f)       // 默认缩放
    setAutoUpdate(true, 150L)   // 平衡更新频率
}
```

### 3. 生命周期管理

```kotlin
override fun onPause() {
    super.onPause()
    // 暂停时停止自动更新以节省资源
    blurView.setAutoUpdate(false)
}

override fun onResume() {
    super.onResume()
    // 恢复时重新启用自动更新
    blurView.setAutoUpdate(true, 100L)
}

override fun onDestroy() {
    super.onDestroy()
    // 必须释放资源
    blurView.release()
}
```

### 4. 检查状态

```kotlin
// 检查是否已初始化
if (blurView.isReady()) {
    // 可以使用模糊效果
    blurView.setAutoUpdate(true, 100L)
} else {
    // 需要先初始化
    requestMediaProjection()
}
```

## ⚙️ API 参考

### DynamicWallpaperBlurView

#### 初始化方法

```kotlin
// 初始化并请求屏幕录制权限
fun initialize(activity: Activity, resultCode: Int, data: Intent): Boolean

// 检查是否已就绪
fun isReady(): Boolean
```

#### 属性设置方法

```kotlin
// 设置模糊半径 (0-25)
fun setBlurRadius(radius: Float)

// 设置缩放因子 (0.1-1.0)
fun setScaleFactor(factor: Float)

// 设置圆角半径
fun setCornerRadius(radius: Float)

// 设置叠加颜色
fun setOverlayColor(color: Int)

// 设置自动更新
fun setAutoUpdate(enabled: Boolean, interval: Long = 100L)
```

#### 操作方法

```kotlin
// 手动更新模糊效果
fun updateBlur()

// 释放资源
fun release()
```

## 📊 性能优化建议

### 1. 更新频率优化

```kotlin
// 根据设备性能选择更新频率
val updateInterval = when {
    isHighEndDevice() -> 100L   // 高端设备：100ms
    isMidRangeDevice() -> 150L  // 中端设备：150ms
    else -> 200L                // 低端设备：200ms
}

blurView.setAutoUpdate(true, updateInterval)
```

### 2. 模糊参数优化

```kotlin
// 根据使用场景选择参数
fun configureForMenu() {
    blurView.apply {
        setBlurRadius(25f)       // 菜单需要强模糊
        setScaleFactor(0.25f)
        setAutoUpdate(true, 100L)
    }
}

fun configureForCard() {
    blurView.apply {
        setBlurRadius(20f)       // 卡片使用中等模糊
        setScaleFactor(0.2f)
        setAutoUpdate(true, 150L)
    }
}
```

### 3. 内存优化

```kotlin
// 及时释放不使用的View
fun removeBlurView(blurView: DynamicWallpaperBlurView) {
    blurView.release()
    parentView.removeView(blurView)
}

// 批量释放
fun releaseAllBlurViews() {
    blurViews.forEach { it.release() }
    blurViews.clear()
}
```

## ⚠️ 注意事项

### 1. 权限要求

- **屏幕录制权限** - 必须通过 `MediaProjection` API 获取
- 用户可以随时撤销权限
- 需要在Activity中处理权限结果

### 2. 兼容性

- 最低支持 Android 5.0 (API 21)
- 推荐在 Android 7.0+ 上使用以获得最佳效果
- 部分设备可能有性能差异

### 3. 性能考虑

- 屏幕捕获需要一定的系统资源
- 建议同时显示的模糊View不超过3个
- 低端设备建议使用较低的更新频率

### 4. 生命周期管理

```kotlin
// 必须在Activity销毁时释放资源
override fun onDestroy() {
    super.onDestroy()
    blurView.release()  // 必须调用
}

// 暂停时停止更新以节省资源
override fun onPause() {
    super.onPause()
    blurView.setAutoUpdate(false)
}
```

## 🐛 常见问题

### Q1: 为什么需要屏幕录制权限？

**A:** 动态壁纸会持续变化，无法通过 `WallpaperManager` 直接获取。需要使用 `MediaProjection` API 捕获屏幕内容（包含动态壁纸）来实现实时模糊效果。

### Q2: 模糊效果卡顿怎么办？

**A:** 尝试以下优化：
1. 降低更新频率：`setAutoUpdate(true, 200L)`
2. 降低模糊半径：`setBlurRadius(15f)`
3. 增加缩放因子：`setScaleFactor(0.2f)`
4. 减少同时显示的模糊View数量

### Q3: 权限被拒绝怎么办？

**A:** 
```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
        if (resultCode != Activity.RESULT_OK) {
            // 引导用户到设置页面
            Toast.makeText(this, "需要屏幕录制权限才能使用动态壁纸模糊效果", Toast.LENGTH_LONG).show()
            // 或者使用静态壁纸模糊作为替代方案
            useStaticWallpaperBlur()
        }
    }
}
```

### Q4: 如何判断使用静态还是动态方案？

**A:**
```kotlin
fun isLiveWallpaper(): Boolean {
    val wallpaperInfo = WallpaperManager.getInstance(context).wallpaperInfo
    return wallpaperInfo != null
}

// 根据壁纸类型选择方案
if (isLiveWallpaper()) {
    // 使用 DynamicWallpaperBlurView
} else {
    // 使用 WallpaperBlurView（更高效）
}
```

### Q5: 可以在Fragment中使用吗？

**A:** 可以，但需要在宿主Activity中处理权限请求：

```kotlin
// Fragment中
class BlurFragment : Fragment() {
    private lateinit var blurView: DynamicWallpaperBlurView
    
    fun initialize(resultCode: Int, data: Intent) {
        activity?.let {
            blurView.initialize(it, resultCode, data)
        }
    }
}

// Activity中
class HostActivity : AppCompatActivity() {
    private lateinit var blurFragment: BlurFragment
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        blurFragment.initialize(resultCode, data)
    }
}
```

## 📚 完整示例

查看以下文件获取完整示例代码：

- **示例Activity**: [DynamicWallpaperBlurExampleActivity.kt](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/wallpaperblur/DynamicWallpaperBlurExampleActivity.kt)
- **核心View**: [DynamicWallpaperBlurView.kt](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/wallpaperblur/DynamicWallpaperBlurView.kt)
- **辅助类**: [DynamicWallpaperBlurHelper.kt](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/wallpaperblur/DynamicWallpaperBlurHelper.kt)

## 🔗 相关文档

- [静态壁纸模糊效果](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/wallpaperblur/WALLPAPER_BLUR_GUIDE.md)
- [全局模糊窗口系统](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/globalblur/README.md)
- [Activity和Fragment使用指南](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/globalblur/USAGE_GUIDE.md)

## 📋 方案对比

| 特性 | WallpaperBlurView | DynamicWallpaperBlurView |
|------|-------------------|--------------------------|
| 适用场景 | 静态壁纸 | 动态壁纸 |
| 权限要求 | 无 | 屏幕录制权限 |
| 性能消耗 | 低 | 中等 |
| 实时性 | 无 | 高 |
| 实现复杂度 | 简单 | 中等 |
| 推荐使用 | 静态壁纸场景 | 动态壁纸场景 |

---

**注意**: 使用本方案时请遵守相关法律法规和用户隐私政策，确保用户知情并同意屏幕录制权限的使用。
