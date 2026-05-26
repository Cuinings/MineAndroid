# FrostedGlowStateful 系列 View 使用说明

## 概述

FrostedGlowStateful 系列 View 提供了一套完整的状态化发光视图组件，支持圆角、多状态背景、描边、内发光、流光动画和毛玻璃效果。该系列包含三个核心类：

| 类名 | 继承关系 | 核心特性 |
| --- | --- | --- |
| `StatefulGlowView` | ConstraintLayout | 圆角、多状态背景、描边、内发光、顶部高光 |
| `AnimatedStatefulGlowView` | StatefulGlowView | 增加流光效果、刷新扫描动画 |
| `FrostedAnimatedGlowView` | AnimatedStatefulGlowView | 增加实时毛玻璃模糊效果 |

---

## 1. StatefulGlowView

### 1.1 功能特性

- **圆角支持**：支持统一圆角或四角独立圆角
- **多状态背景**：支持 normal/pressed/focused/selected/disabled 五种状态
- **描边效果**：支持不同状态的描边颜色和宽度
- **内发光效果**：多层 EVEN_ODD 环形实现的高性能内发光
- **顶部高光**：模拟上方光源反射的高光效果

### 1.2 XML 属性

```xml
<com.cn.core.ui.view.frosted.StatefulGlowView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    
    <!-- 圆角设置 -->
    app:sgv_cornerRadius="16dp"              <!-- 统一圆角 -->
    app:sgv_cornerRadiusTL="8dp"             <!-- 左上角圆角 -->
    app:sgv_cornerRadiusTR="8dp"             <!-- 右上角圆角 -->
    app:sgv_cornerRadiusBR="8dp"             <!-- 右下角圆角 -->
    app:sgv_cornerRadiusBL="8dp"             <!-- 左下角圆角 -->
    
    <!-- 蒙层设置 -->
    app:sgv_overlayEnabled="true"            <!-- 是否启用蒙层 -->
    app:sgv_overlayColorNormal="#20FFFFFF"   <!-- 默认状态蒙层颜色 -->
    app:sgv_overlayColorPressed="#30FFFFFF"  <!-- 按下状态蒙层颜色 -->
    app:sgv_overlayColorFocused="#40FFFFFF"  <!-- 焦点状态蒙层颜色 -->
    app:sgv_overlayColorSelected="#30FFFFFF" <!-- 选中状态蒙层颜色 -->
    app:sgv_overlayColorDisabled="#10FFFFFF" <!-- 禁用状态蒙层颜色 -->
    
    <!-- 描边设置 -->
    app:sgv_strokeEnabled="true"             <!-- 是否启用描边 -->
    app:sgv_strokeWidthNormal="2dp"          <!-- 默认状态描边宽度 -->
    app:sgv_strokeWidthPressed="2dp"         <!-- 按下状态描边宽度 -->
    app:sgv_strokeWidthFocused="3dp"         <!-- 焦点状态描边宽度 -->
    app:sgv_strokeColorNormal="#FFFFFF"      <!-- 默认状态描边颜色 -->
    app:sgv_strokeColorFocused="#00D4FF"     <!-- 焦点状态描边颜色 -->
    
    <!-- 内发光设置 -->
    app:sgv_glowEnabled="true"               <!-- 是否启用内发光 -->
    app:sgv_glowRadiusNormal="20dp"          <!-- 默认状态发光半径 -->
    app:sgv_glowRadiusFocused="30dp"         <!-- 焦点状态发光半径 -->
    app:sgv_glowColorNormal="#80FFFFFF"      <!-- 默认状态发光颜色 -->
    app:sgv_glowColorFocused="#A000D4FF"     <!-- 焦点状态发光颜色 -->
    
    <!-- 顶部高光设置 -->
    app:sgv_topHighlightEnabled="true"       <!-- 是否启用顶部高光 -->
    app:sgv_topHighlightColor="#CCFFFFFF"    <!-- 高光颜色 -->
    />
```

### 1.3 代码 API

```kotlin
val view = StatefulGlowView(context)

// 设置圆角
view.setCornerRadius(16f)
view.setCornerRadii(8f, 8f, 16f, 16f)  // tl, tr, br, bl

// 设置蒙层颜色
view.setOverlayColorForState(Color.parseColor("#20FFFFFF"), StatefulGlowView.State.NORMAL)

// 设置描边
view.setStrokeWidthForState(2f, StatefulGlowView.State.NORMAL)
view.setStrokeColorForState(Color.WHITE, StatefulGlowView.State.NORMAL)

// 设置内发光
view.setGlowRadiusForState(20f, StatefulGlowView.State.FOCUSED)
```

### 1.4 状态说明

| 状态 | 触发条件 | 说明 |
| --- | --- | --- |
| NORMAL | 默认状态 | 无交互时的状态 |
| PRESSED | 触摸按下 | `setPressed(true)` |
| FOCUSED | 获取焦点 | `requestFocus()` |
| SELECTED | 被选中 | `setSelected(true)` |
| DISABLED | 不可用 | `setEnabled(false)` |

