# Core UI (核心 UI 基础库)

提供项目统一使用的 UI 基础设施和可复用组件。

## 核心能力

### Activity / Fragment 基类
- `BasicActivity`、`BasicVmDBActivity`、`BasicFragment` 等
- 支持 ViewBinding、DataBinding、ViewModel

### MVI 架构支持
- `BasicMviViewModel`、`MultiStateMVIViewModel`
- `UiIntent`、`UiState`、`UiEffect`

### RecyclerView 适配器
- `BaseQuickAdapter` — 高度封装的通用适配器
- 多布局、DiffUtil、拖拽、加载更多

### Jetpack Compose
- Material3 UI 组件集成

### 自定义 View
- MicrophoneView、BatteryView、MarqueeTextView、PagedTextView

### 视觉效果
- 毛玻璃 / 模糊效果（`BackgroundBlurDrawable`、`FrostedAnimatedGlowView`、`GlobalBlurService`）

### 国际化
- `LocaleHelper`、`LanguagePickerDialog`、`TranslationImporter`

## 模块类型

Android Library（`com.cn.core:ui:1.0.0`）

## 依赖

- core:core-resources
