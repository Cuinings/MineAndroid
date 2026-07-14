package com.cn.board.proxy.test

/**
 * 聚合后的参会成员 Bean
 *
 * 三方数据源按 [id] 聚合：
 * - [MTEntityInfo]  → name / aliasList
 * - [MtcConfTerminalInfo] → terminalAlias / online
 * - [MTEntityStatus] → online / mute / quiet
 *
 * @author: cn
 * @time: 2026/6/12 15:29
 */
data class ParticipantBean(
    val id: Id,

    /** 显示名称：优先取 MTEntityInfo 的首个别名 */
    var name: String = "",

    /** 在线状态 */
    var online: Boolean = false,

    /** 静音 */
    var mute: Boolean = false,

    /** 静默（被主持人静音） */
    var quiet: Boolean = false,

    /** 终端别名（来自 MtcConfTerminalInfo.achAlias） */
    var terminalAlias: String = "",

    /** 成员别名列表（来自 MTEntityInfo.tMtAlias） */
    var aliasList: List<MtAlias> = emptyList(),

    // ── 角色标记（用于排序）────────────────────────────────────

    /** 主持人 */
    var chairman: Boolean = false,

    /** VIP */
    var vip: Boolean = false,

    /** 发言者 */
    var speaker: Boolean = false
)
