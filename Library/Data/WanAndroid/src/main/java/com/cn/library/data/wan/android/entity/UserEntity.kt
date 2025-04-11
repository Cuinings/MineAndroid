package com.cn.library.data.wan.android.entity

/**
 * @Author: CuiNing
 * @Time: 2025/4/8 18:21
 * @Description: 用户信息
 */
data class UserEntity(
    val admin: Boolean,
    val chapterTops: List<Any>,
    val coinCount: Int,
    val collectIds: List<Any>,
    val email: String,
    val icon: String,
    val id: Long,
    val nickname: String,
    val password: String,
    val publicName: String,
    val token: String,
    val type: Int,
    val username: String
)