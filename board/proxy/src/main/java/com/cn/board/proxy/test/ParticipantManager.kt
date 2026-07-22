package com.cn.board.proxy.test

import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * 参会成员聚合管理器（线程安全）
 *
 * 将三路通知（MTEntityStatus / MtcConfTerminalInfo / MTEntityInfo）
 * 按 [Id] 聚合为 [ParticipantBean]，并提供 CRUD 操作。
 *
 * **线程模型**：
 * - 通知回调线程 → onXxx() 写入（写锁）
 * - UI 线程 → [snapshot] / [get] 读取（读锁）
 * - [snapshot] 返回不可变快照，可直接交给 RecyclerView DiffUtil
 *
 * 用法：
 * ```
 * val mgr = ParticipantManager()
 *
 * // 通知线程写入
 * mgr.onStatusChanged(MTEntityStatus(uId = 1, tId = 100, online = true, mute = false, quiet = false))
 *
 * // UI 线程读取
 * val list = mgr.snapshot()
 * adapter.submitList(list)
 * ```
 *
 * @author: cn
 * @time: 2026/6/12 15:40
 */
class ParticipantManager {

    /** 读写锁：写操作独占，读操作共享 */
    private val lock = ReentrantReadWriteLock()

    /** 核心存储：Id → ParticipantBean（写锁保护） */
    private val participants = linkedMapOf<Id, ParticipantBean>()

    // ── 订阅操作符 ─────────────────────────────────────────────

    /** 通过下标获取成员（读锁） */
    operator fun get(id: Id): ParticipantBean? {
        lock.readLock().lock()
        try {
            return participants[id]
        } finally {
            lock.readLock().unlock()
        }
    }

    // ── 通知入口 ───────────────────────────────────────────────

