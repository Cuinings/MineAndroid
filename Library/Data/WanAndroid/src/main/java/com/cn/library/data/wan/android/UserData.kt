package com.cn.library.data.wan.android

import com.cn.library.data.wan.android.UserData.userEntity
import com.cn.library.data.wan.android.entity.UserEntity

/**
 * @Author: CuiNing
 * @Time: 2025/4/8 18:34
 * @Description:
 */
object UserData {
    var userEntity: UserEntity? = null
}

/**
 * 判断是否登录
 */
fun isLogin(loginAction: () -> Unit, notLoginAction: () -> Unit) {
    if (userEntity != null && (userEntity?.id ?: 0L) != 0L) loginAction.invoke() else notLoginAction.invoke()
}