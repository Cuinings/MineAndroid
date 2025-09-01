package com.cn.sample.test

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.cn.library.common.activity.BasicVBActivity
import com.cn.library.common.flow.collectByScope
import com.cn.sample.test.databinding.ActivityMainBinding
import com.cn.sample.test.wallpaper.receiver.WallpaperChangeReceiver
import com.cn.sample.test.wallpaper.viewmodel.WallpaperEvent
import com.cn.sample.test.wallpaper.viewmodel.WallpaperState
import com.cn.sample.test.wallpaper.viewmodel.WallpaperViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale


@AndroidEntryPoint
class MainActivity : BasicVBActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }) {

    private val viewModel by viewModels<MainActivityViewModel>()
    private val wallpaperViewModel by viewModels<WallpaperViewModel>()

    private val wallpaperChangeReceiver: WallpaperChangeReceiver = WallpaperChangeReceiver {
        wallpaperViewModel.sendUIIntent(WallpaperEvent.WallpaperUpdate(this@MainActivity))
    }

    @SuppressLint("MissingPermission")
    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate: 111")
        Log.d("MainActivity", "onCreate: Locale:${Locale.getDefault()}")
        Locale.getDefault().let {
            Toast.makeText(this, "Locale:$it", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "onCreate: Locale:$it")
        }
        registerReceiver(wallpaperChangeReceiver, IntentFilter(Intent.ACTION_WALLPAPER_CHANGED))
        viewModel.uiState.map { it.micPowerState }.distinctUntilChanged().collectByScope(lifecycleScope) {
            Log.d(TAG, "uiState: $it")
        }
        lifecycleScope.launch {
            binding.micEnergyLayout.background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), resources.getColor(R.color.light, null).toDrawable())
                addState(intArrayOf(), resources.getColor(android.R.color.transparent, null).toDrawable())
            }
            binding.micEnergyLayout1.background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), resources.getColor(R.color.light, null).toDrawable())
                addState(intArrayOf(), resources.getColor(android.R.color.transparent, null).toDrawable())
            }
            var count = 0f
            var repeatCount = 1
            while (true) {
                if (count > 1f) {
                    count = 0f
                    repeatCount ++
//                    Log.i(TAG, "micEnergy: $count, $repeatCount")
                }
                binding.micEnergy.run {
                    toggleActive(repeatCount % 2 == 0)
                    setEnergyLevel(count)
                }
                binding.micEnergyView.run {
                    toggleActive(repeatCount % 2 == 0)
                    setEnergyLevel(count)
                }
                binding.micEnergy1.run {
                    toggleActive(repeatCount % 2 == 0)
                    setEnergyLevel(count)
                }
                binding.micEnergyView1.run {
                    toggleActive(repeatCount % 2 == 0)
                    setEnergyLevel(count)
                }
                count += 0.01f
                delay(10)
            }
        }
        binding.favoriteView.run {
            setOnClickListener {
                it.isSelected = !it.isSelected
                setActive(it.isSelected)
            }
        }

        binding.circleProgressView.run {
            decodeBitmapFromResource(this@MainActivity, R.drawable.ic_launcher_foreground)?.let { setCenterIcon(it) }
        }
        lifecycleScope.launch {
            var progress = 0f
            while (true) {
                if (progress > 1f) progress = 0f
                binding.circleProgressView.setProgress(progress, false)
                progress += 0.01f
                delay(100)
            }
        }
        wallpaperViewModel.uiState.collectByScope(lifecycleScope) {
            when(it) {
                WallpaperState.Init -> wallpaperViewModel.sendUIIntent(WallpaperEvent.WallpaperUpdate(this@MainActivity))
                is WallpaperState.WallpaperColor -> binding.mainActivityView.refreshColor(it.color)
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(wallpaperChangeReceiver)
        super.onDestroy()
    }


    fun decodeBitmapFromResource(context: Context, drawableId: Int): Bitmap? {
        var bitmap: Bitmap? = null
        ContextCompat.getDrawable(context, drawableId)?.let {
            bitmap = createBitmap(it.intrinsicWidth, it.intrinsicHeight)
            val canvas = Canvas(bitmap)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)
        }
        return bitmap
    }


}