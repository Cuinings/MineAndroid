package com.cn.library.common.application

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.cn.library.common.application.ApplicationContextExt.context

/**
 * @Author: CuiNing
 * @Time: 2024/10/15 9:04
 * @Description:
 */
abstract class BasicApplication: Application(), ViewModelStoreOwner {

    protected val TAG: String by lazy { this.javaClass.simpleName }

    private val mAppViewModelStore: ViewModelStore = ViewModelStore()

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        base?.let { context = it }
    }

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()
        getProcessName()?.takeIf { it == packageName }?.let {
            initApplication()
        }
    }

    abstract fun initApplication()

    val viewmodelProvider: ViewModelProvider get() = ViewModelProvider(this, appFactory)

    private val appFactory: ViewModelProvider.Factory get() = ViewModelProvider.AndroidViewModelFactory.getInstance(this)

    override val viewModelStore: ViewModelStore
        get() = mAppViewModelStore

}