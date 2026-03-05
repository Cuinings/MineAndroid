# MineAndroid 项目

## 项目介绍

MineAndroid 是一个综合性的 Android 应用开发项目，包含多个功能模块和演示应用。该项目采用模块化架构设计，旨在提供一个完整的 Android 应用开发框架，涵盖了从基础工具类到复杂功能实现的各个方面。

## 技术栈

### 核心技术
- **语言**: Kotlin 2.1.20
- **构建工具**: Gradle 8.9.2
- **Android Gradle Plugin**: 8.6.0

### 主要依赖
- **AndroidX 核心库**
  - Core: 1.15.0
  - AppCompat: 1.7.0
  - Activity: 1.9.3
  - Fragment: 1.8.8
  - ConstraintLayout: 2.1.4

- **Lifecycle 组件**: 2.8.7
- **Compose 相关**: 2024.12.01
- **依赖注入**: Hilt 2.56
- **网络请求**: OkHttp 4.12.0, Retrofit 2.11.0
- **图片加载**: Glide 4.16.0
- **架构组件**: Room 2.7.0

### 开发工具
- **IDE**: Android Studio
- **代码生成**: KSP, KAPT
- **测试框架**: JUnit, Espresso, Mockito

## 项目结构

```
MineAndroid/
├── .idea/                 # IDE 配置文件
├── .kotlin/               # Kotlin 编译缓存
├── .vscode/               # VS Code 配置
├── Sample/                # 示例项目
│   ├── remote_library_test_one/   # 远程库测试项目1
│   └── remote_library_test_two/   # 远程库测试项目2
├── board/                 # 看板模块
│   ├── contacts/          # 联系人功能
│   ├── database/          # 数据库功能
│   ├── home/              # 主页功能
│   ├── meet/              # 会议相关功能
│   └── proxy/             # 代理功能
├── build-logic/           # 构建逻辑
│   ├── conventions/       # 约定配置
│   └── plugins/           # 自定义插件
├── core/                  # 核心模块
│   ├── core-resources/    # 核心资源
│   ├── core-ui/           # 核心UI组件
│   ├── core-utils/        # 核心工具类
│   └── core-remote/       # 远程通信相关
│       ├── andlinker/     # 链接器
│       ├── msg-router-client/    # 消息路由客户端
│       ├── msg-router-service/   # 消息路由服务
│       ├── msg-subscriber-annotation/   # 消息订阅注解
│       └── msg-subscriber-processor/    # 消息订阅处理器
├── demo/                  # 演示模块
│   ├── app-test/          # 应用测试
│   ├── app_test_resources/    # 测试资源
│   └── other-test/        # 其他测试
├── maven-repo/            # 本地 Maven 仓库
├── tools/                 # 工具文件
├── .gitignore             # Git 忽略文件
├── Jenkins配置指南.md     # Jenkins 配置指南
├── TESTING_GUIDELINES.md  # 测试指南
├── build.gradle.kts       # 项目构建文件
├── gradle.properties      # Gradle 属性配置
├── gradlew                # Gradle 包装器脚本
├── gradlew.bat            # Gradle 包装器批处理文件
├── settings.gradle.kts    # 项目设置文件
└── update-plugins.gradle.kts  # 插件更新脚本
```

## 功能特性

### 1. 核心模块 (core)

核心模块提供了项目的基础功能组件，包括：

- **core-utils**
  - 工具类集合
  - 域名正则匹配
  - 网络工具
  - 邮箱工具
  - IP 工具
  - 防抖与节流工具
  - Gson 相关工具类
  - 应用、设备、文件、加密等工具类

- **core-ui**
  - 基础 UI 组件
  - Activity、Fragment、Service 基类
  - ViewModel 实现（包括 MVI 架构）
  - PopupWindow 基类
  - Flow 扩展函数
  - 应用上下文扩展

- **core-resources**
  - 共享资源文件
  - 颜色定义
  - 尺寸定义
  - 资源扩展函数

- **core-remote**
  - 远程通信相关功能
  - 消息路由（客户端和服务端）
  - 链接器（AndLinker）
  - 消息订阅注解和处理器

### 2. 演示模块 (demo)

演示模块提供了各种功能的示例应用，包括：

