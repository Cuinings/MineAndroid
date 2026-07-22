package com.cn.board.file.server.model

import com.google.gson.annotations.SerializedName

/**
 * 分类信息
 *
 * 对齐 MinePython `CategoryItem`：`{ category, count, total_size }`
 * （`total_size_human` 视后端配置可能返回，故设为可选）
 */
data class CategoryInfo(
    val category: String,
    val count: Int,
    @SerializedName("total_size")
    val totalSize: Long,
    @SerializedName("total_size_human")
    val totalSizeHuman: String? = null
)

/**
 * 分类列表响应
 *
 * 对齐 MinePython `CategoryListResponse`：`{ categories: [...] }`
 */
data class CategoryListResponse(
    val categories: List<CategoryInfo>
)

/**
 * 整理操作响应
 *
 * 对齐 MinePython `POST /api/organize`：`{ ok, message }`
 */
data class OrganizeResponse(
    val ok: Boolean,
    val message: String? = null
)
