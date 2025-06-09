package com.cn.mine.wan.android.repository.banner

import com.cn.mine.wan.android.data.entity.BannerEntity
import com.cn.mine.wan.android.data.entity.CommonEntity
import com.cn.mine.wan.android.repository.IRepository

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 16:31
 * @Description:
 */
interface BannerRepository: IRepository {

    suspend fun banner(): Result<CommonEntity<List<BannerEntity>>?>

}