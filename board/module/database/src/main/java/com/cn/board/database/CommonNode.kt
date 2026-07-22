package com.cn.board.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ──────────────────────────────────────────────
// 枚举1: 节点类型 — 分组(部门) 还是 成员(用户)
// ──────────────────────────────────────────────
enum class NodeType {
    /** 分组/部门节点 */
    GROUP,
    /** 成员/用户节点 */
    MEMBER
}

// ──────────────────────────────────────────────
// 枚举2: 资源类型 — 区分来自哪个数据源 (类型1、2、3...)
// ──────────────────────────────────────────────
enum class ResourceType(val code: Int, val label: String) {
    TYPE_1(1, "类型1"),
    TYPE_2(2, "类型2"),
    TYPE_3(3, "类型3");
    companion object {
        private val map = entries.associateBy { it.code }
        fun fromCode(code: Int): ResourceType =
            map[code] ?: throw IllegalArgumentException("Unknown ResourceType code: $code")
    }
}

// ──────────────────────────────────────────────
// CRUD 通知操作类型
// ──────────────────────────────────────────────
enum class CrudOperation { CREATE, UPDATE, DELETE }

// ──────────────────────────────────────────────
// CommonNode — 聚合实体
// ──────────────────────────────────────────────
/**
 * 通用聚合节点。
 *
 * 将多个数据源（类型1、类型2、类型3...）的部门信息和用户信息
 * 统一聚合成一个节点，支持树形层级和部门用户统计。
 *
 * 主键格式:
 *   GROUP:  "{resourceTypeCode}_DEPT_{deptId}"
 *   MEMBER: "{resourceTypeCode}_USER_{userId}"
 */
@Entity(
    tableName = "common_node",
    indices = [
        Index(value = ["nodeType"]),
        Index(value = ["resourceType"]),
        Index(value = ["parentId"]),
        Index(value = ["deptId"]),
        Index(value = ["userId"]),
        // 复合索引: 按类型+节点类型联合查询
        Index(value = ["resourceType", "nodeType"]),
        // 复合索引: 按类型+部门查成员
        Index(value = ["resourceType", "deptId", "nodeType"])
    ]
)
data class CommonNode(

    /** 复合主键: "{resourceType}_{nodeType}_{deptId/userId}" */
    @PrimaryKey
    val id: String,

    /** 节点类型: 分组(GROUP) 还是 成员(MEMBER) */
    val nodeType: NodeType,

    /** 资源类型: 1,2,3... */
    val resourceType: Int,

    /**
     * 父节点ID。
     * - GROUP 节点: 上级部门的 id（根部门为 null）
     * - MEMBER 节点: 所属部门的 id（= deptId 对应的 GROUP id）
     */
    val parentId: String?,

    /** 部门ID — MEMBER 表示归属哪个部门，GROUP 表示自身部门ID */
    val deptId: String?,

    /** 显示名称（部门名 或 用户名） */
    val name: String,

    /** 用户ID — 仅 MEMBER 节点非空 */
    val userId: String?,

    /** 最后更新时间戳，用于增量同步 */
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        /** 构造 GROUP 节点 ID */
        fun groupId(resourceType: Int, deptId: String): String =
            "${resourceType}_DEPT_$deptId"

        /** 构造 MEMBER 节点 ID */
        fun memberId(resourceType: Int, userId: String): String =
            "${resourceType}_USER_$userId"
    }
}

// ──────────────────────────────────────────────
// 部门成员计数结果
// ──────────────────────────────────────────────
data class DeptMemberCount(
    val deptId: String,
    val cnt: Int
)
