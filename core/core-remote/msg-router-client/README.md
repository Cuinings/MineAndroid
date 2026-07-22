# Msg Router Client (消息路由客户端)

基于 andlinker 的跨进程消息订阅 / 发布系统客户端。

## 功能

- `MsgRouter` — 单例路由器（消息绑定、订阅管理）
- `MsgRouterProvider` — 路由提供者
- 注解驱动：`@Subscribe`、`@Subscriber`、`@Dispatcher`
- `MsgCallback` — 消息回调接口
- 使用 AndroidX Startup 自动初始化

## 模块类型

Android Library（`com.cn.core:msg-router-client:1.0.0`）

## 依赖

- core:core-remote:andlinker
- core:core-remote:msg-subscriber-annotation
