package com.cn.library.remote

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.cn.library.common.service.BasicService
import com.cn.library.remote.andlinker.AndLinkerBinder
import com.cn.library.remote.andlinker.AndLinkerErrorHandler

/**
 * @Author: CuiNing
 * @Time: 2024/10/11 14:23
 * @Description:
 */
@Suppress("UNCHECKED_CAST")
abstract class RemoteService<M: RemoteManager>(
    private val block: () -> Class<out RemoteManager>
): BasicService() {

    private val TAG by lazy { this.javaClass.simpleName }

    private var _linkerBinder: AndLinkerBinder? = null
    private var _remoteManager: M? = null

    protected val mLinkerBinder get() = requireNotNull(_linkerBinder)
    protected val remoteManager get() = requireNotNull(_remoteManager)

    override fun onCreate() {
        super.onCreate()
        AndLinkerErrorHandler.setOnAndLinkerErrorListener(RemoteErrorCallback())
        _remoteManager = (Class.forName(block().name).getDeclaredConstructor(Context::class.java)?.newInstance(this@RemoteService) as M)?.apply {
            _linkerBinder = AndLinkerBinder.Factory.newBinder().apply {
                registerAnyList().forEach {
                    this.registerObject(it)
                    Log.d(TAG, "registerObject: ${it.javaClass.simpleName}")
                }
            }
            Log.d(TAG, "onCreate: ${this.javaClass.simpleName}")
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return _linkerBinder
    }

    override fun onDestroy() {
        _remoteManager?.registerAnyList()?.forEach { _linkerBinder?.unRegisterObject(it) }
        _linkerBinder = null
        _remoteManager = null
        AndLinkerErrorHandler.setOnAndLinkerErrorListener(null)
        super.onDestroy()
    }
}