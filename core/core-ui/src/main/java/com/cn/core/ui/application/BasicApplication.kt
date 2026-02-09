package com.cn.core.ui.application

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.cn.core.ui.application.ApplicationContextExt.context

/**
 * @Author: CuiNing
 * @Time: 2024/10/15 9:04
 * @Description:
 */
abstract class BasicApplication: Application(), ViewModelStoreOwner {

    companion object {
        lateinit var TAG: String
    }

    private val mAppViewModelStore: ViewModelStore = ViewModelStore()

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        base?.let { context = it }
    }

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()
        getProcessName()?.takeIf { it == packageName }?.apply { TAG = this }?.let {
            initApplication()
        }
    }

    abstract fun initApplication()

    val viewmodelProvider: ViewModelProvider get() = ViewModelProvider(this, appFactory)

    private val appFactory: ViewModelProvider.Factory get() = ViewModelProvider.AndroidViewModelFactory.getInstance(this)

    override val viewModelStore: ViewModelStore
        get() = mAppViewModelStore

}