---

## 2. AnimatedStatefulGlowView

### 2.1 功能特性

继承 `StatefulGlowView`，新增：

- **流光效果**：沿描边顺时针循环的高亮效果，仅在焦点状态显示
- **刷新扫描效果**：对角线方向的扫描动画

### 2.2 XML 属性

```xml
<com.cn.core.ui.view.frosted.AnimatedStatefulGlowView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    
    <!-- 继承自 StatefulGlowView 的属性 -->
    app:sgv_cornerRadius="16dp"
    app:sgv_strokeEnabled="true"
    app:sgv_glowEnabled="true"
    
    <!-- 流光效果 -->
    app:asgv_flowEnabled="true"              <!-- 是否启用流光 -->
    app:asgv_flowColor="#CCFFFFFF"           <!-- 流光颜色 -->
    app:asgv_flowDuration="2500"             <!-- 流光一圈时长(ms) -->
    app:asgv_flowSegmentRatio="0.22"         <!-- 流光段占路径比例 [0.05, 0.6] -->
    
    <!-- 刷新扫描效果 -->
    app:asgv_refreshEffectEnabled="true"     <!-- 是否启用刷新效果 -->
    app:asgv_refreshColor="#64FFFFFF"        <!-- 刷新扫描颜色 -->
    />
```

### 2.3 代码 API

```kotlin
val view = AnimatedStatefulGlowView(context)

// 流光控制
view.flowEnabled = true
view.flowColor = Color.parseColor("#CCFFFFFF")
view.flowDurationMs = 2500L
view.flowSegmentRatio = 0.22f

// 刷新效果控制
view.refreshEffectEnabled = true
view.refreshColor = Color.parseColor("#64FFFFFF")

// 手动控制动画
view.startFlow()
view.stopFlow()
view.startRefreshAnimation()
view.stopRefreshAnimation()
```

### 2.4 流光原理

1. 使用 `PathMeasure` 获取圆角矩形路径的总长度
2. 通过 `ValueAnimator` 驱动 progress ∈ [0, 1) 持续循环
3. 根据 progress 在路径上截取一段弧长作为"发光段"
4. 发光段使用 `LinearGradient`（透明→亮色→透明）绘制，形成拖尾效果
5. 当段跨越路径终点时自动拆分为两段绘制（保证无缝衔接）

---

## 3. FrostedAnimatedGlowView

### 3.1 功能特性

继承 `AnimatedStatefulGlowView`，新增：

- **实时毛玻璃效果**：支持从 SurfaceView 或 Bitmap 截取并模糊
- **协程优化**：结构化并发、Channel 通信、对象池复用
- **自适应帧率**：根据性能动态调整更新频率

### 3.2 XML 属性

```xml
<com.cn.core.ui.view.frosted.FrostedAnimatedGlowView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    
    <!-- 继承自父类的属性 -->
    app:sgv_cornerRadius="16dp"
    app:asgv_flowEnabled="true"
    
    <!-- 毛玻璃设置 -->
    app:fagv_blurEnabled="true"              <!-- 是否启用模糊 -->
    app:fagv_blurRadius="15dp"               <!-- 模糊半径 [0, 25] -->
    app:fagv_blurScaleFactor="0.25"          <!-- 缩放因子 [0.1, 1] -->
    />
```

### 3.3 代码 API

```kotlin
val view = FrostedAnimatedGlowView(context)

// 方式一：从 SurfaceView 获取内容
view.surfaceView = mySurfaceView

// 方式二：从 Bitmap 获取内容
view.sourceBitmap = myBitmap
view.sourceBitmapOffsetX = 0  // Bitmap 在屏幕上的偏移
view.sourceBitmapOffsetY = 0
view.sourceBitmapScale = 1f   // 缩放比例

// 模糊参数
view.blurEnabled = true
view.blurRadius = 15f
view.blurScaleFactor = 0.25f

// 控制
view.startRealtimeBlur()
view.stopRealtimeBlur()

// 性能优化
view.adaptiveFrameRate = true  // 自适应帧率
view.frameSkipCount = 1        // 帧跳过数
```

### 3.4 性能优化策略

| 优化项 | 实现方式 |
| --- | --- |
| **对象池** | 预分配 Bitmap、Canvas、Matrix 等对象复用 |
| **协程并发** | 截图在主线程，模糊在 IO 线程，Channel 通信 |
| **帧丢弃** | 使用 CONFLATED Channel，只保留最新帧 |
| **自适应帧率** | 根据帧时间动态调整更新频率 |
| **内存管理** | 监听内存压力，自动回收资源 |

### 3.5非首页使用

继承blurEanble设置为false

```
WallpaperManager.getInstance(context).drawable?.toBitmap()?.let {
            blurBitmap(context, it, 15f, 0.1f)?.let {
                staticWallpaperBlurBitmap = it
                staticWallpaperBlurBitmapFlow.tryEmit(it)//发送后页面接收后实际更新
            }
        }
        
            staticWallpaperBlurBitmapFlow.collectByScope(lifecycleScope) {
                binding.activityContactContent.sourceBitmap = it
            }
```

