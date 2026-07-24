package com.cn.core.ui.view

import android.app.Activity
import android.util.Log
import android.view.SurfaceControl
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.cn.core.ui.view.frosted.SurfaceControlZOrder

/**
 * @author: cn
 * @time: 22/7/2026 上午 11:39
 * @history
 * @description:
 */
class HomeVideoUiOverlayHostStart(
    private val activity: Activity,
    private val card: ViewGroup,
    private val video: SurfaceView,
) {
    private val TAG = "VideoUiOverlayHost"
    private var scvh: SurfaceControlViewHost? = null
    private var overlayRoot: ViewGroup? = null
    private var lastW = -1
    private var lastH = -1

    /** 跟随卡片布局变化（如管理模式切换导致尺寸变化）重定位 UI overlay。 */
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { reposition() }

    /**
     * 搭建 UI overlay：创建 SCVH，把 UI 子 View 从卡片搬进独立 surface 并抬到视频之上。
     * 任何一步失败都安全回退（不移动 UI，视频仍可见）。
     */
    fun setup() {
        if (scvh != null) return
        if (card.width <= 0 || card.height <= 0) {
            Log.w(TAG, "setup: card 尚未布局完成，跳过")
            return
        }
        // 视频的 ABOVE_WINDOW 由 HomeVideoSurfaceView.setZOrderOnTop(true) 持久保证，
        // 这里无需再手动调 bringAboveWindow（手动 Transaction 会被 updateSurface 重置）。

        // 1) hostToken：根 SCVH 直接传窗口 token（公开 API，无需反射）
        //    ⚠️ 不能用 View.getHostToken()/WindowManager.getHostToken()——它们在本 ROM 的
        //       framework 中不存在（getHostToken 是给「已处在 SCVH 内部的 View」取父 token 用的）。
        //       正确来源就是 decorView.getWindowToken()，即窗口的 IWindow token。
        val hostToken = activity.window?.decorView?.windowToken
        if (hostToken == null) {
            Log.w(TAG, "setup: hostToken=null，放弃 SCVH")
            return
        }
        val display = activity.display ?: run {
            Log.w(TAG, "setup: display=null，放弃 SCVH")
            return
        }

        // 3) 收集卡片内除视频外的 UI 子 View
        val uiChildren = ArrayList<View>()
        for (i in 0 until card.childCount) {
            card.getChildAt(i)?.let { if (it != video) uiChildren.add(it) }
        }
        if (uiChildren.isEmpty()) {
            Log.w(TAG, "setup: 无 UI 子 View，跳过")
            return
        }

        // 4) overlay 根（与原卡片同尺寸）
        val root = ConstraintLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // 5) 创建 SCVH 并塞入 UI 子 View（实例不变 → data binding 仍有效）
        val host = try {
            SurfaceControlViewHost(activity, display, hostToken)
        } catch (e: Throwable) {
            Log.e(TAG, "setup: SCVH 创建失败", e)
            return
        }
        for (child in uiChildren) {
            card.removeView(child)
            root.addView(child, child.layoutParams)
        }
        // setView(View, width, height) 为公开签名
        host.setView(root, card.width, card.height)
        lastW = card.width
        lastH = card.height

        scvh = host
        overlayRoot = root
        reposition()

        // 6) 跟随卡片布局变化重定位
        card.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        Log.d(TAG, "setup OK（UI 已抬到视频之上）")
    }

    /**
     * 重定位 UI overlay 并设置其相对视频的层级（置于视频之上）。
     * 尺寸变化时重新 setView 触发内部 View 重新布局。
     */
    fun reposition() {
        val host = scvh ?: return
        if (card.width <= 0 || card.height <= 0) return

        // 真实尺寸变化：重新 setView 让内部 View 重新测量布局
        if (card.width != lastW || card.height != lastH) {
            overlayRoot?.let { host.setView(it, card.width, card.height) }
            lastW = card.width
            lastH = card.height
        }

        // UI overlay 的 live SurfaceControl（公开 API）
        val uiSc = host.surfacePackage?.surfaceControl ?: run {
            Log.w(TAG, "reposition: SurfacePackage 为空，无法定位")
            return
        }
        // 视频的 SurfaceControl（ABOVE_WINDOW，由 setZOrderOnTop(true) 保证持久）
        val videoSc = SurfaceControlZOrder.getSurfaceControl(video)

        // SCVH 的 surface 默认挂在 display 级，原点 = 屏幕原点，故用屏幕坐标直接定位；
        // 不再 reparent 到 windowSc，避免与视频（display 级、setZOrderOnTop）父级不一致，
        // 导致 setRelativeLayer(uiSc, videoSc, 1) 跨父级失效。
        val cardLoc = IntArray(2)
        card.getLocationOnScreen(cardLoc)
        val x = cardLoc[0]
        val y = cardLoc[1]

        val tx = SurfaceControl.Transaction()
        tx.setPosition(uiSc, x.toFloat(), y.toFloat())
        tx.setBufferSize(uiSc, card.width, card.height)
        tx.setVisibility(uiSc, true) // boolean visible = true
        // 关键：把 UI 层相对视频层 +1，确保 UI 一定在视频之上，不受绝对层数值影响
        if (videoSc != null) {
            setRelativeLayer(tx, uiSc, videoSc, 1)
        } else {
            tx.setLayer(uiSc, Int.MAX_VALUE / 2 + 1)
        }
        tx.apply()
        Log.d(TAG, "reposition: pos=($x,$y) size=${card.width}x${card.height} videoSc=${videoSc != null}")
    }

    /** 反射调用隐藏的 Transaction.setRelativeLayer(SurfaceControl, SurfaceControl, int)。 */
    private fun setRelativeLayer(tx: SurfaceControl.Transaction, sc: SurfaceControl, relTo: SurfaceControl, layer: Int) {
        try {
            val m = SurfaceControl.Transaction::class.java.getMethod(
                "setRelativeLayer",
                SurfaceControl::class.java,
                SurfaceControl::class.java,
                Int::class.javaPrimitiveType
            )
            m.isAccessible = true
            m.invoke(tx, sc, relTo, layer)
        } catch (e: Throwable) {
            Log.w(TAG, "setRelativeLayer: ${e.message}，fallback 到绝对层")
            tx.setLayer(sc, Int.MAX_VALUE / 2 + 1)
        }
    }

    /** 释放 SCVH（Fragment onDestroyView 时调用）。 */
    fun release() {
        runCatching { card.viewTreeObserver?.removeOnGlobalLayoutListener(layoutListener) }
        scvh?.let { runCatching { it.release() } }
        scvh = null
        overlayRoot = null
    }
}