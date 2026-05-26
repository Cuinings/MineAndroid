package com.cn.app.test

/**
 * @author: cn
 * @time: 2026/5/14 9:30
 * @history
 * @description:
 */
data class User(
    val id: Long,
    val name: String,
    val avatar: String,
    val likes: Int,
    val isFollowed: Boolean
)
