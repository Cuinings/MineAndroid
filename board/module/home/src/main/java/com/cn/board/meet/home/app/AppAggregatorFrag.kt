package com.cn.board.meet.home.app

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.cn.board.meet.home.HomeModel
import com.cn.board.meet.home.HomeModel.appListFlow
import com.cn.board.meet.home.HomeModel.mainAppListFlow
import com.cn.board.meet.home.HomeVideoUiOverlayHost
import com.cn.board.meet.home.databinding.FragmentAppAggregatorBinding
import com.cn.core.ui.collectOnStarted
import com.cn.core.ui.fragment.BasicVmDBFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author: cn
 * @time: 14/7/2026 下午 3:10
 * @history
 * @description: 首页应用聚合 Fragment，展示 AppStreamAggregator 扫描/对齐后的应用列表
 */
class AppAggregatorFrag: BasicVmDBFragment<AppAggregatorFragModel, FragmentAppAggregatorBinding>(
    { viewModelProvider -> viewModelProvider[AppAggregatorFragModel::class.java] },
    { FragmentAppAggregatorBinding.inflate(it) }
) {

    private val TAG = "AppAggregatorFrag"

    /**
     * 承载卡片 UI（标题/管理按钮/列表）的 overlay 宿主。
     * 类型用 [Any] 而非 [SurfaceControlViewHost]，把所有 SurfaceControl/SurfaceControlViewHost
     * 引用关在 [HomeVideoUiOverlayHost] 内（仅在 API 29+ 懒加载），避免在 <29 设备上
     * 因缺少 SurfaceControlViewHost 类而导致 Fragment 类 VerifyError。
     */
    private var videoUiScvh: Any? = null

    /** 跟随卡片布局变化（如管理模式切换导致尺寸变化）重定位 UI overlay。 */
    private val overlayLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        (videoUiScvh as? HomeVideoUiOverlayHost)?.reposition()
    }

    override fun onBindViewModel() {
    }

    override fun onBindData() {
        // 绑定点击事件变量，避免布局里的 onClick 在 click 为 null 时 NPE
        binding.click = AAppAggregatorFragClick()

        // 视频 ABOVE_WINDOW + UI 经 SurfaceControlViewHost 抬到视频之上（需 API 29+）。
        // 等卡片完成首次布局后再搭建，确保能拿到正确的屏幕尺寸/位置。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val card = binding.mainActivityFragAppManagerConstraintLayout
            val trigger = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (card.width <= 0 || card.height <= 0) return
                    card.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    setupVideoUiOverlay()
                }
            }
            card.viewTreeObserver.addOnGlobalLayoutListener(trigger)
        }
    }

    override fun onObserver() {
        // 订阅 HomeModel 持有的聚合结果流：init() 完成后由 HomeModel 写入。
        // StateFlow 会向新订阅者重放最新值，因此本 Fragment 即使晚于 init() 创建，
        // 也能立即拿到已聚合好的列表；STARTED 时收集、STOPPED 时自动取消，避免泄漏。
        viewLifecycleOwner.collectOnStarted(appListFlow) { list ->
            binding.mainActivityFragAppManagerListLayout.submit(list.toMutableList())
        }
        viewLifecycleOwner.collectOnStarted(mainAppListFlow) { list ->
            binding.mainActivityFragAppManagerEditListLayout.submit(list.toMutableList())
        }

        // 拖拽结束后把新顺序持久化到 DB，并同步回 appListFlow，保证顺序不丢失
        binding.mainActivityFragAppManagerListLayout.onOrderChangedListener = object : AppAggregatorListLayout.OnOrderChangedListener {
            override fun onOrderChanged(order: List<SoftEntity>) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    HomeModel.updateAppOrder(order)
                }
            }
        }

        // 管理模式下点击应用：切换 main（在线）/ offlineMain（离线）标记，并刷新列表与编辑列表
        binding.mainActivityFragAppManagerListLayout.onItemSelectedListener = object : AppAggregatorListLayout.OnItemSelectedListener {
            override fun onItemSelected(entity: SoftEntity, online: Boolean) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    HomeModel.toggleMainFlag(entity, online)
                }
            }
        }
    }

    // ================================================================
    //  视频 ABOVE_WINDOW + UI 经 SurfaceControlViewHost 抬到视频之上
    //
    //  层级（从下到上）：
    //    窗口内容（卡片模糊背景，SYSTEM 跨窗口模糊 → 模糊壁纸）
    //      └─ 视频 SurfaceView · ABOVE_WINDOW（清晰，避开模糊）
    //            └─ UI overlay · SurfaceControlViewHost（标题/列表，置于视频之上）
    //
    //  具体实现全部在 [HomeVideoUiOverlayHost]（API 29+ 才加载，避免 <29 VerifyError）。
    // ================================================================

    /**
     * 搭建 UI overlay（API 29+）。仅把视频抬到窗口之上、把卡片 UI 搬进独立 Surface 抬到视频之上。
     * 任何一步失败都安全回退（不移动 UI，视频仍可见）。
     */
    private fun setupVideoUiOverlay() {
        val card = binding.mainActivityFragAppManagerConstraintLayout
        val video = binding.homeVideoSurfaceView
        videoUiScvh = HomeVideoUiOverlayHost(requireActivity(), card, video)
        (videoUiScvh as? HomeVideoUiOverlayHost)?.setup()
    }

    override fun onDestroyView() {
        // 先移除监听并释放 SCVH（此时 binding 仍有效）
        runCatching { binding.mainActivityFragAppManagerConstraintLayout.viewTreeObserver?.removeOnGlobalLayoutListener(overlayLayoutListener) }
        (videoUiScvh as? HomeVideoUiOverlayHost)?.release()
        videoUiScvh = null
        super.onDestroyView()
    }

    inner class AAppAggregatorFragClick {

        fun manager(view: View) {
            with(binding) {
                val value = !mainActivityFragAppManagerFinish.isVisible
                mainActivityFragAppManagerListLayout.enableManager(value)
                if (value) {
                    mainActivityFragAppManagerAppListTitle.visibility = View.GONE
                    mainActivityFragAppManagerRight.visibility = View.GONE
                    mainActivityFragAppManagerTitle.visibility = View.VISIBLE
                    mainActivityFragAppManagerFinish.visibility = View.VISIBLE
                    mainActivityFragAppManagerEditListLayout.visibility = View.VISIBLE
                } else {
                    mainActivityFragAppManagerTitle.visibility = View.GONE
                    mainActivityFragAppManagerFinish.visibility = View.GONE
                    mainActivityFragAppManagerEditListLayout.visibility = View.GONE
                    mainActivityFragAppManagerAppListTitle.visibility = View.VISIBLE
                    mainActivityFragAppManagerRight.visibility = View.VISIBLE
                }
            }
        }
    }
}
