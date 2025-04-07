package com.cn.sample.hilt.room

import androidx.room.Dao
import androidx.room.Query

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 13:46
 * @Description:
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM user ORDER by id")
    suspend fun users(): List<User>

}