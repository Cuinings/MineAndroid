package com.cn.mine.wan.android.net

import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.BannerEntity
import com.cn.mine.wan.android.data.entity.CommonData
import com.cn.mine.wan.android.data.entity.CommonPageData

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 16:03
 * @Description:
 */
object Config {
    const val BASE_URL = "https://www.wanandroid.com"

    fun test() {
        //文章数据参考 页码，拼接在连接中，从0开始。
        //GET https://www.wanandroid.com/article/list/0/json
//        CommonData<CommonPageData<ArticleEntity>>()
        //Banner数据参考
        //GET https://www.wanandroid.com/banner/json
//        CommonData<BannerEntity>()

    }
}