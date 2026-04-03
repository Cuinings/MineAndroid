# Global Blur Window System

基于Android系统级别API的全局任意区域模糊效果窗口系统

## 📖 简介

Global Blur Window System 是一个强大的Android库，允许你在屏幕任意位置创建模糊效果窗口。该系统完全基于Android系统级API实现，可以在任何应用之上显示模糊效果，非常适合用于创建全局通知、悬浮窗、系统级UI等场景。

## ✨ 核心特性

- **全局任意位置** - 可以在屏幕任意位置显示模糊窗口，不受应用窗口限制
- **实时模糊** - 支持自动更新模式，实时捕获屏幕并应用模糊效果
- **高性能渲染** - 使用 RenderScript 实现高效的模糊算法
- **灵活配置** - 支持模糊半径、圆角、叠加颜色等自定义参数
- **多窗口支持** - 可以同时创建多个独立的模糊窗口
- **权限自动处理** - 自动请求和处理悬浮窗和屏幕录制权限
- **易于集成** - 提供简洁的API接口，几行代码即可实现模糊效果

## 🏗️ 系统架构

### 核心组件

```
GlobalBlurHelper (API入口)
    ├── GlobalBlurService (后台服务)
    │   ├── GlobalBlurWindowManager (窗口管理)
    │   │   └── GlobalBlurView (模糊视图)
    │   │       └── BlurRenderer (模糊渲染)
    │   └── ScreenCaptureHelper (屏幕捕获)
```

#### 1. GlobalBlurWindowManager
- 管理系统级窗口的创建、更新和销毁
- 使用 `WindowManager` 和 `TYPE_APPLICATION_OVERLAY` 实现全局窗口
- 支持多个独立的模糊窗口实例

#### 2. ScreenCaptureHelper
- 使用 `MediaProjection` API 捕获屏幕内容
- 支持指定区域捕获
- 支持单帧捕获和连续捕获模式

#### 3. BlurRenderer
- 基于 `RenderScript` 实现高性能模糊渲染
- 支持自定义模糊半径、缩放因子
- 支持圆角和叠加颜色效果

#### 4. GlobalBlurView
- 自定义视图，负责显示模糊效果
- 支持自动更新模式
- 管理模糊位图的生命周期

#### 5. GlobalBlurService
- 后台服务，管理模糊窗口的生命周期
- 统一管理屏幕捕获和窗口管理

#### 6. GlobalBlurHelper
- 简化的API接口
- 自动处理权限请求
- 管理服务绑定和生命周期

## 🚀 快速开始

### 1. 添加依赖

确保你的项目中已经包含了 `core-ui` 模块。

### 2. 初始化

在你的Activity中初始化 `GlobalBlurHelper`：

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var blurHelper: GlobalBlurHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化模糊助手
        blurHelper = GlobalBlurHelper(this)
        blurHelper.initialize()
    }
    
    // 处理权限请求结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        blurHelper.handleActivityResult(requestCode, resultCode, data)
        
        // 权限授权成功后创建模糊窗口
        if (blurHelper.isReady()) {
            createBlurWindow()
        }
    }
}
```

### 3. 创建模糊窗口

```kotlin
private fun createBlurWindow() {
    val success = blurHelper.createBlurWindow(
        id = "my_blur_window",          // 窗口唯一标识
        x = 100,                         // X坐标
        y = 200,                         // Y坐标
        width = 300,                     // 宽度
        height = 200,                    // 高度
        blurRadius = 20f,                // 模糊半径 (0-25)
        cornerRadius = 16f,              // 圆角半径
        overlayColor = Color.parseColor("#80000000"), // 叠加颜色
        autoUpdate = true,               // 自动更新
        updateInterval = 100L            // 更新间隔(毫秒)
    )
    
    if (success) {
        Toast.makeText(this, "模糊窗口创建成功", Toast.LENGTH_SHORT).show()
    }
}
```

### 4. 更新窗口属性

```kotlin
// 更新模糊半径
blurHelper.updateBlurRadius("my_blur_window", 25f)

// 更新圆角半径
blurHelper.updateCornerRadius("my_blur_window", 24f)

// 更新叠加颜色
blurHelper.updateOverlayColor("my_blur_window", Color.parseColor("#40000000"))

// 移动窗口位置
blurHelper.updateBlurWindowPosition("my_blur_window", 200, 300)

// 调整窗口大小
blurHelper.updateBlurWindowSize("my_blur_window", 400, 300)
```

### 5. 显示/隐藏窗口

```kotlin
// 隐藏窗口
blurHelper.hideBlurWindow("my_blur_window")

// 显示窗口
blurHelper.showBlurWindow("my_blur_window")

