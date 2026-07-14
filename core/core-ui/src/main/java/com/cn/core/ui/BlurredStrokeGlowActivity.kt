//package com.cn.core.ui
//
//import android.os.Bundle
//import androidx.appcompat.app.AppCompatActivity
//import com.cn.core.ui.view.frosted.glass.BlurredStatefulStrokeGlowView
//import com.cn.core.ui.R
//
//class BlurredStrokeGlowActivity : AppCompatActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_blurred_stroke_glow)
//
//        val blurredStrokeGlowView = findViewById<BlurredStatefulStrokeGlowView>(R.id.blurredStrokeGlowView)
//
//        // 让视图获得焦点以显示内发光效果
//        blurredStrokeGlowView.requestFocus()
//
//        // 可选：设置模糊半径
//        // blurredStrokeGlowView.setBlurRadius(15f)
//
//        // 可选：设置模糊区域的圆角半径
//        // blurredStrokeGlowView.setBlurCornerRadius(20f)
//    }
//
//    // 注意：由于使用了简单的模糊策略，不需要在 onDestroy 中释放额外资源
//    // 资源会在视图分离时自动释放
//}
