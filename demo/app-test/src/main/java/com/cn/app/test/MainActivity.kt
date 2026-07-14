package com.cn.app.test

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cn.app.test.strategy.StrategyA_ShowHideActivity
import com.cn.app.test.strategy.StrategyB_AttachDetachActivity
import com.cn.app.test.strategy.StrategyC_ReplaceActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<android.widget.Button>(R.id.btn_strategy_a).setOnClickListener {
            startActivity(Intent(this, StrategyA_ShowHideActivity::class.java))
        }
        findViewById<android.widget.Button>(R.id.btn_strategy_b).setOnClickListener {
            startActivity(Intent(this, StrategyB_AttachDetachActivity::class.java))
        }
        findViewById<android.widget.Button>(R.id.btn_strategy_c).setOnClickListener {
            startActivity(Intent(this, StrategyC_ReplaceActivity::class.java))
        }
    }
}
