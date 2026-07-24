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
 * 把卡片内除视频外的 UI 子 View 搬进 SurfaceControlViewHost，并抬到
 * ABOVE_WINDOW 的视频之上（目标 Android 31，系统权限）。
 *
 * ## 层级（从下到上）
 *   窗口内容：卡片毛玻璃模糊背景（SYSTEM 跨窗口模糊 → 模糊壁纸）
 *     └ 视频 SurfaceView · ABOVE_WINDOW（清晰，避开模糊）
 *          └ UI overlay · SurfaceControlViewHost（标题/列表，置于视频之上）
 *
 * ## API 说明（已对照 ROM 自带 system.jar 与 compileSdk 35 核实）
 * 公开 / 直接调用部分（[SurfaceControlViewHost] 本身是 API 29+ 的公开类，无需反射）：
 *   - [SurfaceControlViewHost] 构造 `SurfaceControlViewHost(Context, Display, IBinder)`
 *   - 根 SCVH 的 hostToken 用公开 API [View.getWindowToken]（`decorView.windowToken`），
 *     即窗口的 IWindow token——这正是 SCVH 构造需要的 host token。
 *
 * 涉及 `system.jar` 的 [SurfaceControlViewHost.SurfacePackage.getSurfaceControl] 以及
 * [SurfaceControl] / [SurfaceControl.Transaction] 合成操作
 * （取视频 SC、reparent、setPosition、setBufferSize、setVisibility、setRelativeLayer、
 * setLayer、apply）**统一走 [SurfaceControlZOrder] 的反射层**：
 *   - 这些成员在 SDK 35 仍为 @hide 或跨 ROM 签名不同；
 *   - 反射 + try-catch 让非系统 / 严格 ROM 上优雅降级（[SurfaceControlZOrder.isAvailable] 返回 false 时 no-op）；
 *   - 与 FrostedAnimatedGlowView 的反射范式保持一致，避免各模块重复写反射。
 *
 * @author cn
 */
class HomeVideoUiOverlayHost(
    private val activity: Activity,
    private val card: ViewGroup,
    private val video: SurfaceView,
) {
    private val TAG = "VideoUiOverlayHost"
    private var scvh: SurfaceControlViewHost? = null
    private var overlayRoot: ViewGroup? = null
    private var lastW = -1
    private var lastH = -1
    /** 最近一次叠加到屏幕坐标上的水平平移量（来自 SwipeFragmentPager 的页容器 translationX）。 */
    private var lastDx = 0f

    /** 跟随卡片布局变化（如管理模式切换导致尺寸变化）重定位 UI overlay。 */
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { reposition(lastDx) }

    /**
     * 搭建 UI overlay：创建 SCVH，把 UI 子 View 从卡片搬进独立 surface 并抬到视频之上。
     * 任何一步失败都安全回退（不移动 UI，视频仍可见）。
     *
     * @param initialDx 该页容器当前的水平平移量（px），来自 SwipeFragmentPager。
     *   多页场景下页容器靠 translationX 错开，而 getLocationOnScreen 不含 translationX，
     *   必须在此把初始平移量叠加进去，否则离屏页的浮层会被定位到原点、与当前页重叠。
     */
    fun setup(initialDx: Float = 0f) {
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
        reposition(initialDx)

        // 6) 跟随卡片布局变化重定位
        card.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        Log.d(TAG, "setup OK（UI 已抬到视频之上）")
    }

    /**
     * 重定位 UI overlay 并设置其相对视频的层级（置于视频之上）。
     * 尺寸变化时重新 setView 触发内部 View 重新布局。
     *
     * @param extraDx 需要叠加到屏幕 X 坐标上的额外水平偏移（px）。SwipeFragmentPager 托管的
     *   多页场景中，页容器靠 [View.setTranslationX] 错开，但
     *   [View.getLocationOnScreen] 返回的是「布局坐标、不含 translationX」，
     *   所以必须把该平移量叠加上去，否则多页的 UI overlay 会叠在同一屏幕位置。
     */
    fun reposition(extraDx: Float = 0f) {
        val host = scvh ?: return
        if (card.width <= 0 || card.height <= 0) return
        lastDx = extraDx

        // 真实尺寸变化：重新 setView 让内部 View 重新测量布局
        if (card.width != lastW || card.height != lastH) {
            overlayRoot?.let { host.setView(it, card.width, card.height) }
            lastW = card.width
            lastH = card.height
        }

        // UI overlay 的 live SurfaceControl（走反射跨 ROM，[SurfacePackage.getSurfaceControl] 在部分 classpath 上不可见）
        val uiSc = SurfaceControlZOrder.getSurfaceControl(host.surfacePackage) ?: run {
            Log.w(TAG, "reposition: 无法获取 UI SurfaceControl，放弃定位")
            return
        }
        // 视频的 SurfaceControl（ABOVE_WINDOW，由 setZOrderOnTop(true) 保证持久）
        val videoSc = SurfaceControlZOrder.getSurfaceControl(video)

        // SCVH 的 surface 默认挂在 display 级，原点 = 屏幕原点，故用屏幕坐标直接定位；
        // 不再 reparent 到 windowSc，避免与视频（display 级、setZOrderOnTop）父级不一致，
        // 导致 setRelativeLayer(uiSc, videoSc, 1) 跨父级失效。
        val cardLoc = IntArray(2)
        card.getLocationOnScreen(cardLoc)
        // getLocationOnScreen 不含 translationX，这里手动叠加页容器的平移量
        val x = cardLoc[0] + extraDx
        val y = cardLoc[1]

        // 全部走 SurfaceControlZOrder 的反射层（system.jar 的 @hide Transaction 操作），
        // 跨 ROM 安全降级；不再直接持有 SurfaceControl.Transaction 实例。
        val tx = SurfaceControlZOrder.createTransaction() ?: run {
            Log.w(TAG, "reposition: 创建 Transaction 失败（反射降级），放弃本次定位")
            return
        }
        SurfaceControlZOrder.setPosition(tx, uiSc, x.toFloat(), y.toFloat())
        SurfaceControlZOrder.setBufferSize(tx, uiSc, card.width, card.height)
        SurfaceControlZOrder.setVisibility(tx, uiSc, true)  // 始终可见（离屏隐藏暂未实现）
        // 关键：把 UI 层相对视频层 +1，确保 UI 一定在视频之上，不受绝对层数值影响
        if (videoSc != null) {
            SurfaceControlZOrder.setRelativeLayer(tx, uiSc, videoSc, 1)
        } else {
            SurfaceControlZOrder.setLayer(tx, uiSc, Int.MAX_VALUE / 2 + 1)
        }
        SurfaceControlZOrder.apply(tx)
        Log.d(TAG, "reposition: pos=($x,$y) size=${card.width}x${card.height} visible=true videoSc=${videoSc != null}")
    }

    /** 释放 SCVH（Fragment onDestroyView 时调用）。 */
    fun release() {
        runCatching { card.viewTreeObserver?.removeOnGlobalLayoutListener(layoutListener) }
        scvh?.let { runCatching { it.release() } }
        scvh = null
        overlayRoot = null
    }
}