// 移除窗口
blurHelper.removeBlurWindow("my_blur_window")

// 移除所有窗口
blurHelper.removeAllBlurWindows()
```

### 6. 清理资源

```kotlin
override fun onDestroy() {
    super.onDestroy()
    blurHelper.release()
}
```

## 📋 权限要求

系统需要以下权限：

### 必需权限

1. **悬浮窗权限** (`SYSTEM_ALERT_WINDOW`)
   - 用于在屏幕上显示系统级窗口
   - Android 6.0+ 需要用户手动授权

2. **前台服务权限** (`FOREGROUND_SERVICE`)
   - 用于运行后台服务

3. **屏幕录制权限** (运行时请求)
   - 用于捕获屏幕内容实现模糊效果
   - 通过 `MediaProjection` API 实现

### 权限处理

`GlobalBlurHelper` 会自动处理权限请求流程：

1. 首次调用 `initialize()` 时，会检查悬浮窗权限
2. 如果没有权限，会自动跳转到权限设置页面
3. 授权后，会自动请求屏幕录制权限
4. 所有权限授权完成后，即可创建模糊窗口

## 🎯 使用场景

### 1. 全局通知

```kotlin
// 创建一个全局通知模糊背景
blurHelper.createBlurWindow(
    id = "notification_bg",
    x = 50,
    y = 100,
    width = screenWidth - 100,
    height = 150,
    blurRadius = 25f,
    cornerRadius = 20f,
    autoUpdate = true
)
```

### 2. 悬浮窗效果

```kotlin
// 创建一个可移动的悬浮模糊窗口
blurHelper.createBlurWindow(
    id = "floating_window",
    x = 100,
    y = 200,
    width = 300,
    height = 400,
    blurRadius = 20f,
    cornerRadius = 16f,
    overlayColor = Color.parseColor("#40000000"),
    autoUpdate = true
)

// 根据触摸事件移动窗口
blurHelper.updateBlurWindowPosition("floating_window", newX, newY)
```

### 3. 对话框背景

```kotlin
// 创建对话框模糊背景
blurHelper.createBlurWindow(
    id = "dialog_bg",
    x = (screenWidth - dialogWidth) / 2,
    y = (screenHeight - dialogHeight) / 2,
    width = dialogWidth,
    height = dialogHeight,
    blurRadius = 15f,
    cornerRadius = 24f,
    overlayColor = Color.parseColor("#80000000"),
    autoUpdate = false  // 静态模糊
)
```

### 4. 底部弹出菜单

```kotlin
// 创建底部菜单模糊背景
blurHelper.createBlurWindow(
    id = "bottom_menu",
    x = 0,
    y = screenHeight - menuHeight,
    width = screenWidth,
    height = menuHeight,
    blurRadius = 20f,
    cornerRadius = 0f,
    autoUpdate = true
)
```

## ⚙️ 高级配置

### 自定义模糊参数

```kotlin
// 创建高模糊效果
blurHelper.createBlurWindow(
    id = "high_blur",
    x = 100, y = 100, width = 300, height = 200,
    blurRadius = 25f,  // 最大模糊半径
    cornerRadius = 0f
)

// 创建轻微模糊效果
blurHelper.createBlurWindow(
    id = "light_blur",
    x = 100, y = 100, width = 300, height = 200,
    blurRadius = 5f,   // 轻微模糊
    cornerRadius = 16f
)
```

### 动态更新效果

```kotlin
// 动态调整模糊强度
val animator = ValueAnimator.ofFloat(5f, 25f)
animator.duration = 1000
animator.addUpdateListener { animation ->
    val radius = animation.animatedValue as Float
    blurHelper.updateBlurRadius("my_blur_window", radius)
}
animator.start()
```

### 多窗口管理

```kotlin
// 创建多个模糊窗口
blurHelper.createBlurWindow("window1", 100, 100, 200, 150, 15f)
blurHelper.createBlurWindow("window2", 350, 100, 200, 150, 20f)
blurHelper.createBlurWindow("window3", 100, 300, 200, 150, 25f)

// 批量操作
blurHelper.getBlurWindowIds().forEach { id ->
    blurHelper.updateBlurRadius(id, 20f)
}

// 清除所有窗口
blurHelper.removeAllBlurWindows()
```

## 📊 性能优化建议

### 1. 更新频率优化

```kotlin
// 对于静态内容，禁用自动更新
blurHelper.createBlurWindow(
    id = "static_blur",
    x = 100, y = 100, width = 300, height = 200,
    blurRadius = 20f,
    autoUpdate = false  // 禁用自动更新
)

