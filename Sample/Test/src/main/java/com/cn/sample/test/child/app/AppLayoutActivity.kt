package com.cn.sample.test.child.app

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.cn.sample.test.R

/**
 * @Author: CuiNing
 * @Time: 2025/12/2 13:16
 * @Description:
 */
class AppLayoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_layout)

        val appEntity0 = AppEntity(name = "0")
        val appEntity1 = AppEntity(name = "1")
        val appEntity2 = AppEntity(name = "2")
        val appEntity3 = AppEntity(name = "3")
        val appEntity4 = AppEntity(name = "4")
        val appEntity5 = AppEntity(name = "5")

        val appLayout: AppLayout? = findViewById<AppLayout>(R.id.app_layout)

        findViewById<Button>(R.id.btn_add0).setOnClickListener {
            appLayout?.addItem(appEntity0)
        }
        findViewById<Button>(R.id.btn_remove0).setOnClickListener {
            appLayout?.removeItem(appEntity0)
        }

        findViewById<Button>(R.id.btn_add1).setOnClickListener {
            appLayout?.addItem(appEntity1)
        }
        findViewById<Button>(R.id.btn_remove1).setOnClickListener {
            appLayout?.removeItem(appEntity1)
        }

        findViewById<Button>(R.id.btn_add2).setOnClickListener {
            appLayout?.addItem(appEntity2)
        }
        findViewById<Button>(R.id.btn_remove2).setOnClickListener {
            appLayout?.removeItem(appEntity2)
        }

        findViewById<Button>(R.id.btn_add3).setOnClickListener {
            appLayout?.addItem(appEntity3)
        }
        findViewById<Button>(R.id.btn_remove3).setOnClickListener {
            appLayout?.removeItem(appEntity3)
        }

        findViewById<Button>(R.id.btn_add4).setOnClickListener {
            appLayout?.addItem(appEntity4)
        }
        findViewById<Button>(R.id.btn_remove4).setOnClickListener {
            appLayout?.removeItem(appEntity4)
        }

        findViewById<Button>(R.id.btn_add5).setOnClickListener {
            appLayout?.addItem(appEntity5)
        }
        findViewById<Button>(R.id.btn_remove5).setOnClickListener {
            appLayout?.removeItem(appEntity5)
        }

        findViewById<Button>(R.id.rows1Columns6).setOnClickListener { appLayout?.setMaxColumnsAndRows(AppLayout.MAX_COLUMNS_6, AppLayout.MAX_ROW_1) }
        findViewById<Button>(R.id.rows2Columns3).setOnClickListener { appLayout?.setMaxColumnsAndRows(AppLayout.MAX_COLUMNS_3, AppLayout.MAX_ROW_2) }
    }
}