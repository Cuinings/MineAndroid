package com.cn.mine.wan.android.repository.article

import com.cn.mine.wan.android.data.wan.android.ArticleEntity
import com.cn.mine.wan.android.data.wan.android.CommonEntity
import com.cn.mine.wan.android.data.wan.android.CommonPageEntity
import com.cn.mine.wan.android.repository.IRepository


/**
 * @Author: CuiNing
 * @Time: 2025/6/7 16:31
 * @Description:
 */
interface ArticleRepository: IRepository {

    suspend fun article(index: Int): Result<CommonEntity<CommonPageEntity<ArticleEntity>>>

}