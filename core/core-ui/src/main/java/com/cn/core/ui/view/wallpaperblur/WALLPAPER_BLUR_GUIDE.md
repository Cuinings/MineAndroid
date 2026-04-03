# 壁纸模糊效果实现指南

在显示壁纸的Activity中实现View区域的动态模糊效果

## 📖 概述

本指南介绍如何在设置了以下属性的Activity中，对特定View区域实现动态模糊效果：

```xml
<item name="android:windowShowWallpaper">true</item>
<item name="android:windowBackground">@android:color/transparent</item>
```

## 🎯 核心原理

当Activity显示壁纸时，系统会将壁纸绘制在窗口背景层。我们的解决方案通过以下步骤实现模糊效果：

1. **捕获壁纸** - 使用 `WallpaperManager` 获取当前壁纸
2. **计算区域** - 根据View在屏幕上的位置计算对应的壁纸区域
3. **裁剪模糊** - 对壁纸的对应区域进行裁剪和模糊处理
4. **绘制显示** - 将模糊后的壁纸区域绘制到View上

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

### 2. 在布局中使用 WallpaperBlurView

#### XML布局方式

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- 模糊背景View -->
    <com.cn.core.ui.view.wallpaperblur.WallpaperBlurView
        android:id="@+id/blurView"
        android:layout_width="match_parent"
        android:layout_height="300dp"
        android:layout_gravity="bottom" />
    
    <!-- 其他内容 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:orientation="vertical">
        
        <!-- 你的UI组件 -->
        
    </LinearLayout>
    
</FrameLayout>
```

#### 代码方式创建

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var blurView: WallpaperBlurView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建布局
        val rootLayout = FrameLayout(this)
        setContentView(rootLayout)
        
        // 创建模糊View
        blurView = WallpaperBlurView(this).apply {
            setBlurRadius(20f)           // 设置模糊半径
            setCornerRadius(24f)         // 设置圆角
            setOverlayColor(Color.parseColor("#40000000")) // 设置叠加颜色
        }
        
        // 添加到布局
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            300
        ).apply {
            gravity = Gravity.BOTTOM
        }
        rootLayout.addView(blurView, params)
    }
}
```

### 3. 配置模糊效果

```kotlin
// 设置模糊半径 (0-25)
blurView.setBlurRadius(20f)

// 设置圆角半径
blurView.setCornerRadius(24f)

// 设置叠加颜色 (半透明颜色)
blurView.setOverlayColor(Color.parseColor("#40000000"))

// 设置缩放因子 (影响性能和效果)
blurView.setScaleFactor(0.25f)

// 启用自动更新 (动态壁纸需要)
blurView.setAutoUpdate(true, 100L)
```

## 📱 完整示例

### 示例1: 底部菜单模糊背景

```kotlin
class BottomMenuActivity : AppCompatActivity() {
    
    private lateinit var blurView: WallpaperBlurView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_menu)
        
        blurView = findViewById(R.id.blurView)
        
        // 配置模糊效果
        blurView.apply {
            setBlurRadius(25f)
            setCornerRadius(0f)  // 底部菜单通常不需要圆角
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
    
    <!-- 模糊背景 -->
    <com.cn.core.ui.view.wallpaperblur.WallpaperBlurView
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val rootLayout = FrameLayout(this)
        setContentView(rootLayout)
        
        // 创建多个模糊卡片
        createBlurCard(rootLayout, 50, 100, 300, 200)
        createBlurCard(rootLayout, 100, 350, 250, 150)
        createBlurCard(rootLayout, 50, 550, 350, 180)
    }
    
    private fun createBlurCard(
        parent: FrameLayout,
        x: Int, y: Int,
        width: Int, height: Int
    ) {
        val blurView = WallpaperBlurView(this).apply {
            setBlurRadius(20f)
            setCornerRadius(16f)
            setOverlayColor(Color.parseColor("#40000000"))
        }
        
        val params = FrameLayout.LayoutParams(width, height).apply {
            leftMargin = x
            topMargin = y
        }
        
        parent.addView(blurView, params)
    }
}
```

### 示例3: 动态调整模糊效果

```kotlin
class DynamicBlurActivity : AppCompatActivity() {
    
    private lateinit var blurView: WallpaperBlurView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dynamic)
        
        blurView = findViewById(R.id.blurView)
        
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

### 1. 使用 WallpaperBlurHelper 自定义处理

如果需要更灵活的控制，可以直接使用 `WallpaperBlurHelper`：

```kotlin
class CustomBlurActivity : AppCompatActivity() {
    
    private lateinit var blurHelper: WallpaperBlurHelper
    private lateinit var customView: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom)
        
        blurHelper = WallpaperBlurHelper(this)
        customView = findViewById(R.id.customView)
        
        // 配置模糊参数
        blurHelper.apply {
            setBlurRadius(20f)
            setCornerRadius(16f)
            setOverlayColor(Color.parseColor("#40000000"))
        }
        
        // 设置自定义View的绘制
        customView.viewTreeObserver.addOnGlobalLayoutListener {
            customView.invalidate()
        }
    }
    
    // 在自定义View的onDraw中使用
    private fun drawBlur(canvas: Canvas) {
        blurHelper.drawBlurredRegion(canvas, customView)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        blurHelper.release()
    }
}
```

### 2. 处理动态壁纸

对于动态壁纸，需要启用自动更新：

```kotlin
blurView.apply {
    setAutoUpdate(true, 100L)  // 每100ms更新一次
}
```

### 3. 性能优化

```kotlin
// 对于静态壁纸，禁用自动更新
blurView.setAutoUpdate(false)

