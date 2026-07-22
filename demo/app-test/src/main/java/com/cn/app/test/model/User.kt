package com.cn.app.test.model

data class User(
    val id: Long,
    val name: String,
    val avatar: String,
    val likes: Int,
    val isFollowed: Boolean
)
