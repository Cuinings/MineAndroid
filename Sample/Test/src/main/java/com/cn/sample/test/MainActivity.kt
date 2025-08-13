package com.cn.sample.test

import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.cn.library.common.activity.BasicVBActivity
import com.cn.sample.test.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : BasicVBActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate: 111")
        Log.d("MainActivity", "onCreate: Locale:${Locale.getDefault()}")
        Locale.getDefault().let {
            Toast.makeText(this, "Locale:$it", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "onCreate: Locale:$it")
        }
        lifecycleScope.launch {
            var count = 0f
            var repeatCount = 0
            binding.micEnergyLayout.background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), resources.getColor(R.color.black, null).toDrawable())
                addState(intArrayOf(), resources.getColor(android.R.color.transparent, null).toDrawable())
            }
            binding.micEnergy.run {
                setActive(true)
                while(count < 1f) {
                    setActive(repeatCount % 2 == 0)
                    delay(10)
                    count += 0.01f
                    setEnergyLevel(count)
                    if (count >= 1f) {
                        count = 0f
                        repeatCount ++
                    }
                }
            }
        }
    }
}