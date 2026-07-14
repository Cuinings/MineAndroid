package com.cn.board.meet.home.app

import android.graphics.Bitmap
import com.cn.board.database.AppInfo

data class SoftEntity(
    val appInfo: AppInfo? = null,
    var isSelect: Boolean = false,
    var deleting: Boolean = false,
    var isEdit: Boolean = false,
    var span: Int = 1,
    var bitmap: Bitmap? = null,
    var isShowSelect: Boolean = false,
)