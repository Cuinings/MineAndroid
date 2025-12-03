package com.cn.sample.test.child.app

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * @Author: CuiNing
 * @Time: 2025/12/2 11:20
 * @Description:
 */
data class AppEntity(
    @StringRes var nameRes: Int? = null,
    @DrawableRes var iconRes: Int? = null,
    var type: AppType? = null,
    var name: String? = null,
    var icon: Bitmap? = null
)

enum class AppType {
    CUSTOM,
    INSTALL
}