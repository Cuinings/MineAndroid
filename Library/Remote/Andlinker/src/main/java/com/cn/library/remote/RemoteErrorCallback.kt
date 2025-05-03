package com.cn.library.remote

import android.util.Log
import com.cn.library.remote.andlinker.AndLinkerErrorHandler

/**
 * @Author: CuiNing
 * @Time: 2024/10/23 16:58
 * @Description:
 */
class RemoteErrorCallback: AndLinkerErrorHandler.OnAndLinkerErrorListener {
    override fun onAndLinkerError(tag: String?, errorMsg: String?) {
        Log.e(RemoteErrorCallback::class.simpleName, "onAndLinkerError: $tag, $errorMsg")
    }
}