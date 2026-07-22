# 类 Wallpaper Engine 壁纸渲染架构方案

> 目标：在 Android Live Wallpaper 上，用**统一渲染接口**支撑多种壁纸类型（视频 / 图片轮播 / 视差），
> 像 Wallpaper Engine 那样「一套框架、可插拔渲染器、运行时无缝切换」，并预留 Web / 3D 场景扩展位。

---

## 1. 背景与现状

项目原本已有一个 `BoardWallpaperService`，用 **MediaCodec 直渲** 把 MP4 解码后直接画到壁纸
Surface（`TYPE_WALLPAPER` 层），属于「视频壁纸」。它的优点（零回读、零中间 Bitmap、性能最佳）保留，
但痛点也很明显：

- 渲染逻辑与 `WallpaperService.Engine` 耦合，新增图片/Web/3D 类型要改服务本身；
- 配置靠一个全局 `wallpaperPath` 变量，跨进程（`:wallpaper` 进程）不实时、不可扩展；
- 没有统一的生命周期 / 可见性策略。

本方案把「**驱动**」与「**渲染**」彻底分离：

- `BoardWallpaperEngine` 只负责把 `SurfaceHolder` 生命周期、可见性、分页偏移、电量状态**转发**给渲染器；
- `WallpaperRenderer` 是所有壁纸类型的统一契约；
- `VideoWallpaperRenderer` / `ImageWallpaperRenderer` 各自实现，互不影响。

---

## 2. 整体架构

```
┌─────────────────────────────── :wallpaper 进程 ───────────────────────────────┐
│                                                                                │
│   BoardWallpaperService                                                        │
│      │  onCreateEngine()                                                       │
│      ▼                                                                          │
│   BoardWallpaperEngine  ── 转发生命周期 ──┐                                      │
│      │   ├─ onSurfaceCreated / Changed    │                                     │
│      │   ├─ onVisibilityChanged           │                                     │
│      │   ├─ onOffsetsChanged (视差)        ├──▶  WallpaperRenderer (接口)         │
│      │   └─ onDestroy                      │      ├─ VideoWallpaperRenderer     │
│      │                                     │      ├─ ImageWallpaperRenderer     │
│      │  订阅 WallpaperConfigStore.Listener │      └─ (未来) Web/GL Renderer      │
│      │  注册配置广播接收器                  │                                     │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘
        ▲                                              │
        │  WallpaperConfigStore.save() + 广播           │ 读/写配置
        │  (SharedPreferences + 应用内广播)             ▼
┌─────────────────────────────── 主进程 ────────────────────────────────────────┐
│   WallpaperSettingsActivity  ──▶ WallpaperConfigStore.save(config)             │
│   (选择 视频 / 图片轮播，引导系统设为 Live Wallpaper)                           │
└────────────────────────────────────────────────────────────────────────────────┘
```

**分层职责**

| 层 | 类 | 职责 |
|----|----|------|
| 入口层 | `BoardWallpaperService` | 进程内单例、兼容 `wallpaperPath`、创建 Engine |
| 驱动层 | `BoardWallpaperEngine` | 把系统回调转发给渲染器；订阅配置广播 |
| 渲染抽象层 | `WallpaperRenderer` | 统一生命周期契约（attach / 可见性 / 偏移 / 配置 / release） |
| 渲染实现层 | `VideoWallpaperRenderer`、`ImageWallpaperRenderer` | 具体渲染，互不耦合 |
| 配置层 | `WallpaperConfigStore` | 跨进程持久化 + 广播通知 |

---

## 3. 核心抽象：WallpaperRenderer

```kotlin
interface WallpaperRenderer {
    fun attach(holder: SurfaceHolder)                       // 拿到壁纸 Surface
    fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int)
    fun onVisibilityChanged(visible: Boolean)               // 桌面可见=渲染，隐藏=暂停
    fun onOffsetsChanged(xOffset: Float, yOffset: Float,
                         xStep: Float, yStep: Float,
                         xPixels: Int, yPixels: Int) {}      // 桌面分页视差（默认空）
    fun onConfigChanged(config: WallpaperConfig)             // 运行时切壁纸/改参数
    fun release()                                           // 释放资源（可重入）
    fun isRunning(): Boolean
}
```

**约定**

- `attach` 与 `onVisibilityChanged(true)` 的先后顺序不固定，实现需对
  「surface 已就绪但不可见」和「可见但 surface 未就绪」两种情况都做幂等处理。
- `release()` 必须可重入且线程安全。
- `onOffsetsChanged` 有默认空实现，按需重写。

---

## 4. 渲染类型实现

### 4.1 视频壁纸（已实现，MediaCodec 直渲）
- 文件：`VideoWallpaperRenderer.kt`
- 策略：MediaExtractor 选视频轨 → MediaCodec 硬解 →
  `codec.configure(format, wallpaperSurface, null, 0)` → `releaseOutputBuffer(outIdx, true)` 直渲。
- 解码器输出 Surface = 壁纸 Surface，**全程零回读、零中间 Bitmap**。
- 循环播放靠 `flush()` + `seekTo(0)`；可见性切换复用 decoder 避免重建开销。

