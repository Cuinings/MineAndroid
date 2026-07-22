# Msg Subscriber Processor (消息订阅注解处理器)

编译期注解处理器（APT / KAPT），为消息订阅系统自动生成代码。

## 功能

- 基于 `@AutoService(Processor::class)` 注册
- 处理 `@Subscriber` 注解
- 自动生成订阅回调代码（`SubscriberGenerate`、`SubscriberCallbackGenerate`）
- 使用 JavaPoet + KotlinPoet 生成源代码
- 运行时提取 `applicationId` 用于跨进程消息路由

## 模块类型

Kotlin/Java 注解处理器（`com.cn.core:msg-subscriber-processor:1.0.0`）

## 依赖

- core:core-remote:msg-subscriber-annotation
