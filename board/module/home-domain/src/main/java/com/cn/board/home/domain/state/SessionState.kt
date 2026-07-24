package com.cn.board.home.domain.state

/**
 * 运行时会话状态，替代全局可变 [com.cn.board.home.state.State] 对象。
 *
 * 所有状态通过 [SessionStateHolder] 以 StateFlow 形式对外暴露，
 * 确保单向数据流和线程安全。
 */
data class SessionState(
    val isLoadingApp: Boolean = false,
    val isSendAss: Boolean = false,
    val isInConf: Boolean = false,
    val vrsLoginState: Boolean = false,
    val isOpenAPS: Boolean = false,
    val bpModel: Boolean = false,
    val chromeEnable: Boolean = true,
    val mcuModel: Boolean = false,
    val smModel: Boolean = false,
    val commandDispatcherModel: Boolean = false,
)