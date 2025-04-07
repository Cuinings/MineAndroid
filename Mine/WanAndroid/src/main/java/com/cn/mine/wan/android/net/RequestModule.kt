package com.cn.mine.wan.android.net

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
    fun providerOkhttpClientCookie(@ApplicationContext context: Context): WanAndroidCookie = WanAndroidCookie(context)

    @Singleton
    @Provides
    fun providerOkHttpClient(cookie: WanAndroidCookie): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ResponseInterceptor())
        .addInterceptor(HttpLoggingInterceptor())
        .cookieJar(cookie)
        .build()

    @Singleton
    @Provides
    fun providerRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(Config.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

}