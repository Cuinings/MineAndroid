package com.cn.mine.wan.android.repository

import com.cn.mine.wan.android.repository.article.ArticleRepository
import com.cn.mine.wan.android.repository.article.impl.ArticleRepositoryImpl
import com.cn.mine.wan.android.repository.article.source.remote.ArticleDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @Author: CuiNing
 * @Time: 2025/6/14 18:07
 * @Description:
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun providerArticleRepository(articleDataSource: ArticleDataSource): ArticleRepository =
        ArticleRepositoryImpl(articleDataSource)
}