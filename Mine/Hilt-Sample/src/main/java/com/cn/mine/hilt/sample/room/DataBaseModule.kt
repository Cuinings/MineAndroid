package com.cn.mine.hilt.sample.room

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 13:19
 * @Description:
 */
@Module
@InstallIn(SingletonComponent::class)
object DataBaseModule {

    @Singleton
    @Provides
    fun providerAppDataBase(@ApplicationContext context: Context): AppDataBase {
        return Room.databaseBuilder(context, AppDataBase::class.java, "hilt_sample.db").build()
    }

    @Singleton
    @Provides
    fun providerUserDao(appDataBase: AppDataBase): UserDao {
        return  appDataBase.userDao()
    }

}