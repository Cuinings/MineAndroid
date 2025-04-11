package com.cn.library.data.wan.android.entity

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 16:30
 * @Description:
 */
data class CommonPageData<T>(
    var curPage: Int,
    var datas: MutableList<T>,
    var offset: Int = 0,
    var over: Boolean = false,
    var pageCount: Int = 0,
    var size: Int = 0,
    var total: Int = 0,
)
