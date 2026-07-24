package com.cn.board.home.domain.state

/**
 * 基于 SessionStateHolder 的业务逻辑选择器。
 *
 * 替代原 [com.cn.board.home.function.Function] 中依赖可变全局 [State] 的扩展函数。
 * 读取的是 StateFlow 快照值，确保在协程内一致性。
 */

/**
 * bpModel 模式判断：false 走 action（普通模式），true 走 action1（bp 模式）。
 */
inline fun <T, R> T.resolveBpModel(action: T.() -> R, bpAction: T.() -> R): R {
    return if (!SessionStateHolder.bpModel) action(this) else bpAction(this)
}

/**
 * MCU/CommandDispatcher 三路选择。
 */
inline fun <T, R> T.resolveModel(
    normal: T.() -> R,
    mcu: T.() -> R,
    commandDispatcher: T.() -> R
): R {
    return when {
        SessionStateHolder.commandDispatcherModel -> commandDispatcher(this)
        SessionStateHolder.mcuModel -> mcu(this)
        else -> normal(this)
    }
}

/**
 * MCU 模式判断。
 */
inline fun <T, R> T.resolveMcModel(mcuAction: T.() -> R, normalAction: T.() -> R): R {
    return if (SessionStateHolder.mcuModel) mcuAction(this) else normalAction(this)
}

/**
 * CommandDispatcher 模式判断。
 */
inline fun <T, R> T.resolveCommandDispatcher(
    cdAction: T.() -> R,
    normalAction: T.() -> R
): R = if (SessionStateHolder.commandDispatcherModel) cdAction(this) else normalAction(this)

/**
 * SM 模式判断。
 */
inline fun <T, R> T.resolveSmModel(smAction: T.() -> R, normalAction: T.() -> R): R =
    if (SessionStateHolder.smModel) smAction(this) else normalAction(this)

/**
 * 会议中状态判断。
 */
inline fun <T> T.ifInConf(inAction: T.() -> Unit, notInAction: T.() -> Unit) {
    if (SessionStateHolder.isInConf) inAction(this) else notInAction(this)
}
