# ToSetting - Android 系统设置跳转工具

一个用于跳转各种 Android 系统设置页面的工具库，支持不同手机品牌的差异化适配。

## 功能特性

- ✅ **35个常用设置项** - 覆盖应用、权限、网络、系统四大类
- ✅ **22种手机品牌适配** - 小米、华为、OPPO、VIVO、三星等
- ✅ **密封类架构** - 类型安全，编译时检查
- ✅ **DSL风格API** - 简洁优雅的调用方式
- ✅ **扩展函数** - `Context.openSetting()` 一行调用

## 快速开始

### 基础调用

```kotlin
// 打开单个设置
openSetting(Setting.PermissionSettings)

// 打开多个设置
openSettings(Setting.WifiSettings, Setting.BluetoothSettings)

// 按分类打开
openSettingsByCategory(SettingCategory.NETWORK)
```

### DSL风格配置

```kotlin
// 获取所有设置
val all = settings { }

// 按分类获取
val networkSettings = settingsByCategory(SettingCategory.NETWORK)

// 只获取品牌差异化设置
val brandSpecific = brandSpecificSettings()

// 自定义过滤
val filtered = settings {
    filter { it.requiresBrandSpecific }
    category(SettingCategory.PERMISSION)
}
```

### 执行器配置

```kotlin
SettingExecutor.init {
    showErrorToast = true
    onResult { setting, success ->
        // 日志记录、埋点统计
    }
}
```

