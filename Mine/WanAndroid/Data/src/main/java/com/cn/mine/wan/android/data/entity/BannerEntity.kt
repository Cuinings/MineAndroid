package com.cn.mine.wan.android.data.entity

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 17:05
 * @Description:
 */
data class BannerEntity(
    val desc: String,
    val id: Int,
    val imagePath: String,
    val isVisible: Int,
    val order: Int,
    val title: String,
    val type: Int,
    val url: String
)