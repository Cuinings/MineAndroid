# MineAndroid 项目记忆

## 项目概况
- Android 原生项目，Kotlin 技术栈，包名 com.cn.core.ui
- Gradle 8.7, AGP 8.6.0, Kotlin 2.1.20；核心模块 core-ui（自定义 View 层）

## 继承链 / 分层绘制规范（2026-07-02）
- ConstraintLayout → StatefulGlowView → AnimatedStatefulGlowView → (BlurView / FrostedAnimatedGlowView)；两者各自直接继承 AnimatedStatefulGlowView（平行实现）；WallpaperBlurView 为独立自包含视图
- StatefulGlowView: `drawAllDecorations()`(overlay/glow/stroke/highlight) + `dispatchDrawChildren()`(直接 ConstraintLayout.dispatchDraw)
- AnimatedStatefulGlowView: Layer2(overlay+glow)→Layer3(children)→Layer4(stroke+highlight+flow+refresh)
- FrostedAnimatedGlowView: Layer1(blur)→Layer2→Layer3→Layer4；stroke/highlight 必须画在 children 上方
- 子类 dispatchDraw 不走 super 链，避免装饰重复+层级混乱
- 设计约束：setBackground 被 override 为空，子类经 `setBackgroundInternal()` 绕过；仅 View 区域模糊，区域外壁纸清晰

## BlurView（core/core-ui/.../frosted/BlurView.kt）
- 继承：ConstraintLayout → StatefulGlowView → AnimatedStatefulGlowView → BlurView（直接继承，无 BaseBlurView）
- 2026-07-06 曾做架构优化（新增 BaseBlurView 基类 + 剥离动态壁纸死代码 + DEBUG=false + 修 backgroundBlurOnly 全屏泄漏），**同日用户要求"恢复到修改前"已整体回退**至优化前原状（原 blob 经 git fsck 从悬挂对象找回恢复）
- 当前（回退后）真实状态：
  - 6 模式 NONE/SYSTEM/FALLBACK/BITMAP/WALLPAPER/OVERLAY
- **2026-07-09**：路径2(A3 Overlay/动态壁纸 SurfaceControl 子层反射)已屏蔽，`tryActivateOverlay()` 首行 `return false`，原逻辑块注释保留；动态壁纸现在 fall through 到 SYSTEM（Aggregator）或 FALLBACK 路径
  - DEBUG=true（生产环境 diagRecheckRunnable 仍 500/1500/3000ms 反射进 ViewRootImpl 做诊断）
  - backgroundBlurOnly 仍走机制 A/Bitmap 窗口级模糊（全屏泄漏未修复）
  - 死文件仍在：BlurViewOld.kt（包 com.cn.blur.v）/ FrostedAnimatedGlowView1.kt / ScreenLayerCapture.kt
- SYSTEM 模式约束（通用知识，再次优化时可复用）：
  - 进入 setLayerType(NONE)，退出恢复 HARDWARE（否则硬件层遮 SurfaceFlinger 合成）
  - isCrossWindowBlurEnabled 前置检查（OEM 禁用→降级 FALLBACK）
  - 三机制同步清理 A+B+C：deactivateBlur SYSTEM 分支 + BlurMaskManager.unregister(refCount=0 先清 A+B)

## BackgroundBlurDrawable 模式（2026-06-25）
- 反射创建系统 BackgroundBlurDrawable(API31+)，设为背景→SurfaceFlinger 合成
- View 移动仅 setBlurRegion(l,t,r,b)→零延时；不走 new/re-setBackground/invalidate
- 文件：BackgroundBlurDrawable.kt、BackgroundBlurViewHelper.kt；FrostedAnimatedGlowView 经 useBackgroundBlurMode=true 启用

## WallpaperBlurView（独立自包含视图，非 BlurView 继承链）
- V2(2026-06-02): Compositor Blur 主路径 setBlurBehindRadius+FLAG_BLUR_BEHIND；OEM 检测 isCrossWindowBlurEnabled
- V2 黑屏修复：坐标转 decorView 画布、getOpacity=TRANSLUCENT、Compositor 模式 LAYER_TYPE_NONE
- V3 动态壁纸(2026-06-03): isLiveWallpaper→全屏截屏+裁剪渲染+2s 刷新；禁 getDrawable()
- 内发光：EVEN_ODD 32层环形+高斯σ=0.35+1.5x亮度；顶部高光焦点显示；支持 .9 图片替代（逐状态，`sgv_glowDrawable*` 属性，`StateDrawables` 五态数据类）

## 状态机（StatefulGlowView，2026-07-07 修复）
- **设计约束**：`drawableStateChanged()` = 状态切换唯一入口；`setPressed/setSelected/onFocusChanged/setEnabled` 禁止独立 `animateStateChange()`
- `selfEnabled` 字段替代 `isEnabled`：Android `isEnabled` 递归祖先链导致父级临时禁用时误判 DISABLED
- `setEnabled(enabled)` 在 `super.setEnabled()` 之前更新 `selfEnabled`，防止 `drawableStateChanged` 读到过期值

## 流光效果（AnimatedStatefulGlowView，2026-07-07 优化）
- **与焦点解耦**：流光仅由 `flowEnabled` 控制，不再依赖 `isFocused`；refresh 扫描仍由焦点驱动
- 显示条件：`flowEnabled && isAttachedToWindow && flowAlpha>=0.01f`（drawFlowEffect 内部 + FrostedAnimatedGlowView.dispatchDraw + flowEnabled setter + onAttachedToWindow 全部去掉 isFocused）
- 流光宽度 = `focusedStrokeWidth`（紧贴描边中心，不溢出；原 *1.4f 已移除）
- 顺时针：`addRoundRect(..., Path.Direction.CW)` + progress 递增沿 CW 推进
- 不间断：`ValueAnimator INFINITE/RESTART/LinearInterpolator` + 跨末尾拆段（端点 L≡0 重合无缝）
- `gainFlowOnFocus()` 默认 true，语义改为"是否在焦点变化时启停 refresh"（流光已不受其控制）
- `setEnabled(true)` 时若 flowEnabled 已开自动 startFlow 恢复

## 属性前缀
- sgv_ / asgv_ / fagv_(useBackgroundBlurMode/backgroundBlurRadius/cornerRadius) / wbv_ / bv_

## 性能/其他
- 生产 DEBUG=false，inline log 替代 Log；FallbackBlurDrawable ScrShotCache 缓存+RenderEffect 缓存
- BlurPopupWindow(2026-06-29): BlurPopupWindow+PopupWindowBlurHelper，blurRadius[0,25]/dimAmount[0,1]/blurEnabled

## FrostedVideoOverlay（2026-07-08）
- 路径 C：BackgroundBlurDrawable Aggregator 反射，per-View 模糊，零开销
- TextureView 视频覆盖场景专用 FrameLayout；enableWindowPipeline=false 避免 Window 级模糊；仅设 FLAG_BLUR_BEHIND
- 属性前缀 fvo_（blurRadius/cornerRadius/overlayColor）
