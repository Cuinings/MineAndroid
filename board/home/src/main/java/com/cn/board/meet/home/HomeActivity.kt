package com.cn.board.meet.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "HomeActivity"
    }
    
    private var fragmentContainer: View? = null
    private var mainView: View? = null
    
    private var isTouchServiceBound = false
    private var isOverviewServiceBound = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - isTouchServiceBound: $isTouchServiceBound, isOverviewServiceBound: $isOverviewServiceBound")
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        mainView = findViewById<View>(R.id.main)
        fragmentContainer = findViewById<View>(R.id.fragment_container)
        
        ViewCompat.setOnApplyWindowInsetsListener(mainView!!) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AppListFragment())
                .commit()
        }
    }
    
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart - isTouchServiceBound: $isTouchServiceBound, isOverviewServiceBound: $isOverviewServiceBound")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - HomeActivity resumed, enabling gestures")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause - HomeActivity paused, disabling gestures")
    }
    
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop - isTouchServiceBound: $isTouchServiceBound, isOverviewServiceBound: $isOverviewServiceBound")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - isTouchServiceBound: $isTouchServiceBound, isOverviewServiceBound: $isOverviewServiceBound")
    }
}