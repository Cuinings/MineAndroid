# 类 Wallpaper Engine 壁纸系统方案（System Plan）

> 本文是宏观「系统方案」，覆盖**产品范围、总体架构、子系统划分、能力矩阵、性能/兼容/测试目标、风险与路线图**。
> 渲染框架的微观设计（接口、渲染器、跨进程配置）见姊妹文档 [`WALLPAPER_ENGINE_DESIGN.md`](./WALLPAPER_ENGINE_DESIGN.md)。
>
> 当前代码基座：`:board:module:wallpaper`（运行于 `:wallpaper` 进程）、`:board:module:home`（入口）、`WallpaperSettingsActivity`（设置）。

---

## 0. 文档定位与读者

| 读者 | 关注点 |
|------|--------|
| 技术负责人 / 架构评审 | 分层、进程模型、技术决策、风险 |
| 客户端开发 | 子系统职责、扩展新壁纸类型的成本 |
| 产品 / 项目经理 | 能力矩阵、落地路线图、范围边界 |
| 测试 | 测试策略、兼容性矩阵、性能指标 |

---

## 1. 方案目标与产品范围

### 1.1 目标
在 Android Live Wallpaper（动态壁纸）之上，构建一套**「一个引擎、多类型壁纸、可插拔渲染器、运行时无缝切换」**的壁纸系统，对标 Wallpaper Engine 的核心能力，但落地为符合 Android 系统约束的轻量实现。

### 1.2 能力对标 Wallpaper Engine（移动侧合理裁剪）

| Wallpaper Engine 能力 | 本系统 | 说明 |
|------------------------|--------|------|
| 视频壁纸 | ✅ 已实现 | MediaCodec 硬解直渲，零回读 |
| 图片 / 轮播 / 视差 | ✅ 已实现 | Canvas 直绘，多图轮播 + 桌面分页视差 |
| Web / HTML 壁纸 | 🔲 预留 | 扩展点已留，未实现 |
| 2D / 3D 场景壁纸 | 🔲 预留 | 扩展点已留，未实现 |
| 运行时切换 / 参数调整 | ✅ 已实现 | 跨进程广播，热切换 |
| 功耗策略（充电/低电量） | ✅ 已实现 | 低电量节流 |
| 音频 | ⛔ 范围外 | 桌面壁纸场景默认静音 |
| 编辑器 / 创意工坊 | ⛔ 范围外 | 服务端生态，非客户端范畴 |

### 1.3 范围内 / 范围外

- **范围内**：视频、图片/轮播、视差；跨进程配置；可见性 + 电量策略；统一渲染抽象；设置页 + 入口。
- **范围外（本期）**：Web/3D 渲染器实现、音频、创意工坊/编辑器、云端壁纸市场。

---

## 2. 系统总体架构

### 2.1 进程模型

```
┌─────────────────── 主进程 (UI) ───────────────────┐
│  HomeActivity  ──▶ 入口(右下角"壁纸设置")           │
│  WallpaperSettingsActivity ──▶ 选择类型/参数        │
│        │                                          │
│        └─ WallpaperConfigStore.save() ─┐          │
└────────────────────────────────────────┼──────────┘
                                          │ ① 持久化(SP) + ② 应用内广播
                                          ▼
┌─────────────── :wallpaper 进程 (壁纸服务) ──────────┐
│  BoardWallpaperService                            │
│    └─ BoardWallpaperEngine (驱动层)                │
│         ├─ 转发 Surface/可见性/偏移/销毁             │
│         ├─ 订阅 WallpaperConfigStore.Listener       │
│         └─ createRenderer(type) ──┐                 │
│                                   ▼                 │
│            WallpaperRenderer (统一接口)              │
│              ├─ VideoWallpaperRenderer              │
│              └─ ImageWallpaperRenderer              │
│                 (Web/GL 渲染器 = 扩展位)            │
└────────────────────────────────────────────────────┘
```

要点：
- **壁纸服务必须运行在独立进程**（系统要求 `WallpaperService` 进程存活以持续绘制），本系统用 `android:process=":wallpaper"`。
- **主进程只负责「配置」不负责「绘制」**；两进程通过「持久化 + 广播」解耦，互不持有对方引用。

### 2.2 分层职责（宏观）

| 层 | 模块 / 类 | 职责 |
|----|-----------|------|
| 入口层 | `HomeActivity`、`WallpaperSettingsActivity` | 触发设置、引导系统设为动态壁纸 |
| 配置层 | `WallpaperConfigStore` | 跨进程持久化 + 广播通知 |
| 服务/驱动层 | `BoardWallpaperService` + `BoardWallpaperEngine` | 进程存活、生命周期转发、渲染器调度 |
| 渲染抽象层 | `WallpaperRenderer` | 统一生命周期契约 |
| 渲染实现层 | `Video/Image/WallpaperRenderer` | 具体绘制，互不耦合 |

