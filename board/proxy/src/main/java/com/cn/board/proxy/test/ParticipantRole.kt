package com.cn.board.proxy.test

/**
 * 成员角色标记，用于批量更新 [ParticipantBean] 的 chairman / vip / speaker。
 *
 * 对应会议 SDK 中角色通知数据。
 *
 * @author: cn
 * @time: 2026/6/12 17:13
 */
data class ParticipantRole(
    val uId: Int,
    val tId: Int,
    val chairman: Boolean = false,
    val vip: Boolean = false,
    val speaker: Boolean = false
)
