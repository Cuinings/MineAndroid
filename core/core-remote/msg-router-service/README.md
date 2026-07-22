# Msg Router Service (消息路由服务端)

跨进程消息路由服务，运行在独立进程 `:MsgRouter` 中。

## 功能

- 接收各客户端进程的消息订阅请求
- 管理消息分发（`MsgRouterManager`）
- 实现 `MsgRouterAPIImpl` — 消息路由 API 具体实现
- 通过 `MsgRouterServiceInitializer` 使用 AndroidX Startup 自启动

## 模块类型

Android Library（`com.cn.core:msg-router-service:1.0.0`）

## 进程

独立进程：`:MsgRouter`

## 依赖

- core:core-remote:andlinker
- core:core-remote:msg-router-client
