# AndLinker (跨进程通信基础设施)

Android 跨进程 Service 绑定与通信的基础设施层。

## 功能

- `RemoteService` — 远程服务基类
- `RemoteApiManager` — 远程 API 代理创建和回调管理（`BinderUnit` 缓存）
- `RemoteBuilder` — Builder 模式构建远程连接
- 支持绑定 / 解绑、回调注册、API 缓存

## 模块类型

Android Library（`com.cn.core:andlinker:1.0.0`）

## 定位

整个消息路由系统（msg-router-* 系列）的底层 IPC 通信基础。
