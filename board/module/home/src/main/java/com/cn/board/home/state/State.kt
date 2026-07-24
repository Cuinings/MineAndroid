package com.cn.board.home.state

import com.cn.board.home.domain.state.SessionStateHolder

/**
 * 全局状态对象（兼容层）。
 *
 * Phase 2: 所有读写委托给 [SessionStateHolder] 的 StateFlow，
 * 确保线程安全和单向数据流。旧代码可无缝迁移。
 *
 * Phase 3 TODO: 逐步消除所有直接引用，最终移除此兼容层。
 */
object State {

    var isLoadingApp: Boolean
        get() = SessionStateHolder.isLoadingApp
        set(value) { SessionStateHolder.setIsLoadingApp(value) }

    var isSendAss: Boolean
        get() = SessionStateHolder.isSendAss
        set(value) { SessionStateHolder.update { it.copy(isSendAss = value) } }

    var isInConf: Boolean
        get() = SessionStateHolder.isInConf
        set(value) { SessionStateHolder.setIsInConf(value) }

    var vrsLoginState: Boolean
        get() = SessionStateHolder.vrsLoginState
        set(value) { SessionStateHolder.setVrsLoginState(value) }

    var isOpenAPS: Boolean
        get() = SessionStateHolder.isOpenAPS
        set(value) { SessionStateHolder.setIsOpenAPS(value) }

    var bpModel: Boolean
        get() = SessionStateHolder.bpModel
        set(value) { SessionStateHolder.setBpModel(value) }

    var chromeEnable: Boolean
        get() = SessionStateHolder.chromeEnable
        set(value) { SessionStateHolder.setChromeEnable(value) }

    var mcuModel: Boolean
        get() = SessionStateHolder.mcuModel
        set(value) { SessionStateHolder.setMcuModel(value) }

    var smModel: Boolean
        get() = SessionStateHolder.smModel
        set(value) { SessionStateHolder.setSmModel(value) }

    var commandDispatcherModel: Boolean
        get() = SessionStateHolder.commandDispatcherModel
        set(value) { SessionStateHolder.setCommandDispatcherModel(value) }
}