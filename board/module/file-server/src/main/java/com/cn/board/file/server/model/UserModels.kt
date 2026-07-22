package com.cn.board.file.server.model

import com.google.gson.annotations.SerializedName

/**
 * 管理员操作用户请求体
 */
data class AdminUserRequest(
    val username: String,
    val password: String,
    val nickname: String? = null,
    val role: String? = null,
    val status: String? = null
)

/**
 * 用户信息（管理员视角，含密码哈希）
 */
data class AdminUserInfo(
    val id: Int,
    val username: String,
    val nickname: String,
    val password: String,
    val role: String,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * 用户列表响应
 */
data class AdminUserListResponse(
    val users: List<AdminUserInfo>
)

/**
 * 待审批用户信息（精简）
 */
data class PendingUserInfo(
    val id: Int,
    val username: String
)

/**
 * 待审批用户响应
 */
data class PendingUsersResponse(
    val count: Int,
    val users: List<PendingUserInfo>
)

/**
 * 当前用户信息（GET /api/auth/me）
 *
 * 对齐 MinePython：`{ ok, username, nickname, role, status, permissions }`
 */
data class MeResponse(
    val ok: Boolean,
    val username: String,
    val nickname: String? = null,
    val role: String? = null,
    val status: String? = null,
    val permissions: List<String> = emptyList()
)

/**
 * 管理员批量操作用户请求体
 *
 * 对齐 MinePython `AdminBatchRequest`：`{ ids: [int], action }`
 * action ∈ "approve" | "reject" | "delete"
 */
data class AdminBatchRequest(
    val ids: List<Int>,
    val action: String
)

/**
 * 批量操作中单个失败项
 */
data class AdminBatchFailedItem(
    val id: Int,
    val error: String
)

/**
 * 管理员批量操作响应
 *
 * 对齐 MinePython `POST /api/admin/users/batch`：
 * `{ ok, action, processed: [int], failed: [{ id, error }] }`
 */
data class AdminBatchResponse(
    val ok: Boolean,
    val action: String? = null,
    val processed: List<Int> = emptyList(),
    val failed: List<AdminBatchFailedItem> = emptyList()
)

/**
 * 审计日志单条
 *
 * 对齐 MinePython `AuditItem`：{ id, username, action, target, ip, created_at }
 */
data class AuditItem(
    val id: Int,
    val username: String,
    val action: String,
    val target: String,
    val ip: String,
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * 审计日志列表响应
 */
data class AuditListResponse(
    val logs: List<AuditItem>
)