## 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                        表现层 (Presentation)                      │
├─────────────────────────────────────────────────────────────────┤
│  MainActivity.kt          SettingAdapter.kt                      │
│  - 显示手机信息            - RecyclerView适配器                   │
│  - 初始化执行器            - 设置项展示                           │
│  - 展示设置列表                                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        业务层 (Business)                          │
├─────────────────────────────────────────────────────────────────┤
│  Setting.kt (密封类)          SettingExecutor.kt                 │
│  - 定义所有设置项             - 执行设置跳转                       │
│  - 类型安全                   - 配置管理                          │
│  - 编译时检查                 - 结果回调                          │
│                                                                  │
│  SettingBuilder.kt              SettingCategory.kt               │
│  - DSL风格构建器               - 设置分类枚举                      │
│  - 过滤设置项                  - APP / PERMISSION                 │
│  - 分类筛选                    - NETWORK / SYSTEM                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        实现层 (Implementation)                    │
├─────────────────────────────────────────────────────────────────┤
│  SettingHelper.kt                                                │
│  - 手机品牌识别 (22种品牌)                                        │
│  - 通用设置跳转 (Android SDK标准API)                              │
│  - 品牌适配跳转 (小米/华为/OPPO/VIVO/三星/魅族等)                   │
└─────────────────────────────────────────────────────────────────┘
```

## 核心文件

| 文件 | 职责 | 设计模式 |
|------|------|----------|
| `Setting.kt` | 定义所有设置项 | 密封类 (Sealed Class) |
| `SettingExecutor.kt` | 执行设置跳转 | 执行器模式 + 扩展函数 |
| `SettingBuilder.kt` | DSL构建器 | 构建者模式 (Builder) |
| `SettingHelper.kt` | 底层实现 | 工具类 + 策略模式 |
| `SettingAdapter.kt` | 列表适配器 | 适配器模式 (Adapter) |

## 设置项列表

### 应用相关 (APP) - 3个

| 设置项 | 说明 |
|--------|------|
| `AppSettings` | 应用详情设置页面 |
| `AllAppsSettings` | 所有应用列表 |
| `DefaultAppsSettings` | 默认应用设置 (Android 7.0+) |

### 权限管理 (PERMISSION) - 12个

| 设置项 | 说明 | 品牌适配 |
|--------|------|----------|
| `PermissionSettings` | 应用权限设置 | ✅ |
| `NotificationSettings` | 应用通知设置 | - |
| `AutoStartSettings` | 自启动管理 | ✅ |
| `BatteryOptimization` | 电池优化设置 | ✅ |
| `IgnoreBatteryOptimization` | 请求忽略电池优化 | - |
| `FloatingWindow` | 悬浮窗权限 | ✅ |
| `BackgroundSettings` | 后台运行设置 | ✅ |
| `InstallUnknownApps` | 安装未知应用权限 (Android 8.0+) | - |
| `PictureInPicture` | 画中画权限 (Android 8.0+) | - |

### 网络设置 (NETWORK) - 9个

| 设置项 | 说明 |
|--------|------|
| `WifiSettings` | WiFi设置页面 |
| `BluetoothSettings` | 蓝牙设置页面 |
| `LocationSettings` | 位置服务设置 |
| `MobileDataSettings` | 移动数据设置 |
| `NetworkSettings` | 网络运营商设置 |
| `AirplaneModeSettings` | 飞行模式设置 |
| `DataUsageSettings` | 流量使用设置 |
| `VpnSettings` | VPN设置 |
| `NfcSettings` | NFC设置 |

### 系统设置 (SYSTEM) - 11个

| 设置项 | 说明 |
|--------|------|
| `DisplaySettings` | 显示设置页面 |
| `SoundSettings` | 声音设置页面 |
| `StorageSettings` | 存储设置页面 |
| `MemorySettings` | 内存设置页面 |
| `BatterySettings` | 电池设置页面 |
| `SecuritySettings` | 安全设置页面 |
| `PrivacySettings` | 隐私设置页面 |
| `LanguageSettings` | 语言设置页面 |
| `DateSettings` | 日期时间设置 |
| `AccessibilitySettings` | 无障碍设置 |
| `DeveloperSettings` | 开发者选项 |
| `AboutPhoneSettings` | 关于手机 |
| `UserSettings` | 用户设置 |
| `PrintSettings` | 打印设置 |

## 品牌适配支持

支持以下手机品牌的差异化设置跳转：

| 品牌 | 自启动 | 电池优化 | 悬浮窗 | 后台运行 |
|------|:------:|:--------:|:------:|:--------:|
| 小米/红米 | ✅ | ✅ | ✅ | ✅ |
| 华为 | ✅ | ✅ | ✅ | ✅ |
| 荣耀 | ✅ | ✅ | ✅ | ✅ |
| OPPO | ✅ | ✅ | ✅ | ✅ |
| VIVO | ✅ | ✅ | ✅ | ✅ |
| 三星 | ✅ | ✅ | ✅ | - |
| 魅族 | ✅ | - | - | - |
| 一加 | - | - | - | - |
| Realme | - | - | - | - |
| iQOO | - | - | - | - |
| 中兴 | - | - | - | - |
| 努比亚 | - | - | - | - |
| 红魔 | - | - | - | - |
| 联想 | - | - | - | - |
| 摩托罗拉 | - | - | - | - |
| 诺基亚 | - | - | - | - |
| HTC | - | - | - | - |
| 索尼 | - | - | - | - |
| LG | - | - | - | - |
| 华硕 | - | - | - | - |
| 谷歌 | - | - | - | - |

## 扩展指南

### 新增设置项

1. 在 `SettingHelper.kt` 添加跳转方法：

```kotlin
fun openNewSettings(context: Context): Boolean {
    return openSettingsSafely(context, Settings.ACTION_NEW_SETTINGS)
}
```

2. 在 `Setting.kt` 添加密封类对象：

```kotlin
object NewSettings : Setting(
    title = "新设置",
    description = "打开新设置页面",
    iconResId = R.drawable.ic_new,
    category = SettingCategory.SYSTEM
) {
    override fun execute(context: Context): Boolean = SettingHelper.openNewSettings(context)
}
```

3. 在 `Setting.all` 列表中添加新设置项

### 新增品牌适配

在 `SettingHelper.kt` 中添加品牌识别和对应的设置跳转方法：

```kotlin
// 1. 在 PhoneBrand 枚举中添加
NEW_BRAND

// 2. 在 getPhoneBrand() 方法中添加识别逻辑
manufacturer.contains("newbrand") || brand.contains("newbrand") -> PhoneBrand.NEW_BRAND

// 3. 添加品牌特定的跳转方法
private fun openNewBrandAutoStartSettings(context: Context): Boolean {
    // 实现跳转逻辑
}
```

## 依赖要求

- minSdk: 24 (Android 7.0)
- targetSdk: 35 (Android 15)
- Kotlin: 1.9+

## 许可证

MIT License
