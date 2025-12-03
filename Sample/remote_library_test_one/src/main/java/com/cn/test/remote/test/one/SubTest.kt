package com.cn.test.remote.test.one

import android.util.Log
import com.cn.library.remote.msg.subscriber.annotation.Subscriber

/**
 * @Author: CuiNing
 * @Time: 2025/11/25 11:29
 * @Description:
 */
class SubTest {

    init {
        SubTestSub.registerSubscriber(this)
    }

    @Subscriber("XXXX")
    fun xxx(value: NameBean){
        Log.d(MainActivity::class.simpleName, "xxx: $value")
    }
}