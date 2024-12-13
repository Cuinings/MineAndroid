package com.cn.mine.wan.android.app.page.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.cn.library.common.activity.BasicVBActivity
import com.cn.mine.wan.android.databinding.ActivityMainBinding
import com.cn.mine.wan.android.net.WanAndroidAPI
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BasicVBActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }) {

    private val viewModel by viewModels<MainActivityViewModel>()

    @Inject
    lateinit var wanAndroidAPI: WanAndroidAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 100)
        viewModel.doSomething()
        Log.d(TAG, "onCreate: wanAndroidAPI -> $wanAndroidAPI")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "requestCode:$requestCode, permissions:$permissions, grantResults:${grantResults.size}, ${grantResults[0]}")
        if (requestCode == 100) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.article(0) {

                }
            }
        }
    }

}