    /**
     * 处理 [MTEntityStatus] 通知：更新 online / mute / quiet（写锁）
     */
    fun onStatusChanged(status: MTEntityStatus) {
        lock.writeLock().lock()
        try {
            val id = Id(uId = status.uId, tId = status.tId)
            val bean = participants.getOrPut(id) { ParticipantBean(id = id) }
            bean.online = status.online
            bean.mute = status.mute
            bean.quiet = status.quiet
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * 处理 [MtcConfTerminalInfo] 通知：更新 terminalAlias / online（写锁）
     *
     * 注意：[MtcConfTerminalInfo.tId] 是 String，此处转换为 Int 构造 [Id]。
     * 若 tId 不是纯数字，跳过此条（不写入）。
     */
    fun onTerminalInfo(info: MtcConfTerminalInfo) {
        lock.writeLock().lock()
        try {
            val numericTId = info.tId.toIntOrNull() ?: return
            val id = Id(uId = info.uId, tId = numericTId)
            val bean = participants.getOrPut(id) { ParticipantBean(id = id) }
            bean.terminalAlias = info.achAlias
            bean.online = info.online
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * 处理 [MTEntityInfo] 通知：更新 name / aliasList（写锁）
     */
    fun onEntityInfo(info: MTEntityInfo) {
        lock.writeLock().lock()
        try {
            val id = Id(uId = info.uId, tId = info.tId)
            val bean = participants.getOrPut(id) { ParticipantBean(id = id) }
            bean.aliasList = info.tMtAlias.arrAlias
            // 显示名称：取别名列表中的首项
            bean.name = info.tMtAlias.arrAlias.firstOrNull()?.achAlias ?: bean.name
        } finally {
            lock.writeLock().unlock()
        }
    }

    // ── 角色更新 ───────────────────────────────────────────────

    /**
     * 批量更新成员角色标记（写锁）
     *
     * 通常随 MTEntityInfo / MTEntityStatus 中的角色标志下发后调用。
     */
    fun onRoleBatch(roles: List<ParticipantRole>) {
        lock.writeLock().lock()
        try {
            roles.forEach { role ->
                val id = Id(uId = role.uId, tId = role.tId)
                val bean = participants.getOrPut(id) { ParticipantBean(id = id) }
                bean.chairman = role.chairman
                bean.vip = role.vip
                bean.speaker = role.speaker
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * 更新单个成员角色标记（写锁）
     */
    fun onRoleChanged(role: ParticipantRole) {
        lock.writeLock().lock()
        try {
            val id = Id(uId = role.uId, tId = role.tId)
            val bean = participants.getOrPut(id) { ParticipantBean(id = id) }
            bean.chairman = role.chairman
            bean.vip = role.vip
            bean.speaker = role.speaker
        } finally {
            lock.writeLock().unlock()
        }
    }

    // ── 批量通知（一次写锁）────────────────────────────────────

    /**
     * 批量处理状态变更（单次写锁）
     */
    fun onStatusBatch(statusList: List<MTEntityStatus>) {
        lock.writeLock().lock()
        try {
            statusList.forEach { status ->
                val id = Id(uId = status.uId, tId = status.tId)
                val bean = participants.getOrPut(id) { ParticipantBean(id = id) }
                bean.online = status.online
                bean.mute = status.mute
                bean.quiet = status.quiet
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * 批量处理成员信息（单次写锁）
     */
    fun onEntityInfoBatch(infoList: List<MTEntityInfo>) {
        lock.writeLock().lock()
        try {
            infoList.forEach { info ->
                val id = Id(uId = info.uId, tId = info.tId)
                val bean = participants.getOrPut(id) { ParticipantBean(id = id) }
                bean.aliasList = info.tMtAlias.arrAlias
                bean.name = info.tMtAlias.arrAlias.firstOrNull()?.achAlias ?: bean.name
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * 清空并从全新的终端列表重建（单次写锁）
     *
     * 典型场景：入会后收到全量终端列表
     */
    fun rebuildFromTerminalList(list: List<MtcConfTerminalInfo>) {
        lock.writeLock().lock()
        try {
            participants.clear()
            list.forEach { info ->
                val numericTId = info.tId.toIntOrNull() ?: return@forEach
                val id = Id(uId = info.uId, tId = numericTId)
                val bean = participants.getOrPut(id) { ParticipantBean(id = id) }
                bean.terminalAlias = info.achAlias
                bean.online = info.online
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    // ── 读取 ───────────────────────────────────────────────────

    /**
     * 返回不可变快照，可直接提交给 RecyclerView DiffUtil
     *
     * 调用时机：每次聚合通知后，UI 线程调用此方法获取最新列表
     */
    fun snapshot(): List<ParticipantBean> {
        lock.readLock().lock()
        try {
            return participants.values.toList()
        } finally {
            lock.readLock().unlock()
        }
    }

    /** 返回所有已聚合成员（读锁） */
    val all: List<ParticipantBean>
        get() = snapshot()

    /** 成员数量（读锁） */
    val size: Int
        get() {
            lock.readLock().lock()
            try {
                return participants.size
            } finally {
                lock.readLock().unlock()
            }
        }

    /** 是否有成员（读锁） */
    fun isEmpty(): Boolean {
        lock.readLock().lock()
        try {
            return participants.isEmpty()
        } finally {
            lock.readLock().unlock()
        }
    }

    /** 条件查询（读锁） */
    fun filter(predicate: (ParticipantBean) -> Boolean): List<ParticipantBean> {
        lock.readLock().lock()
        try {
            return participants.values.filter(predicate)
        } finally {
            lock.readLock().unlock()
        }
    }

    /** 按 uId 查找（读锁） */
    fun findByUId(uId: Int): List<ParticipantBean> {
        return filter { it.id.uId == uId }
    }

    /** 按 tId 查找（读锁） */
    fun findByTId(tId: Int): List<ParticipantBean> {
        return filter { it.id.tId == tId }
    }

    /**
     * 在线成员按角色优先级排序：chairman → vip → speaker → 其他
     *
     * 只返回 `online == true` 的成员，排序后可直接提交 RecyclerView。
     */
    fun onlineByRole(): List<ParticipantBean> {
        lock.readLock().lock()
        try {
            // chairman=3, vip=2, speaker=1, 其他=0
            return participants.values
                .filter { it.online }
                .sortedByDescending { bean ->
                    when {
                        bean.chairman -> 3
                        bean.vip -> 2
                        bean.speaker -> 1
                        else -> 0
                    }
                }
        } finally {
            lock.readLock().unlock()
        }
    }

    // ── 删除 ───────────────────────────────────────────────────

    /** 移除单个成员（写锁） */
    fun remove(id: Id): ParticipantBean? {
        lock.writeLock().lock()
        try {
            return participants.remove(id)
        } finally {
            lock.writeLock().unlock()
        }
    }

    /** 移除指定 uId 的所有成员（写锁） */
    fun removeByUId(uId: Int) {
        lock.writeLock().lock()
        try {
            participants.keys.removeAll { it.uId == uId }
        } finally {
            lock.writeLock().unlock()
        }
    }

    /** 移除指定 tId 的所有成员（写锁） */
    fun removeByTId(tId: Int) {
        lock.writeLock().lock()
        try {
            participants.keys.removeAll { it.tId == tId }
        } finally {
            lock.writeLock().unlock()
        }
    }

    /** 清空全部（写锁） */
    fun clear() {
        lock.writeLock().lock()
        try {
            participants.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }

    // ── 调试 ───────────────────────────────────────────────────

    override fun toString(): String =
        buildString {
            lock.readLock().lock()
            try {
                appendLine("ParticipantManager(size=$size)")
                participants.values.forEach { bean ->
                    appendLine("  ${bean.id} → name=${bean.name}, online=${bean.online}, mute=${bean.mute}")
                }
            } finally {
                lock.readLock().unlock()
            }
        }
}
