# Database (共享数据库)

智慧平板各模块共用的数据库层。

## 功能

- 基于 Room 持久化库实现
- 定义 `AppDatabase`（包含 `AppInfo` 和 `CommonNode` 两张表）
- 提供通用的 `CommonNode` CRUD 操作
- Repository 模式 + ViewModel 支持
- 通过 `DatabaseInitProvider`（ContentProvider）实现无侵入自动初始化

## 模块类型

Android Library（`com.cn.board.database`）

## 使用者

- board:module:home
- board:module:contacts