### 2.3 关键数据流

1. **设置 → 生效**：Activity 调 `WallpaperConfigStore.save()` → SP 写入 + 广播 → `:wallpaper` 进程 `Listener` 收 → `Engine.onConfigChanged()` → 释放旧/创建新渲染器或调 `renderer.onConfigChanged()`。
2. **系统 → 渲染**：`SurfaceHolder` 就绪 / 可见性变化 / 桌面分页偏移 → `Engine` 转发 → 对应渲染器方法。

---

## 3. 子系统设计

### 3.1 渲染引擎子系统
- **统一契约**：`WallpaperRenderer`（`attach` / `onSurfaceChanged` / `onVisibilityChanged` / `onOffsetsChanged` / `onConfigChanged` / `release` / `isRunning`）。
- **调度器**：`BoardWallpaperService.createRenderer(type)` 按 `WallpaperType` 分流；类型切换只动这一行，不动引擎与配置层——这是抽象的核心价值。
- **已实现渲染器**：
  - `VideoWallpaperRenderer`：MediaCodec 硬解直渲 `TYPE_WALLPAPER` Surface，零回读、零中间 Bitmap；`flush()+seekTo(0)` 循环；可见性切换复用 decoder。
  - `ImageWallpaperRenderer`：Canvas `lockCanvas` 直绘；center-crop；多图轮播；桌面分页视差；下采样 + `RGB_565` 控内存；解码/绘制跑独立 `HandlerThread`，跨线程字段 `@Volatile`。

### 3.2 配置与跨进程通信子系统
- **持久化**：`SharedPreferences`（原子写），服务重建时 `load()` 兜底。
- **实时通知**：`onSharedPreferenceChangeListener` 跨进程不可靠，改用**本包应用内广播** `ACTION_CONFIG_CHANGED`，服务进程收后 `load()` + 派发。
- **模型**：`WallpaperConfig`（type / videoPath / imagePaths / carouselIntervalMs / parallax / fpsCap），含 `video()` / `image()` 便捷构造。
- **向后兼容**：保留 `BoardWallpaperService.wallpaperPath`（`@Deprecated`），setter 转调 `WallpaperConfigStore.save()`。

### 3.3 生命周期与可见性子系统
- 桌面隐藏 → `onVisibilityChanged(false)` → 暂停解码/轮播（省电）；可见 → 恢复。
- `attach` 与 `onVisibilityChanged(true)` 顺序不固定，渲染器对「surface 就绪不可见」「可见但 surface 未就绪」两种状态均做幂等处理。
- `release()` 可重入、线程安全，多显示器每个显示区对应一个 Engine，天然支持。

### 3.4 性能策略子系统
- 视频：保持解码器时序帧率（体验优先）。
- 图片轮播：按 `carouselIntervalMs` 间隔切换。

### 3.5 设置与入口子系统
- `WallpaperSettingsActivity`：视频 / 图片双类型 UI，保存即经 `WallpaperConfigStore` 生效。
- `HomeActivity`：右下角「壁纸设置」入口，遵循项目 MVI（导航走 `renderEffect` 副作用）：点击 → `dispatch(OpenWallpaperSettings)` → ViewModel → `startActivity`。

---

## 4. 关键技术决策（ADR）

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| 视频渲染 | MediaCodec 直渲 vs TextureView+SurfaceTexture | **MediaCodec 直渲壁纸 Surface** | 零回读、零中间 Bitmap、CPU 占用最低；壁纸必须直渲 `TYPE_WALLPAPER` 层，不能用 `core-ui` 的 `CodecLoopVideoView`(TextureView) |
| 图片渲染 | OpenGL vs Canvas | **Canvas 2D** | 静态/轮播场景无需 GPU 管线开销，实现简单、兼容性好 |
| 跨进程通信 | ContentProvider / AIDL / SP+广播 | **SP + 应用内广播** | 配置是低频小数据，广播足够；避免 AIDL 复杂度与 CP 权限 |
| 渲染器调度 | 注册表 vs 简单 when | **when 分流** | 类型有限且稳定，简单可读；类型爆炸时再升级为注册表 |
| 多类型内存 | 多实例 vs 单一复用 | **类型切换重建** | 视频/图片资源模型差异大，重建比复用更稳，切换频率低可接受 |

---

## 5. 能力矩阵与实现状态

