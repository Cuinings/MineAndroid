package com.cn.mine.wan.android

import com.cn.mine.wan.android.Config.BASE_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RequestModule {

    @Singleton
    @Provides
    fun providerWanAndroidAPI(retrofit: Retrofit): WanAndroidAPI = retrofit.create(WanAndroidAPI::class.java)

    @Singleton
    @Provides
    fun okHttpClientProvider(): OkHttpClient = OkHttpClient.Builder().build()

    @Singleton
    @Provides
    fun retrofitProvider(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

}