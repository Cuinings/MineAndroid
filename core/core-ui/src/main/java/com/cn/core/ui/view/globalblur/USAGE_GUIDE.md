# 在 Activity 和 Fragment 中实现全局模糊窗口

本文档详细介绍如何在 Activity 和 Fragment 中集成和使用全局模糊窗口系统。

## 📱 Activity 中的实现

### 1. 基本集成

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var blurHelper: GlobalBlurHelper
    private var isBlurWindowVisible = false
    
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
        
        // 权限授权成功后可以创建模糊窗口
        if (blurHelper.isReady()) {
            Toast.makeText(this, "全局模糊服务已就绪", Toast.LENGTH_SHORT).show()
        }
    }
}
```

### 2. 创建模糊窗口

```kotlin
private fun createBlurWindow() {
    val windowId = "my_blur_window"
    
    // 检查窗口是否已存在
    if (blurHelper.hasBlurWindow(windowId)) {
        Toast.makeText(this, "模糊窗口已存在", Toast.LENGTH_SHORT).show()
        return
    }
    
    // 获取屏幕尺寸
    val screenWidth = resources.displayMetrics.widthPixels
    val screenHeight = resources.displayMetrics.heightPixels
    
    // 创建模糊窗口
    val success = blurHelper.createBlurWindow(
        id = windowId,
        x = screenWidth / 4,           // X坐标
        y = screenHeight / 3,          // Y坐标
        width = screenWidth / 2,       // 宽度
        height = 300,                  // 高度
        blurRadius = 20f,              // 模糊半径
        cornerRadius = 24f,            // 圆角半径
        overlayColor = Color.parseColor("#80000000"), // 叠加颜色
        autoUpdate = true,             // 自动更新
        updateInterval = 100L          // 更新间隔
    )
    
    if (success) {
        isBlurWindowVisible = true
        Toast.makeText(this, "模糊窗口创建成功", Toast.LENGTH_SHORT).show()
    }
}
```

### 3. 更新模糊效果

```kotlin
private fun updateBlurEffect() {
    val windowId = "my_blur_window"
    
    if (!blurHelper.hasBlurWindow(windowId)) {
        Toast.makeText(this, "请先创建模糊窗口", Toast.LENGTH_SHORT).show()
        return
    }
    
    // 更新模糊半径
    blurHelper.updateBlurRadius(windowId, 25f)
    
    // 更新圆角半径
    blurHelper.updateCornerRadius(windowId, 32f)
    
    // 更新叠加颜色
    blurHelper.updateOverlayColor(windowId, Color.parseColor("#40000000"))
    
    Toast.makeText(this, "模糊效果已更新", Toast.LENGTH_SHORT).show()
}
```

### 4. 移动和调整窗口

```kotlin
private fun moveBlurWindow() {
    val windowId = "my_blur_window"
    
    if (!blurHelper.hasBlurWindow(windowId)) {
        return
    }
    
    // 移动窗口位置
    blurHelper.updateBlurWindowPosition(windowId, newX, newY)
    
    // 调整窗口大小
    blurHelper.updateBlurWindowSize(windowId, newWidth, newHeight)
}
```

### 5. 显示/隐藏窗口

```kotlin
private fun toggleBlurWindow() {
    val windowId = "my_blur_window"
    
    if (!blurHelper.hasBlurWindow(windowId)) {
        return
    }
    
    if (isBlurWindowVisible) {
        blurHelper.hideBlurWindow(windowId)
        isBlurWindowVisible = false
    } else {
        blurHelper.showBlurWindow(windowId)
        isBlurWindowVisible = true
    }
}
```

### 6. 生命周期管理

```kotlin
override fun onPause() {
    super.onPause()
    // Activity暂停时隐藏模糊窗口
    if (::blurHelper.isInitialized && blurHelper.hasBlurWindow("my_blur_window")) {
        blurHelper.hideBlurWindow("my_blur_window")
    }
}

override fun onResume() {
    super.onResume()
    // Activity恢复时显示模糊窗口
    if (::blurHelper.isInitialized && 
        blurHelper.hasBlurWindow("my_blur_window") && 
        isBlurWindowVisible) {
        blurHelper.showBlurWindow("my_blur_window")
    }
}

