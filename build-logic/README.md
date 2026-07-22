# Build Logic (构建约定插件)

项目级 Gradle 复合构建（Composite Build），提供统一的构建约定插件系统。

## 结构

- `conventions/` — 约定插件定义
- `plugins/` — 公共插件配置

## 提供的插件

| 插件 ID | 用途 |
|---------|------|
| `mineandroid.android.application` | Android 应用模块通用配置 |
| `mineandroid.android.library` | Android 库模块通用配置 |
| `mineandroid.android.dynamic-feature` | 动态功能模块通用配置 |
| `mineandroid.kotlin.library` | 纯 Kotlin/Java 库通用配置 |

## 统一管理

- SDK 版本（`AndroidSdkVersions`）
- 代码质量检查（detekt、ktlint）
- 发布配置（maven-publish）
- 测试配置
