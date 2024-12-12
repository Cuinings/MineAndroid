package com.cn.mine.hilt.sample.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 13:43
 * @Description:
 */
@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Long,
    var username: String = "",
    var useralias: String = "",
    var age: Int = 0
)