override fun onDestroy() {
    super.onDestroy()
    // Activity销毁时释放资源
    if (::blurHelper.isInitialized) {
        blurHelper.release()
    }
}
```

## 📄 Fragment 中的实现

### 1. 基本集成

```kotlin
class BlurFragment : Fragment() {
    private lateinit var blurHelper: GlobalBlurHelper
    private var isBlurWindowVisible = false
    private var isInitialized = false
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeBlurHelper()
    }
    
    private fun initializeBlurHelper() {
        activity?.let { activity ->
            blurHelper = GlobalBlurHelper(activity)
            blurHelper.initialize()
            isInitialized = true
        }
    }
    
    // Fragment中需要手动处理权限结果
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (::blurHelper.isInitialized) {
            blurHelper.handleActivityResult(requestCode, resultCode, data)
            
            if (blurHelper.isReady()) {
                Toast.makeText(requireContext(), "全局模糊服务已就绪", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

### 2. 在宿主Activity中处理权限结果

```kotlin
class HostActivity : AppCompatActivity() {
    private lateinit var blurFragment: BlurFragment
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        blurFragment = BlurFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, blurFragment)
            .commit()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 将权限结果传递给Fragment
        blurFragment.handleActivityResult(requestCode, resultCode, data)
    }
}
```

### 3. Fragment中的模糊窗口操作

```kotlin
private fun createBlurWindow() {
    if (!isInitialized || !blurHelper.isReady()) {
        Toast.makeText(requireContext(), "请等待初始化完成", Toast.LENGTH_SHORT).show()
        return
    }
    
    val windowId = "fragment_blur_window"
    
    if (blurHelper.hasBlurWindow(windowId)) {
        return
    }
    
    val screenWidth = resources.displayMetrics.widthPixels
    val screenHeight = resources.displayMetrics.heightPixels
    
    val success = blurHelper.createBlurWindow(
        id = windowId,
        x = screenWidth / 6,
        y = screenHeight / 4,
        width = screenWidth * 2 / 3,
        height = 250,
        blurRadius = 18f,
        cornerRadius = 20f,
        overlayColor = Color.parseColor("#60000000"),
        autoUpdate = true,
        updateInterval = 150L
    )
    
    if (success) {
        isBlurWindowVisible = true
    }
}
```

### 4. Fragment生命周期管理

```kotlin
override fun onPause() {
    super.onPause()
    // Fragment暂停时隐藏模糊窗口
    if (::blurHelper.isInitialized && blurHelper.hasBlurWindow("fragment_blur_window")) {
        blurHelper.hideBlurWindow("fragment_blur_window")
    }
}

override fun onResume() {
    super.onResume()
    // Fragment恢复时显示模糊窗口
    if (::blurHelper.isInitialized && 
        blurHelper.hasBlurWindow("fragment_blur_window") && 
        isBlurWindowVisible) {
        blurHelper.showBlurWindow("fragment_blur_window")
    }
}

override fun onDestroyView() {
    super.onDestroyView()
    // Fragment视图销毁时移除模糊窗口
    if (::blurHelper.isInitialized) {
        blurHelper.removeBlurWindow("fragment_blur_window")
    }
}
```

## 🎯 实际应用场景示例

### 场景1: 对话框背景模糊

```kotlin
class DialogBlurActivity : AppCompatActivity() {
    private lateinit var blurHelper: GlobalBlurHelper
    
    fun showBlurDialog() {
        val dialogWidth = 600
        val dialogHeight = 400
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        
        // 创建对话框背景模糊
        blurHelper.createBlurWindow(
            id = "dialog_background",
            x = (screenWidth - dialogWidth) / 2,
            y = (screenHeight - dialogHeight) / 2,
            width = dialogWidth,
            height = dialogHeight,
            blurRadius = 25f,
            cornerRadius = 16f,
            overlayColor = Color.parseColor("#80000000"),
            autoUpdate = false  // 静态模糊
        )
        
        // 显示对话框
        showDialog()
    }
    
    fun dismissDialog() {
        // 移除模糊背景
        blurHelper.removeBlurWindow("dialog_background")
    }
}
```

### 场景2: 底部菜单模糊背景

```kotlin
class BottomMenuActivity : AppCompatActivity() {
    private lateinit var blurHelper: GlobalBlurHelper
    
    fun showBottomMenu() {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val menuHeight = 400
        
        // 创建底部菜单模糊背景
        blurHelper.createBlurWindow(
            id = "bottom_menu_bg",
            x = 0,
            y = screenHeight - menuHeight,
            width = screenWidth,
            height = menuHeight,
            blurRadius = 20f,
            cornerRadius = 0f,
            overlayColor = Color.parseColor("#70000000"),
            autoUpdate = true
        )
    }
    
    fun hideBottomMenu() {
        blurHelper.removeBlurWindow("bottom_menu_bg")
    }
}
```

### 场景3: 列表项悬浮模糊效果

```kotlin
class ListBlurFragment : Fragment() {
    private lateinit var blurHelper: GlobalBlurHelper
    
    fun onItemHover(itemPosition: Int, itemView: View) {
        val location = IntArray(2)
        itemView.getLocationOnScreen(location)
        
        // 创建悬浮项模糊背景
        blurHelper.createBlurWindow(
            id = "hover_item_bg",
            x = location[0],
            y = location[1],
            width = itemView.width,
            height = itemView.height,
            blurRadius = 15f,
            cornerRadius = 8f,
            overlayColor = Color.parseColor("#40000000"),
            autoUpdate = true,
            updateInterval = 50L
        )
    }
    
    fun onItemHoverEnd() {
        blurHelper.removeBlurWindow("hover_item_bg")
    }
}
```

### 场景4: 动态模糊效果

```kotlin
class DynamicBlurActivity : AppCompatActivity() {
    private lateinit var blurHelper: GlobalBlurHelper
    
    fun animateBlurEffect() {
        // 创建模糊窗口
        blurHelper.createBlurWindow(
            id = "animated_blur",
            x = 100, y = 100, width = 300, height = 200,
            blurRadius = 5f,
            autoUpdate = true
        )
        
        // 动态调整模糊强度
        val animator = ValueAnimator.ofFloat(5f, 25f)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val radius = animation.animatedValue as Float
            blurHelper.updateBlurRadius("animated_blur", radius)
        }
        animator.start()
    }
}
```

## ⚠️ 注意事项

### 1. 权限检查

```kotlin
// 在创建窗口前检查权限
if (!blurHelper.isReady()) {
    Toast.makeText(this, "请先授权必要权限", Toast.LENGTH_SHORT).show()
    return
}
```

### 2. 窗口ID管理

```kotlin
// 使用有意义的窗口ID
private const val DIALOG_BLUR_ID = "dialog_blur"
private const val MENU_BLUR_ID = "menu_blur"

// 检查窗口是否存在
if (blurHelper.hasBlurWindow(DIALOG_BLUR_ID)) {
    // 窗口已存在
}
```

### 3. 资源释放

```kotlin
// 及时释放不使用的窗口
override fun onStop() {
    super.onStop()
    blurHelper.removeBlurWindow("temporary_blur")
}

// Activity/Fragment销毁时释放所有资源
override fun onDestroy() {
    super.onDestroy()
    blurHelper.release()
}
```

### 4. 性能优化

```kotlin
// 对于静态内容，禁用自动更新
blurHelper.createBlurWindow(
    id = "static_blur",
    // ...
    autoUpdate = false
)

// 对于动态内容，使用适当的更新间隔
blurHelper.createBlurWindow(
    id = "dynamic_blur",
    // ...
    autoUpdate = true,
    updateInterval = 200L  // 较长的更新间隔
)
```

## 📚 完整示例

查看以下文件获取完整示例代码：

- **Activity示例**: [GlobalBlurActivityExample.kt](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/globalblur/GlobalBlurActivityExample.kt)
- **Fragment示例**: [GlobalBlurFragmentExample.kt](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/globalblur/GlobalBlurFragmentExample.kt)

## 🔗 相关文档

- [主README文档](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/globalblur/README.md)
- [API参考文档](file:///c:/Users/Work/AndroidStudioProjects/MineAndroid/core/core-ui/src/main/java/com/cn/core/ui/view/globalblur/README.md#-api-参考)