---

## 4. 使用示例

### 4.1 基础用法（带发光效果的按钮）

```xml
<com.cn.core.ui.view.frosted.StatefulGlowView
    android:id="@+id/glowButton"
    android:layout_width="200dp"
    android:layout_height="56dp"
    android:clickable="true"
    android:focusable="true"
    app:sgv_cornerRadius="28dp"
    app:sgv_strokeEnabled="true"
    app:sgv_strokeWidthNormal="2dp"
    app:sgv_strokeColorNormal="#FFFFFF"
    app:sgv_strokeColorFocused="#00D4FF"
    app:sgv_glowEnabled="true"
    app:sgv_glowRadiusNormal="15dp"
    app:sgv_glowRadiusFocused="25dp"
    app:sgv_glowColorNormal="#40FFFFFF"
    app:sgv_glowColorFocused="#6000D4FF">
    
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="发光按钮"
        android:textColor="#FFFFFF"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"/>
        
</com.cn.core.ui.view.frosted.StatefulGlowView>
```

### 4.2 带流光效果的输入框容器

```xml
<com.cn.core.ui.view.frosted.AnimatedStatefulGlowView
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:focusable="true"
    android:focusableInTouchMode="true"
    app:sgv_cornerRadius="8dp"
    app:sgv_strokeEnabled="true"
    app:sgv_strokeWidthNormal="1dp"
    app:sgv_strokeWidthFocused="2dp"
    app:sgv_strokeColorNormal="#40FFFFFF"
    app:sgv_strokeColorFocused="#00D4FF"
    app:sgv_glowEnabled="true"
    app:sgv_glowRadiusFocused="20dp"
    app:sgv_glowColorFocused="#3000D4FF"
    app:asgv_flowEnabled="true"
    app:asgv_flowColor="#CCFFFFFF"
    app:asgv_flowDuration="2000">
    
    <EditText
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@null"
        android:textColor="#FFFFFF"
        android:hint="请输入内容"
        android:padding="16dp"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"/>
        
</com.cn.core.ui.view.frosted.AnimatedStatefulGlowView>
```

### 4.3 毛玻璃卡片

```xml
<com.cn.core.ui.view.frosted.FrostedAnimatedGlowView
    android:id="@+id/frostedCard"
    android:layout_width="300dp"
    android:layout_height="200dp"
    app:sgv_cornerRadius="16dp"
    app:sgv_strokeEnabled="true"
    app:sgv_strokeWidthNormal="1dp"
    app:sgv_strokeColorNormal="#80FFFFFF"
    app:sgv_glowEnabled="true"
    app:sgv_glowRadiusNormal="30dp"
    app:sgv_glowColorNormal="#20FFFFFF"
    app:fagv_blurEnabled="true"
    app:fagv_blurRadius="12dp"
    app:fagv_blurScaleFactor="0.3">
    
    <!-- 卡片内容 -->
    
</com.cn.core.ui.view.frosted.FrostedAnimatedGlowView>
```

```kotlin
// 设置毛玻璃源
val frostedCard = findViewById<FrostedAnimatedGlowView>(R.id.frostedCard)
frostedCard.surfaceView = mySurfaceView  // 或 sourceBitmap
frostedCard.startRealtimeBlur()
```

---

## 5. 注意事项

### 5.1 性能建议

1. **内发光层数**：默认 32 层，可通过修改 `INNER_GLOW_LAYERS` 调整，层数越高效果越平滑但性能消耗越大
2. **模糊半径**：建议在 10-20dp 之间，过大的模糊半径会严重影响性能
3. **缩放因子**：建议使用 0.25-0.5，较小的缩放因子可显著提升模糊性能

### 5.2 内存管理

- `FrostedAnimatedGlowView` 会创建多个 Bitmap，建议在视图销毁时调用 `stopRealtimeBlur()`
- 监听 `onTrimMemory` 回调，在内存紧张时主动释放资源

### 5.3 兼容性

- `PixelCopy` 需要 API 26+
- `RenderScript` 需要在 build.gradle 中启用：
  ```gradle
  android {
      buildFeatures {
          renderscript true
      }
  }
  ```

---

## 6. 类继承关系

```
ConstraintLayout
    └── StatefulGlowView          (基础状态化发光视图)
            └── AnimatedStatefulGlowView  (增加流光和刷新动画)
                    └── FrostedAnimatedGlowView  (增加毛玻璃效果)
```

---

## 7. 状态切换时序

```
NORMAL ──(触摸按下)──> PRESSED ──(松开)──> NORMAL
    │                     │
    │                     └──(获取焦点)──> FOCUSED
    │                                           │
    └──(setSelected)──> SELECTED                │
    │                                           │
    └──(setEnabled=false)──> DISABLED <─────────┘
```

所有状态切换均有 200ms 的平滑过渡动画。
