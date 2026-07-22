package com.cn.board.file.server.model

import com.google.gson.annotations.SerializedName

/**
 * 认证请求 (login / register 共用)
 */
data class AuthRequest(
    val username: String,
    val password: String,
    val nickname: String? = null
)

/**
 * 认证响应
 *
 * 对齐 MinePython `AuthResponse`：
 * `{ ok, token, message, role, nickname, permissions, require_password_change }`
 */
data class AuthResponse(
    val ok: Boolean,
    val token: String? = null,
    val message: String = "",
    val role: String? = null,
    val nickname: String? = null,
    val permissions: List<String> = emptyList(),
    @SerializedName("require_password_change")
    val requirePasswordChange: Boolean = false
)

/**
 * 通用操作响应（delete、approve、logout 等）
 */
data class OpResponse(
    val ok: Boolean,
    val message: String = ""
)

/**
 * 修改自身密码请求体
 *
 * 对齐 MinePython `PasswordChangeRequest`：`{ old_password, new_password }`
 */
data class PasswordChangeRequest(
    val old_password: String,
    val new_password: String
)

/**
 * 注销自身账号请求体
 *
 * 对齐 MinePython `DeactivateRequest`：`{ password? }`
 */
data class DeactivateRequest(
    val password: String? = null
)
