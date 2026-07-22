package com.cn.board.proxy.test

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil

/**
 * [ParticipantBean] 的 RecyclerView DiffUtil 回调
 *
 * 用法：
 * ```
 * val adapter = BaseBinderAdapter().apply {
 *     addItemBinder(...)
 *     setDiffCallback(ParticipantBeanDiff)
 * }
 *
 * // 每次聚合变更后
 * adapter.submitList(manager.snapshot())
 * ```
 *
 * Payload key 常量定义在 companion，Binder 中按 key 局部刷新。
 *
 * @author: cn
 * @time: 2026/6/12 15:46
 */
class ParticipantBeanDiff : DiffUtil.ItemCallback<ParticipantBean>() {

    // ── 身份判定 ───────────────────────────────────────────────

    /** 按 [Id] 判定是否为同一项 */
    override fun areItemsTheSame(oldItem: ParticipantBean, newItem: ParticipantBean): Boolean {
        return oldItem.id == newItem.id
    }

    // ── 内容判定 ───────────────────────────────────────────────

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: ParticipantBean, newItem: ParticipantBean): Boolean {
        return oldItem.name == newItem.name
                && oldItem.online == newItem.online
                && oldItem.mute == newItem.mute
                && oldItem.quiet == newItem.quiet
                && oldItem.terminalAlias == newItem.terminalAlias
                && oldItem.aliasList == newItem.aliasList
                && oldItem.chairman == newItem.chairman
                && oldItem.vip == newItem.vip
                && oldItem.speaker == newItem.speaker
    }

    // ── 局部刷新 Payload ───────────────────────────────────────

    override fun getChangePayload(oldItem: ParticipantBean, newItem: ParticipantBean): Any? {
        return Bundle().apply {
            if (oldItem.name != newItem.name) putString(KEY_NAME, newItem.name)
            if (oldItem.online != newItem.online) putBoolean(KEY_ONLINE, newItem.online)
            if (oldItem.mute != newItem.mute) putBoolean(KEY_MUTE, newItem.mute)
            if (oldItem.quiet != newItem.quiet) putBoolean(KEY_QUIET, newItem.quiet)
            if (oldItem.terminalAlias != newItem.terminalAlias) {
                putString(KEY_TERMINAL_ALIAS, newItem.terminalAlias)
            }
            if (oldItem.chairman != newItem.chairman) putBoolean(KEY_CHAIRMAN, newItem.chairman)
            if (oldItem.vip != newItem.vip) putBoolean(KEY_VIP, newItem.vip)
            if (oldItem.speaker != newItem.speaker) putBoolean(KEY_SPEAKER, newItem.speaker)
        }.takeIf { !it.isEmpty }
    }

    companion object {
        const val KEY_NAME = "name"
        const val KEY_ONLINE = "online"
        const val KEY_MUTE = "mute"
        const val KEY_QUIET = "quiet"
        const val KEY_TERMINAL_ALIAS = "terminalAlias"
        const val KEY_CHAIRMAN = "chairman"
        const val KEY_VIP = "vip"
        const val KEY_SPEAKER = "speaker"

        /** 单例，可直接传入 setDiffCallback */
        val INSTANCE = ParticipantBeanDiff()
    }
}