| 子系统 | 状态 | 负责人域 | 备注 |
|--------|------|----------|------|
| 渲染抽象 `WallpaperRenderer` | ✅ 已完成 | 框架 | 接口稳定 |
| 视频渲染器 | ✅ 已完成 | 框架 | 硬解直渲 |
| 图片/轮播/视差 | ✅ 已完成 | 框架 | Canvas 直绘 |
| 跨进程配置 | ✅ 已完成 | 配置 | SP+广播 |
| 设置页 + 入口 | ✅ 已完成 | UI | 双类型 UI + Home 入口 |
| Web / HTML 渲染器 | 🔲 未实现 | 扩展位 | 需 WebView→Surface 方案 |
| GL 2D/3D 场景渲染器 | 🔲 未实现 | 扩展位 | 需 EGL 绑壁纸 Surface |
| 音频 | ⛔ 范围外 | — | 桌面壁纸默认静音 |
| 长列表 OOM 防御 | 🟡 部分 | 健壮性 | 已下采样，超长列表需懒加载/磁盘缓存 |

---

## 6. 性能指标（目标值）

| 指标 | 视频壁纸 | 图片轮播 |
|------|----------|----------|
| 单帧 CPU 占用 | 硬解 ≈ 0（解码器承担） | < 2% (中端机) |
| 内存增量 | 解码器 buffer < 8MB | 单图 < 4MB（RGB_565） |
| 帧率 | 视频源帧率 | 静态不重绘 / 切图瞬时 |
| 桌面隐藏时功耗 | 暂停解码 0 功耗 | 停止轮播 0 功耗 |

---

## 7. 兼容性矩阵

| 维度 | 现状 / 约束 |
|------|-------------|
| Android 版本 | `minSdk` 由模块 `build.gradle.kts` 决定；MediaCodec 直渲需 API ≥ 16，视差 `onOffsetsChanged` 依赖桌面支持 |
| 厂商 ROM | 部分 ROM 限制动态壁纸 / 后台进程保活；需在真机回归（尤其国产 ROM 省电策略） |
| 双进程 | `:wallpaper` 进程被系统回收后由 `WallpaperManager` 重启，`load()` 兜底恢复配置 |
| 多显示器 | 每显示区一个 Engine，已天然支持 |

---

## 8. 测试策略

| 层级 | 范围 | 手段 |
|------|------|------|
| 单元 | `WallpaperConfig` 构造、`WallpaperConfigStore` 读写、center-crop 计算、电量节流逻辑 | JUnit（纯逻辑可测部分） |
| 集成 | 配置保存 → 广播 → 渲染器切换 | `:wallpaper` 模块 Robolectric / 真机 |
| 系统 | 桌面隐藏/可见、充电/拔电、多图轮播、视频循环、跨进程重建 | 真机 + 自动化脚本 |
| 兼容 | 主流厂商 ROM、不同分辨率/折叠屏 | 真机矩阵回归 |

---

## 9. 风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| `:wallpaper` 进程被厂商 ROM 回收 | 壁纸短暂消失 | `load()` 兜底 + 引导用户加白名单 |
| MediaCodec 某机型解码失败 | 视频壁纸黑屏 | try/catch 降级为图片/纯色 + 上报 |
| WebView→Surface 方案复杂 | Web 壁纸延期 | 先用离屏渲染 `lockCanvas` 拷贝，性能不足再评估 TextureView 复用 |
| 长轮播列表 OOM | 闪退 | 懒加载 + 磁盘缓存（后续） |
| 跨进程广播收不到 | 切换不生效 | SP 兜底 + 启动/重建时 `load()` |

---

## 10. 落地路线图（里程碑）

| 里程碑 | 内容 | 状态 |
|--------|------|------|
| M1 渲染框架 | 统一接口 + 视频/图片渲染器 + 配置/电量 | ✅ 完成 |
| M2 入口与设置 | 设置页双类型 + Home 入口 MVI | ✅ 完成 |
| M3 健壮性 | 解码降级、进程回收兜底、OOM 防御 | 🟡 部分 |
| M4 Web 壁纸 | `WebWallpaperRenderer` + 设置 UI | 🔲 待启动 |
| M5 场景壁纸 | `GLWallpaperRenderer` + 示例着色器 | 🔲 待启动 |
| M6 真机兼容回归 | 厂商 ROM / 折叠屏矩阵 | 🔲 待启动 |

---

## 11. 与现有文档/代码的映射

- 微观渲染设计 → [`WALLPAPER_ENGINE_DESIGN.md`](./WALLPAPER_ENGINE_DESIGN.md)
- 入口实现 → `board/module/home/.../HomeActivity.kt`、`activity_home.xml`
- 渲染框架代码 → `board/module/wallpaper/src/main/java/com/cn/board/wallpaper/`（`BoardWallpaperService` / `WallpaperRenderer` / `VideoWallpaperRenderer` / `ImageWallpaperRenderer` / `WallpaperConfig` / `WallpaperConfigStore`）
- 设置页 → `board/module/wallpaper/.../WallpaperSettingsActivity.kt`

> 扩展新壁纸类型只需 6 步（见 DESIGN 文档第 8 节），无需改动驱动层、配置层、策略层。