// 使用较大的缩放因子提高性能
blurView.setScaleFactor(0.2f)  // 默认0.25f

// 使用较低的模糊半径
blurView.setBlurRadius(15f)  // 默认15f
```

### 4. 多窗口管理

```kotlin
class MultiBlurActivity : AppCompatActivity() {
    
    private val blurViews = mutableListOf<WallpaperBlurView>()
    
    fun addBlurView(x: Int, y: Int, width: Int, height: Int) {
        val blurView = WallpaperBlurView(this).apply {
            setBlurRadius(20f)
            setCornerRadius(16f)
        }
        
        // 添加到布局
        blurViews.add(blurView)
    }
    
    fun updateAllBlurViews() {
        blurViews.forEach { it.updateBlur() }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        blurViews.forEach { it.release() }
        blurViews.clear()
    }
}
```

## ⚙️ API 参考

### WallpaperBlurView

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

### WallpaperBlurHelper

#### 核心方法

```kotlin
// 捕获当前壁纸
fun captureWallpaper(): Bitmap?

// 获取指定区域的模糊壁纸
fun getBlurredRegion(
    view: View,
    regionLeft: Int = 0,
    regionTop: Int = 0,
    regionRight: Int = view.width,
    regionBottom: Int = view.height
): Bitmap?

// 绘制模糊区域到Canvas
fun drawBlurredRegion(
    canvas: Canvas,
    view: View,
    regionLeft: Int = 0,
    regionTop: Int = 0,
    regionRight: Int = view.width,
    regionBottom: Int = view.height
)
```

## 📊 性能优化建议

### 1. 更新频率优化

```kotlin
// 静态壁纸 - 禁用自动更新
blurView.setAutoUpdate(false)

// 动态壁纸 - 使用适当的更新间隔
blurView.setAutoUpdate(true, 150L)  // 150ms更新一次
```

### 2. 模糊参数优化

```kotlin
// 高性能配置
blurView.apply {
    setBlurRadius(15f)       // 中等模糊
    setScaleFactor(0.2f)     // 较小缩放
    setAutoUpdate(false)     // 禁用自动更新
}

// 高质量配置
blurView.apply {
    setBlurRadius(25f)       // 最大模糊
    setScaleFactor(0.3f)     // 较大缩放
    setAutoUpdate(true, 100L) // 频繁更新
}
```

### 3. 资源管理

```kotlin
// 及时释放资源
override fun onDestroy() {
    super.onDestroy()
    blurView.release()
}

// 暂停时停止更新
override fun onPause() {
    super.onPause()
    blurView.setAutoUpdate(false)
}

// 恢复时重新启用
override fun onResume() {
    super.onResume()
    blurView.setAutoUpdate(true, 100L)
}
```

## ⚠️ 注意事项

### 1. 权限要求

不需要特殊权限，`WallpaperManager` 可以直接访问当前壁纸。

### 2. 兼容性

- 支持静态壁纸和动态壁纸
- 最低支持 Android 5.0 (API 21)
- 推荐在 Android 8.0+ 上使用以获得最佳效果

### 3. 性能考虑

- 模糊操作需要一定的计算资源
- 建议同时显示的模糊View不超过3个
- 对于低端设备，建议使用较低的模糊半径和缩放因子

### 4. 内存管理

```kotlin
// 避免内存泄漏
override fun onDestroy() {
    super.onDestroy()
    blurView.release()  // 必须调用
}
```

## 🐛 常见问题

### Q1: 模糊效果不显示？

**A:** 检查以下几点：
1. Activity主题是否正确设置了 `windowShowWallpaper` 和 `windowBackground`
2. View的宽高是否大于0
3. 是否在View布局完成后才调用模糊方法

### Q2: 动态壁纸模糊效果卡顿？

**A:** 尝试以下优化：
1. 降低更新频率：`setAutoUpdate(true, 200L)`
2. 降低模糊半径：`setBlurRadius(15f)`
3. 增加缩放因子：`setScaleFactor(0.2f)`

### Q3: 如何实现部分区域模糊？

**A:** 使用 `WallpaperBlurHelper` 的 `getBlurredRegion` 方法：

```kotlin
val blurHelper = WallpaperBlurHelper(this)
val partialBlur = blurHelper.getBlurredRegion(
    view,
    regionLeft = 50,
    regionTop = 50,
    regionRight = 250,
    regionBottom = 200
)
```

### Q4: 模糊效果与壁纸不同步？

**A:** 确保启用了自动更新：

```kotlin
blurView.setAutoUpdate(true, 100L)
```

## 📚 完整示例

查看以下文件获取完整示例代码：

- **示例Activity**: [WallpaperBlurExampleActivity.kt](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/wallpaperblur/WallpaperBlurExampleActivity.kt)
- **核心View**: [WallpaperBlurView.kt](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/wallpaperblur/WallpaperBlurView.kt)
- **辅助类**: [WallpaperBlurHelper.kt](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/wallpaperblur/WallpaperBlurHelper.kt)

## 🔗 相关文档

- [全局模糊窗口系统](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/globalblur/README.md)
- [Activity和Fragment使用指南](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/globalblur/USAGE_GUIDE.md)