// 对于动态内容，使用适当的更新间隔
blurHelper.createBlurWindow(
    id = "dynamic_blur",
    x = 100, y = 100, width = 300, height = 200,
    blurRadius = 20f,
    autoUpdate = true,
    updateInterval = 200L  // 200ms更新一次，平衡性能和效果
)
```

### 2. 窗口大小优化

```kotlin
// 避免创建过大的窗口
// 推荐尺寸：300x200 或更小

// 对于大区域，考虑分割成多个小窗口
blurHelper.createBlurWindow("blur_part1", 0, 0, 300, 200, 20f)
blurHelper.createBlurWindow("blur_part2", 300, 0, 300, 200, 20f)
```

### 3. 模糊半径优化

```kotlin
// 模糊半径越大，性能消耗越高
// 推荐范围：10-20

// 轻度模糊（高性能）
blurRadius = 10f

// 中度模糊（平衡）
blurRadius = 15f

// 强度模糊（高质量）
blurRadius = 20f
```

### 4. 及时释放资源

```kotlin
// 不使用时及时移除窗口
blurHelper.removeBlurWindow("unused_window")

// Activity销毁时释放所有资源
override fun onDestroy() {
    super.onDestroy()
    blurHelper.release()
}
```

## 🔧 API 参考

### GlobalBlurHelper

#### 初始化方法

```kotlin
// 初始化模糊助手
fun initialize()

// 处理权限请求结果
fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

// 检查是否就绪
fun isReady(): Boolean

// 释放资源
fun release()
```

#### 窗口管理方法

```kotlin
// 创建模糊窗口
fun createBlurWindow(
    id: String,              // 窗口唯一标识
    x: Int,                  // X坐标
    y: Int,                  // Y坐标
    width: Int,              // 宽度
    height: Int,             // 高度
    blurRadius: Float = 15f, // 模糊半径 (0-25)
    cornerRadius: Float = 0f,// 圆角半径
    overlayColor: Int = Color.TRANSPARENT, // 叠加颜色
    autoUpdate: Boolean = false, // 自动更新
    updateInterval: Long = 100L  // 更新间隔(毫秒)
): Boolean

// 更新窗口位置
fun updateBlurWindowPosition(id: String, x: Int, y: Int): Boolean

// 更新窗口大小
fun updateBlurWindowSize(id: String, width: Int, height: Int): Boolean

// 更新模糊半径
fun updateBlurRadius(id: String, radius: Float): Boolean

// 更新圆角半径
fun updateCornerRadius(id: String, radius: Float): Boolean

// 更新叠加颜色
fun updateOverlayColor(id: String, color: Int): Boolean

// 显示窗口
fun showBlurWindow(id: String): Boolean

// 隐藏窗口
fun hideBlurWindow(id: String): Boolean

// 移除窗口
fun removeBlurWindow(id: String): Boolean

// 移除所有窗口
fun removeAllBlurWindows()
```

## 🐛 常见问题

### Q1: 为什么模糊窗口不显示？

**A:** 检查以下几点：
1. 是否已授权悬浮窗权限
2. 是否已授权屏幕录制权限
3. `blurHelper.isReady()` 是否返回 `true`
4. 窗口坐标和大小是否在屏幕范围内

### Q2: 模糊效果卡顿怎么办？

**A:** 尝试以下优化：
1. 降低更新频率（增大 `updateInterval`）
2. 减小模糊半径（降低 `blurRadius`）
3. 减小窗口大小
4. 禁用自动更新（`autoUpdate = false`）

### Q3: 如何实现静态模糊效果？

**A:** 创建窗口时设置 `autoUpdate = false`：
```kotlin
blurHelper.createBlurWindow(
    id = "static_blur",
    x = 100, y = 100, width = 300, height = 200,
    blurRadius = 20f,
    autoUpdate = false
)
```

### Q4: 可以创建多少个模糊窗口？

**A:** 理论上没有限制，但建议：
- 同时显示的窗口不超过5个
- 总窗口数不超过10个
- 不使用的窗口及时移除

### Q5: 如何处理权限被拒绝的情况？

**A:** `GlobalBlurHelper` 会自动处理权限请求，如果用户拒绝：
1. 会显示提示信息
2. 可以再次调用 `initialize()` 重新请求
3. 或者引导用户到设置页面手动授权

## 📱 系统要求

- **最低SDK版本**: Android 5.0 (API 21)
- **推荐SDK版本**: Android 8.0+ (API 26+)
- **编译SDK版本**: Android 13 (API 33)

## 📄 许可证

本项目遵循 MIT 许可证。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📮 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 GitHub Issue
- 发送邮件至项目维护者

---

**注意**: 本系统使用了 Android 系统级 API，请在使用时遵守相关法律法规和用户隐私政策。
