# Proxy (主应用壳工程)

智慧平板的主入口应用，负责模块路由和运行时模块加载。

## 功能

- 通过 `ModuleConfig` 控制运行时加载哪个业务模块（home / meet / contacts / wallpaper）
- 使用系统签名运行（`sharedUserId="android.uid.system"`）
- 使用 `BuildConfig` 开关控制各模块启用/禁用

## 模块类型

Android Application（`com.cn.board.proxy`）

## 依赖

- board:module:home
- board:module:meet
- board:module:contacts
- board:module:wallpaper
