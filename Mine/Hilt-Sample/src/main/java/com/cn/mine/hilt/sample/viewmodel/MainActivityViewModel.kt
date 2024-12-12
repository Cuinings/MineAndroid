package com.cn.mine.hilt.sample.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cn.mine.hilt.sample.repository.MainActivityRepository
import com.cn.mine.hilt.sample.room.UserDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 10:40
 * @Description:
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val repository: MainActivityRepository
): ViewModel() {

    @Inject
    lateinit var userDao: UserDao

    fun doSomething() { viewModelScope.launch {
        Log.d(MainActivityViewModel::class.simpleName, "doSomething: ")
        repository.doSomething()
        userDao.users().let {
            Log.d(MainActivityViewModel::class.simpleName, "doSomething: users -> ${it.isEmpty()}")
        }
    } }

}