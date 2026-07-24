package com.cn.board.home.function

import com.cn.board.home.domain.state.SessionStateHolder

/**
 * 业务逻辑选择函数。
 *
 * 全部委托给 Domain 层的 [SessionStateHolder] 的 StateFlow 快照值，
 * 确保线程安全。不再依赖全局可变 State 或全局 CoroutineScope。
 */

inline fun <T, R> T.bpModel(action: T.() -> R, action1: T.() -> R): R {
    return if (!SessionStateHolder.bpModel) action(this) else action1(this)
}

inline fun <T, R> T.modelReturn(common: T.() -> R, mcu: T.() -> R, commandDispatcher: T.() -> R): R {
    return when {
        SessionStateHolder.commandDispatcherModel -> commandDispatcher(this)
        SessionStateHolder.mcuModel -> mcu(this)
        else -> common(this)
    }
}

inline fun <T, R> T.mcModel(mcAction: T.() -> R, action: T.() -> R): R {
    return if (SessionStateHolder.mcuModel) mcAction(this) else action(this)
}

inline fun <T, R> T.commandDispatcherModel(commandDispatcher: T.() -> R, action: T.() -> R): R =
    if (SessionStateHolder.commandDispatcherModel) commandDispatcher(this) else action(this)

inline fun <T, R> T.smModel(smAction: T.() -> R, action: T.() -> R): R =
    if (SessionStateHolder.smModel) smAction(this) else action(this)

inline fun <T> T.ifConf(inAction: T.() -> Unit, notInAction: T.() -> Unit) {
    if (SessionStateHolder.isInConf) inAction(this) else notInAction(this)
}
