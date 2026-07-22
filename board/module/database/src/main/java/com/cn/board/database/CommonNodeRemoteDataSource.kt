package com.cn.board.database

/**
 * 远程数据源接口 — 各类型数据的网络请求入口。
 *
 * 每个资源类型可独立实现此接口，由 Repository 统一调度。
 * 当 Room 无数据或触发刷新时，调用对应类型的 fetch 方法。
 */
interface CommonNodeRemoteDataSource {

    /**
     * 根据资源类型拉取全量节点列表（部门 + 用户）。
     *
     * @param resourceType 资源类型 code (1, 2, 3...)
     * @return 该类型下的所有 CommonNode（GROUP 和 MEMBER）
     */
    suspend fun fetchNodes(resourceType: Int): List<CommonNode>

    /**
     * 拉取增量变更（基于 lastUpdated 时间戳）。
     * 可选实现，默认返回空列表即回退到全量刷新。
     */
    suspend fun fetchIncremental(
        resourceType: Int,
        since: Long
    ): List<CommonNode> = emptyList()
}