### 4.2 图片 / 轮播 / 视差（已实现，Canvas 直绘）
- 文件：`ImageWallpaperRenderer.kt`
- 渲染：`SurfaceHolder.lockCanvas()` → center-crop 绘制 → `unlockCanvasAndPost()`。
- 轮播：`imagePaths.size > 1 且 carouselIntervalMs > 0` 时按间隔切图。
- 视差：`onOffsetsChanged` 拿到 `xOffset`/`xStep`，在图片溢出宽度内做水平位移（多桌面分页时生效）。
- 内存：按屏幕尺寸 `inSampleSize` 下采样，`RGB_565` 解码；所有解码/绘制跑在独立 `HandlerThread`，
  共享字段用 `@Volatile`。

### 4.3 Web / HTML 壁纸（预留）
- 新增 `WebWallpaperRenderer`：在 `Surface` 上挂载 `WebView` / Chromium，
  把网页渲染输出到壁纸 Surface（需 `Surface` 兼容的 WebView 方案，或离屏渲染后 `lockCanvas` 拷贝）。
- `createRenderer` 增加 `WallpaperType.WEB` 分支 + `WebWallpaperConfig`。

### 4.4 2D / 3D 场景壁纸（预留）
- 新增 `GLWallpaperRenderer`：`GLSurfaceView`/EGL 上下文绑定到壁纸 Surface，
  用 OpenGL/Vulkan 绘制粒子、着色器等实时场景。
- `createRenderer` 增加 `WallpaperType.SCENE` 分支。

---

## 5. 跨进程配置：WallpaperConfigStore

壁纸服务运行在 `android:process=":wallpaper"`，设置入口在主进程。配置传递靠两条腿：

1. **持久化**：`SharedPreferences`（文件原子写）保存最新配置，服务启动/重建时 `load()` 读取。
2. **实时通知**：`onSharedPreferenceChangeListener` 跨进程不可靠，改用在 `setPackage(本应用)` 的
   **应用内广播** `ACTION_CONFIG_CHANGED`，服务进程收到后 `load()` + 派发给订阅者。

```kotlin
// 设置端（主进程）
WallpaperConfigStore.save(context, WallpaperConfig.video(path))
WallpaperConfigStore.save(context, WallpaperConfig.image(listOf(a, b), carouselIntervalMs = 5000))

// 服务进程（:wallpaper）
WallpaperConfigStore.subscribe(object : WallpaperConfigStore.Listener {
    override fun onConfigChanged(config: WallpaperConfig) { /* 无缝切换 */ }
})
```

**运行时切换类型**：Engine 收到新配置，若 `type` 变化 → 释放旧渲染器、创建新渲染器、重新绑定当前
Surface；否则仅调 `renderer.onConfigChanged(config)`。

---

## 6. 生命周期策略

- **可见性**：桌面隐藏 → `onVisibilityChanged(false)` → 暂停解码/轮播（省电）；可见 → 恢复。

---

## 7. 与现有代码的映射

| 原文件 | 现状 | 本方案 |
|--------|------|--------|
| `BoardWallpaperService.kt` | 渲染逻辑内联在 Engine | 改为「驱动层」，转发给 `WallpaperRenderer` |
| `WallpaperSettingsActivity.kt` | 直接写 `wallpaperPath` | 改用 `WallpaperConfigStore`，支持视频/图片两种类型 UI |
| `home/WALLPAPER_SETUP.md` | 文档提到 `HomeWallpaperManager`/`HomeWallpaperUtil`（实际未实现） | 统一走 `WallpaperConfigStore` + 本框架 |
| `core-ui/CodecLoopVideoView.kt` | TextureView 方案（普通 View 用） | 壁纸必须直渲 `TYPE_WALLPAPER` 层，故视频渲染仍用裸 MediaCodec，不复用此 View |

**向后兼容**：保留 `BoardWallpaperService.wallpaperPath` 属性（已标 `@Deprecated`），
其 setter 内部转调 `WallpaperConfigStore.save(...)`，外部旧调用仍可工作。

---

## 8. 扩展一种新壁纸类型（步骤）

1. 在 `WallpaperType` 枚举加一项（如 `WEB`）。
2. 在 `WallpaperConfig` 增加对应字段 + 便捷构造（如 `WebWallpaperConfig`）。
3. 新建 `XxxWallpaperRenderer : WallpaperRenderer`，实现生命周期方法。
4. 在 `BoardWallpaperService.createRenderer` 增加 `WallpaperType.WEB -> XxxWallpaperRenderer(config)`。
5. （可选）在 `WallpaperSettingsActivity` 增加对应 UI 入口。
6. 无需改动 `BoardWallpaperEngine`、配置层、电量层 —— 这就是抽象的价值。

---

## 9. 已知限制与后续

- **Web/3D 渲染器未实现**：仅预留接口与扩展点，需后续补齐（见 4.3 / 4.4）。
- **音频**：当前视频壁纸仅解码视频轨（与原实现一致），如需声音可扩展 `AudioTrack`。
- **多显示器**：每个显示区对应一个 Engine，已天然支持；若需「不同屏不同壁纸」可扩展配置 key。
- **OOM 防御**：图片解码已下采样，但超长轮播列表建议改为懒加载/磁盘缓存。
- **编译验证**：本方案为可运行骨架，需在 Android 工程中随 `:board:wallpaper` 模块一起编译（依赖
  `androidx.core`、`appcompat`、`material` 及 `:core:core-utils`）。
