# Home (Launcher 首页)

智慧平板的主桌面（Launcher）模块。

## 功能

- 注册为 `CATEGORY_HOME` 意图过滤器，可作为系统桌面替代
- 应用抽屉（AppAggregator）与应用聚合展示
- Room 数据库集成（通过 database 模块）
- 动态壁纸集成
- LeakCanary 内存泄露检测

## 模块类型

Android Library（`com.cn.board.meet.home`）

## 核心类

- `HomeActivity` — 首页主 Activity
- `AppAggregator` — 应用聚合管理
