package com.cn.test.remote.test.one

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cn.library.remote.msg.router.client.MsgRouter.dispatcherMsg
import com.cn.library.remote.msg.router.client.bean.MsgBody
import com.cn.library.remote.msg.subscriber.annotation.Subscriber

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivitySub.registerSubscriber(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        SubscriberCallback()
        SubTest()
    }

    override fun onDestroy() {
        MainActivitySub.unRegisterSubscriber()
        super.onDestroy()
    }

    fun dispatcherMsgTest(view: View) {
        dispatcherMsg(MsgBody().apply {
            this.code = "XXXX"
        })
    }

    @Subscriber("XXXX")
    fun xxx(){
        Log.d(MainActivity::class.simpleName, "xxx: ")
    }
}