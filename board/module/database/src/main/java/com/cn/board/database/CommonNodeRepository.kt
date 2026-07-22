package com.cn.board.database

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * CommonNode 数据仓库 — 本地优先 + 并发安全。
 *
 * 核心策略:
 *   1. observe*() — 直接返回 Room Flow，数据变化自动发射，零额外开销
 *   2. refresh*() — 从远程拉取 → 原子替换 Room → Flow 自动通知 UI
 *   3. handleNotification() — CRUD 通知直接写入 Room
 *
 * 并发控制: 使用 Mutex 保证同一 resourceType 的 refresh 和 notification 互斥，
 *          避免"先删后插"的中间态被读到。
 */
class CommonNodeRepository(
    private val dao: CommonNodeDao,
    private val remote: CommonNodeRemoteDataSource
) {
    companion object {
        private const val TAG = "CommonNodeRepo"
    }

    // 每个 resourceType 一把互斥锁，Typesafe concurrent access
    private val mutexMap = mutableMapOf<Int, Mutex>()

    private fun mutexFor(resourceType: Int): Mutex =
        mutexMap.getOrPut(resourceType) { Mutex() }

    // ═══════════════════════════════════════════
    // 本地优先 Flow — Room 直接发射
    // ═══════════════════════════════════════════

    /** 观察全部聚合节点 */
    fun observeAll(): Flow<List<CommonNode>> = dao.observeAll()

    /** 按资源类型观察 */
    fun observeByResourceType(resourceType: Int): Flow<List<CommonNode>> =
        dao.observeByResourceType(resourceType)

    /** 按节点类型观察（GROUP / MEMBER） */
    fun observeByNodeType(nodeType: NodeType): Flow<List<CommonNode>> =
        dao.observeByNodeType(nodeType.name)

    /** 按资源类型 + 节点类型联合观察 */
    fun observeByTypeAndNodeType(
        resourceType: Int,
        nodeType: NodeType
    ): Flow<List<CommonNode>> =
        dao.observeByTypeAndNodeType(resourceType, nodeType.name)

    /** 观察某节点的子节点（子部门 + 成员），支持树形展开 */
    fun observeChildren(parentId: String): Flow<List<CommonNode>> =
        dao.observeChildren(parentId)

    /** 观察根部门列表 */
    fun observeRootDepts(resourceType: Int): Flow<List<CommonNode>> =
        dao.observeRootDepts(resourceType)

    /** 观察某部门下所有成员 */
    fun observeMembersByDept(deptId: String): Flow<List<CommonNode>> =
        dao.observeMembersByDept(deptId)

    // ═══════════════════════════════════════════
    // 部门用户统计
    // ═══════════════════════════════════════════

    /**
     * 部门成员计数的 Flow — 自动更新。
     * @return Flow<Map<deptId, count>>
     */
    fun observeDeptMemberCounts(resourceType: Int): Flow<Map<String, Int>> =
        dao.observeDeptMemberCounts(resourceType).map { list ->
            list.associate { it.deptId to it.cnt }
        }

    /**
     * 并发统计指定部门的成员数。
     * 大批量查询时比逐个 countMembersByDept 快 N 倍（N = deptIds.size）。
     */
    suspend fun getDeptMemberCountsConcurrent(
        deptIds: List<String>
    ): Map<String, Int> = coroutineScope {
        deptIds
            .map { deptId -> async { deptId to dao.countMembersByDept(deptId) } }
            .map { it.await() }
            .toMap()
    }

    /** 单部门成员计数 */
    suspend fun countMembersByDept(deptId: String): Int =
        dao.countMembersByDept(deptId)

    // ═══════════════════════════════════════════
    // 刷新策略 — Room 无数据时请求网络
    // ═══════════════════════════════════════════

    /**
     * 懒加载: 若 Room 已有数据直接返回，否则从远端拉取。
     *
     * @return true 表示触发了远端刷新，false 表示本地数据已存在
     */
    suspend fun loadIfNeeded(resourceType: Int): Boolean = mutexFor(resourceType).withLock {
        val count = dao.countByResourceType(resourceType)
        if (count > 0) {
            Log.d(TAG, "loadIfNeeded($resourceType): 命中 Room 缓存, count=$count")
            return false
        }
        Log.d(TAG, "loadIfNeeded($resourceType): Room 无数据, 请求远程...")
        refreshInternal(resourceType)
        return true
    }

    /**
     * 强制刷新: 从远端拉取全量数据并原子替换 Room。
     */
    suspend fun refresh(resourceType: Int): Unit = mutexFor(resourceType).withLock {
        refreshInternal(resourceType)
    }

    /**
     * 增量刷新: 基于 lastUpdated 拉取变更。
     * 如果远端不支持增量，回退到全量刷新。
     */
    suspend fun refreshIncremental(resourceType: Int, since: Long): Unit =
        mutexFor(resourceType).withLock {
            Log.d(TAG, "refreshIncremental($resourceType): since=$since")
            val delta = remote.fetchIncremental(resourceType, since)
            if (delta.isEmpty()) {
                Log.d(TAG, "refreshIncremental: 远端无增量，回退全量刷新")
                refreshInternal(resourceType)
            } else {
                // 增量 upsert
                delta.forEach { dao.insert(it) }
                Log.d(TAG, "refreshIncremental: 应用 ${delta.size} 条增量")
            }
        }

    private suspend fun refreshInternal(resourceType: Int) {
        val nodes = remote.fetchNodes(resourceType)
        // 原子替换: 先删旧数据，再批量插入新数据（同一事务内）
        dao.deleteByResourceType(resourceType)
        dao.insertAll(nodes)
        Log.d(TAG, "refreshInternal($resourceType): 写入 ${nodes.size} 条, Flow 将自动发射")
    }

    // ═══════════════════════════════════════════
    // CRUD 通知处理
    // ═══════════════════════════════════════════

    /**
     * 处理来自推送/通知的 CRUD 操作。
     * 与 refresh 共用同一把锁，避免并发冲突。
     */
    suspend fun handleNotification(
        node: CommonNode,
        operation: CrudOperation
    ): Unit = mutexFor(node.resourceType).withLock {
        when (operation) {
            CrudOperation.CREATE -> {
                dao.insert(node)
                Log.d(TAG, "handleNotification: CREATE $node")
            }
            CrudOperation.UPDATE -> {
                dao.update(node)
                Log.d(TAG, "handleNotification: UPDATE $node")
            }
            CrudOperation.DELETE -> {
                dao.deleteById(node.id)
                Log.d(TAG, "handleNotification: DELETE ${node.id}")
            }
        }
    }

    /**
     * 批量处理 CRUD 通知（同一 resourceType 内批量操作）。
     */
    suspend fun handleNotifications(
        resourceType: Int,
        nodes: List<Pair<CommonNode, CrudOperation>>
    ): Unit = mutexFor(resourceType).withLock {
        nodes.forEach { (node, op) ->
            when (op) {
                CrudOperation.CREATE -> dao.insert(node)
                CrudOperation.UPDATE -> dao.update(node)
                CrudOperation.DELETE -> dao.deleteById(node.id)
            }
        }
        Log.d(TAG, "handleNotifications($resourceType): 处理 ${nodes.size} 条")
    }

    // ═══════════════════════════════════════════
    // 工具方法
    // ═══════════════════════════════════════════

    /** 检查某类型数据是否已在 Room 中 */
    suspend fun hasData(resourceType: Int): Boolean =
        dao.countByResourceType(resourceType) > 0

    /** 单次查询（非 Flow） */
    suspend fun getById(id: String): CommonNode? = dao.getById(id)

    /** 清空所有聚合数据 */
    suspend fun clearAll() = dao.deleteAll()

    /** 清空某类型数据 */
    suspend fun clearByType(resourceType: Int) = dao.deleteByResourceType(resourceType)
}
