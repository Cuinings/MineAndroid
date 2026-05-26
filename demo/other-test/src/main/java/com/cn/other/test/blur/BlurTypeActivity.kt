package com.cn.other.test.blur

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.cn.other.test.R

/**
 * @author: cn
 * @time: 2026/4/10 16:07
 * @history
 * @description:
 */
class BlurTypeActivity: AppCompatActivity() {

    @SuppressLint("UnsafeIntentLaunch")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blur_type);
        findViewById<Button>(R.id.btn_dialog_blur_background).setOnClickListener {
            //仅模糊背景
            BlurDialog(this@BlurTypeActivity, BlurDialog.BLUR_TYPE_BLUR_BACKGROUND).show()
        }
        findViewById<Button>(R.id.btn_dialog_blur_behind).setOnClickListener {
            BlurDialog(this@BlurTypeActivity, BlurDialog.BLUR_TYPE_BLUR_BEHIND).show()
        }
        findViewById<Button>(R.id.btn_dialog_blur_background_and_behind).setOnClickListener {
            BlurDialog(this@BlurTypeActivity, BlurDialog.BLUR_TYPE_BLUR_BACKGROUND_AND_BEHIND).show()
        }
        findViewById<Button>(R.id.btn_activity_blur_background).setOnClickListener {

            val intent = Intent(this@BlurTypeActivity, BlurActivity::class.java);
            intent.putExtra(BlurActivity.EXTRA_KEY_BLUR_TYPE, BlurActivity.BLUR_TYPE_BLUR_BACKGROUND)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btn_activity_blur_behind).setOnClickListener {
            //仅模糊后方屏幕
            val intent = Intent(this@BlurTypeActivity, BlurActivity::class.java)
            intent.putExtra(BlurActivity.EXTRA_KEY_BLUR_TYPE, BlurActivity.BLUR_TYPE_BLUR_BEHIND)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btn_activity_blur_background_and_behind).setOnClickListener {
            //同时模糊背景和后方屏幕
            val intent = Intent(this@BlurTypeActivity, BlurActivity::class.java)
            intent.putExtra(BlurActivity.EXTRA_KEY_BLUR_TYPE, BlurActivity.BLUR_TYPE_BLUR_BACKGROUND_AND_BEHIND)
            startActivity(intent)
        }
    }
}