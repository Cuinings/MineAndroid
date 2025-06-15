package com.cn.mine.wan.android.app.module

import com.cn.mine.wan.android.events.article.ArticleEvent
import com.cn.mine.wan.android.events.banner.BannerEvent
import com.cn.mine.wan.android.events.entrance.EntranceEvent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @Author: CuiNing
 * @Time: 2025/6/8 15:28
 * @Description:
 */
@Module
@InstallIn(SingletonComponent::class)
object EventModule {

    @Singleton
    @Provides
    fun providerEntranceEvent(): EntranceEvent = EntranceEvent()

    @Singleton
    @Provides
    fun providerBannerEvent(): BannerEvent = BannerEvent()

    @Singleton
    @Provides
    fun providerArticleEvent(): ArticleEvent = ArticleEvent()
}