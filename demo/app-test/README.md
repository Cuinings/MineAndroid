# App Test (集成测试应用)

独立的集成测试与功能演示应用。

## 功能

- 依赖 core-ui、core-utils、core-resources 进行集成验证
- 使用 Google Play Core 加载 `app_test_resources` 动态功能模块
- 演示 Fragment 切换的三种策略：
  - show / hide
  - attach / detach
  - replace
- 包含 SplashActivity、GuideActivity、MainActivity

## 模块类型

Android Application（`com.cn.app.test`）

## 测试

完整的单元测试环境（JUnit 5 + Mockito + Truth）。
