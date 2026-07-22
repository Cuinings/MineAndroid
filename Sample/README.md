# Sample (跨进程通信示例)

演示和验证跨进程消息路由（IPC）功能的示例应用集合。

## 子项目

### remote_library_test_one
- 同时依赖 msg-router-client 和 msg-router-service
- 模拟拥有路由器 Service 的进程

### remote_library_test_two
- 仅依赖 msg-router-client
- 模拟纯客户端进程

## 用途

验证 core-remote 系列模块在多 App 进程间的消息路由通信能力。
