package com.cn.mine.hilt.sample.repository

import android.util.Log
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 10:42
 * @Description:
 */
@ViewModelScoped
class MainActivityRepository @Inject constructor() {

    fun doSomething() {
        Log.d(MainActivityRepository::class.simpleName, "doSomething: ")
    }
}