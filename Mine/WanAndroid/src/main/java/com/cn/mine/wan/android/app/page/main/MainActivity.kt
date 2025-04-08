package com.cn.mine.wan.android.app.page.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.cn.library.common.activity.BasicVBActivity
import com.cn.mine.wan.android.data.entity.result
import com.cn.mine.wan.android.databinding.ActivityMainBinding
import com.cn.mine.wan.android.databinding.ActivityMainBinding.*
import com.cn.mine.wan.android.net.WanAndroidAPI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BasicVBActivity<ActivityMainBinding>({ inflate(it) }) {

    private val viewModel by viewModels<MainActivityViewModel>()

    @Inject
    lateinit var wanAndroidAPI: WanAndroidAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.doSomething()
        Log.d(TAG, "onCreate: wanAndroidAPI -> $wanAndroidAPI")
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 100)
        binding.articleView.setOnRefreshListener { onRefresh() }
    }

    private fun onRefresh() {
        lifecycleScope.launch {
            wanAndroidAPI.article(0).result({
                Log.d(MainActivityViewModel::class.simpleName, "article: $it")
                binding.articleView.addArticle(it)
            }) {
                Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                binding.articleView.isRefreshing = false
            }
            Log.d(TAG, "onCreate: onRefresh")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "requestCode:$requestCode, permissions:$permissions, grantResults:${grantResults.size}, ${grantResults[0]}")
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            binding.articleView.isRefreshing = true
        }
    }

}