- **app-test**
  - 基础 MVVM 架构示例
  - 状态管理演示
  - 副作用处理演示

- **other-test**
  - 相机控制示例
  - 控制面板示例
  - 毛玻璃效果示例
  - RecyclerView 示例

### 3. 看板模块 (board)

看板模块提供了多种业务功能，包括：

- **contacts**
  - 联系人管理功能
  - 组织架构展示

- **database**
  - 本地数据库功能
  - 应用信息存储

- **home**
  - 主页功能
  - 应用列表展示

- **meet**
  - 会议功能演示

- **proxy**
  - 代理功能
  - 模块配置管理

## 安装部署

### 环境要求

- **JDK**: 17 或更高版本
- **Android Studio**: 2023.1.1 或更高版本
- **Android SDK**: API Level 34 或更高
- **Gradle**: 8.9 或更高版本

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <项目仓库地址>
   cd MineAndroid
   ```

2. **打开项目**
   - 启动 Android Studio
   - 选择 "Open an existing project"
   - 导航到 MineAndroid 项目目录并选择

3. **同步依赖**
   - Android Studio 会自动检测并提示同步项目
   - 点击 "Sync Now" 按钮开始同步

4. **构建项目**
   - 选择 "Build" -> "Make Project" 或使用快捷键 Ctrl+F9

### 运行模块

1. **选择运行模块**
   - 在 Android Studio 的顶部工具栏中，选择要运行的模块（如 demo:app-test、board:proxy 等）

2. **选择设备**
   - 连接真实设备或启动模拟器
   - 在运行配置中选择目标设备

3. **运行应用**
   - 点击 "Run" 按钮或使用快捷键 Shift+F10

## 使用方法

### 演示应用模块

1. **app-test**
   - 展示了基础的 MVVM 架构实现
   - 包含计数器、加载状态等演示功能
   - 演示了状态管理和副作用处理

2. **other-test**
   - 相机控制：演示相机操作功能
   - 控制面板：演示各种 UI 控件的使用
   - 毛玻璃效果：演示自定义视图效果
   - RecyclerView：演示列表视图的高级用法

## 开发指南

### 模块化开发

项目采用模块化架构，每个模块都有明确的职责：

- **core 模块**：提供基础功能和工具类，供其他模块依赖
- **demo 模块**：提供功能演示和测试
- **board 模块**：实现特定业务功能

### 代码风格

- 遵循 Kotlin 官方代码风格指南
- 使用 4 个空格进行缩进
- 类名使用 PascalCase，方法和变量名使用 camelCase
- 常量使用大写字母加下划线

### 测试指南

项目包含完整的测试配置，详细测试指南请参考 [TESTING_GUIDELINES.md](TESTING_GUIDELINES.md) 文件。

### CI/CD 配置

项目支持 Jenkins 持续集成，详细配置指南请参考 [Jenkins配置指南.md](Jenkins配置指南.md) 文件。

## 贡献指南

1. **Fork 项目**
2. **创建特性分支** (`git checkout -b feature/AmazingFeature`)
3. **提交更改** (`git commit -m 'Add some AmazingFeature'`)
4. **推送到分支** (`git push origin feature/AmazingFeature`)
5. **打开 Pull Request**

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 LICENSE 文件

## 联系方式

- **项目维护者**: [Cui Ning]
- **邮箱**: [1015597172@qq.com]
- **GitHub**: [Your GitHub Profile]

## 更新日志

### 版本 1.0.0
- 初始化项目结构
- 实现核心模块功能
- 实现演示应用模块

### 版本 1.0.1
- 修复已知问题

### 版本 1.0.2
- 更新技术栈版本
  - Android Gradle Plugin: 8.6.0
  - Room: 2.7.0
- 扩展看板模块功能
  - 添加 contacts 子模块（联系人管理）
  - 添加 database 子模块（本地数据库）
  - 添加 home 子模块（主页功能）
  - 添加 proxy 子模块（代理功能）
- 完善核心模块结构
  - 扩展 core-ui 模块，添加 Activity、Fragment、ViewModel 等基类
  - 增强 core-utils 模块，添加 Gson 相关工具类
  - 优化 core-remote 模块结构
- 更新项目文档

---

感谢您使用 MineAndroid 项目！如有任何问题或建议，欢迎联系我们。