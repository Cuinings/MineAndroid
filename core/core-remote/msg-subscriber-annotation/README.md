# Msg Subscriber Annotation (消息订阅注解)

消息订阅系统的注解定义模块。

## 注解

- `@Subscriber` — 标记消息订阅方法
  - `@Target(FUNCTION)`, `@Retention(RUNTIME)`
  - 参数：`subscribeId`

## 模块类型

纯 Kotlin/Java 注解库（`com.cn.core:msg-subscriber-annotation:1.0.0`）

## 依赖关系

被 `msg-subscriber-processor` 在编译期处理，被 `msg-router-client` 在运行期使用。
