package com.cn.board.home.frag.soft

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cn.board.home.HomeActivityViewModel
import com.cn.board.home.HomeModel
import com.cn.board.home.HomeModelIntent
import com.cn.board.home.R
import com.cn.board.home.binder.loadIcon
import com.cn.board.home.binder.loadTextByAppInfo
import com.cn.board.home.data.appInfoData
import com.cn.board.home.databinding.FragmentSoftBinding
import com.cn.board.home.databinding.ItemSoftBinding
import com.cn.board.home.databinding.ItemSoftMainBinding
import com.cn.board.home.entity.EmAppType
import com.cn.board.home.entity.SoftEntity
import com.cn.board.home.entity.diff.SoftDiffCallBack
import com.cn.board.home.state.State.commandDispatcherModel
import com.cn.board.home.state.State.isOpenAPS
import com.cn.board.home.state.State.mcuModel
import com.cn.core.ui.fragment.BasicVmDBFragment
import com.cn.core.ui.view.recyclerview.adapter.BaseDragBinderAdapter
import com.cn.core.ui.view.recyclerview.adapter.binder.QuickDataBindingItemBinder
import com.cn.core.ui.view.recyclerview.adapter.listener.OnItemDragListener
import com.cn.core.utils.Throttle.throttle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * @author: cn
 * @time: 24/7/2026 上午 11:48
 * @history
 * @description:
 */
class SoftFrag: BasicVmDBFragment<SoftFragViewModel, FragmentSoftBinding>(
    {it[SoftFragViewModel::class.java]},
    { FragmentSoftBinding.inflate(it)}
) {
    companion object {
        private const val MAX_UNINSTALL_WINDOW_DIS_DISTENCE = 10
    }
    private val homeActivityModel: HomeActivityViewModel by activityViewModels()

    private val binderAdapter = BaseDragBinderAdapter()
    private val binderMainAdapter = BaseDragBinderAdapter()

    override fun onBindViewModel() {
        binding.click = ProxyClick()
    }

    override fun onBindData() {
        with(binding) {
            with(mainSoftList) {
                itemAnimator = null
                layoutManager = GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false)
                addItemDecoration(AppMainDecoration())
                adapter = binderMainAdapter.apply {
                    addItemBinder(BinderMainSoft(), SoftDiffCallBack())
                    draggableModule.run {
                        isDragEnabled = true
                        setOnItemDragListener(dragLeftListener)
                    }
                }
            }
            with(softList){
                layoutManager = GridLayoutManager(requireContext(), 4)
                addItemDecoration(AppItemDecoration())
                adapter = binderAdapter.apply {
                    addItemBinder(BinderSoft(), SoftDiffCallBack())
                    draggableModule.run {
                        isDragEnabled = true
                        setOnItemDragListener(dragListener)
                    }
                }
            }
        }
    }

    override fun onObserver() {
        lifecycleScope.launch {
            HomeModel.homeUiState.collectLatest { state ->
                binderAdapter.setDiffNewData(state.allApps.toMutableList())
                binderMainAdapter.setDiffNewData(state.homeAppList.toMutableList())
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    inner class ProxyClick {
        fun onEditType(view: View) {
            with(binding) {
                with(viewModel) {
                    val isSelected = !tvFinish.isVisible
                    mainEditVis.value = if (mainEditVis.value == GONE) VISIBLE else GONE
                    tvAppList.visibility = if (isSelected) GONE else VISIBLE
                    ivSoftMng.visibility = if (isSelected) GONE else VISIBLE
                    tvAppsRight.visibility = if (isSelected) GONE else VISIBLE
                    tvApps.visibility = if (isSelected) VISIBLE else GONE
                    tvFinish.visibility = if (isSelected) VISIBLE else GONE
                    mainSoftList.visibility = if (isSelected) VISIBLE else GONE
                    binderMainAdapter.run {
                        data.forEachIndexed { index, entity ->
                            (entity as SoftEntity).isEdit = mainEditVis.value == VISIBLE && entity.appInfo?.appType != EmAppType.Add
                            notifyItemChanged(index)
                        }
                    }
                    binderAdapter.run {
                        data.forEachIndexed { index, entity ->
                            (entity as SoftEntity).isEdit = mainEditVis.value == VISIBLE
                            notifyItemChanged(index)
                        }
                    }
                    HomeModel.processIntent(HomeModelIntent.InitAppStream)
                }
            }
        }
    }

    private inner class BinderMainSoft: QuickDataBindingItemBinder<SoftEntity, ItemSoftMainBinding>() {

        override fun onClick(holder: BinderDataBindingHolder<ItemSoftMainBinding>, view: View, data: SoftEntity, position: Int) {
            super.onClick(holder, view, data, position)
            throttle {
                if (data.appInfo?.mainIndex == 10000) return@throttle
                val bLogin = isOpenAPS
                if (bLogin || commandDispatcherModel || mcuModel == true) {
                    data.appInfo?.main = 0
                    binderAdapter.data.forEachIndexed { index, any ->
                        if ((any as SoftEntity).appInfo?.main != 1) {
                            binderAdapter.notifyItemChanged(index)
                        }
                    }
                } else {
                    data.appInfo?.offlineMain = 0
                    binderAdapter.data.forEachIndexed { index, any ->
                        if ((any as SoftEntity).appInfo?.offlineMain != 1) {
                            binderAdapter.notifyItemChanged(index)
                        }
                    }
                }
                homeActivityModel.saveHomeAppState(data)
                viewModel.notify(data)
            }
        }

        override fun convert(holder: BinderDataBindingHolder<ItemSoftMainBinding>, data: SoftEntity) = with(holder.dataBinding) {
            (data.appInfo?.let { VISIBLE }?:GONE).let {
                ivtpIcon.visibility = it
                ivThirdIcon.visibility = it
                tvSoftName.visibility = it
            }
            data.appInfo?.let {
                loadIcon(ivtpIcon, it.packageName, it.clazz, it.appType)
                ivThirdIcon.setImageBitmap(data.bitmap)
                loadTextByAppInfo(tvSoftName, it, false)
                ivSelect.visibility = if (data.appInfo?.appType != EmAppType.NONE) VISIBLE else GONE
            }
            ivThirdIcon.setImageBitmap(data.bitmap)
            ivSelect.setImageResource(R.drawable.icon_app_choiced_remove)
        }

        override fun convert(
            holder: BinderDataBindingHolder<ItemSoftMainBinding>,
            data: SoftEntity,
            payloads: List<Any>
        ) = with(holder.dataBinding) {
            if (payloads.isNotEmpty()) convert(holder, data)
            else {
                val payload = payloads.firstOrNull() as Bundle
                when {
                    payload.containsKey("packageName") || payload.containsKey("clazz") || payload.containsKey("appType") || payload.containsKey("isSelect")  -> {
                        (data.appInfo?.let { VISIBLE }?:GONE).let {
                            ivtpIcon.visibility = it
                            ivThirdIcon.visibility = it
                            tvSoftName.visibility = it
                        }
                        data.appInfo?.let {
                            loadIcon(ivtpIcon, it.packageName, it.clazz, it.appType)
                            ivThirdIcon.setImageBitmap(data.bitmap)
                            loadTextByAppInfo(tvSoftName, it, false)
                        }
                        ivSelect.apply { setImageResource(R.drawable.icon_app_choiced_remove) }.visibility = if (data.appInfo?.appType != EmAppType.NONE) VISIBLE else GONE
                    }
                }
            }
        }

        override fun onCreateDataBinding(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemSoftMainBinding =
            ItemSoftMainBinding.inflate(layoutInflater, parent, false)

    }

    private inner class BinderSoft: QuickDataBindingItemBinder<SoftEntity, ItemSoftBinding>() {

        override fun onClick(holder: BinderDataBindingHolder<ItemSoftBinding>, view: View, data: SoftEntity, position: Int) {
            super.onClick(holder, view, data, position)
            throttle { data.run {
                if (isEdit) {
                    appInfo?.let {
                        appInfoData.mainAppListSize.let { size ->

                            val bLogin = isOpenAPS
                            if (bLogin || commandDispatcherModel || mcuModel) {
                                val main = it.main
                                it.main = if (size < 6 && main == 0) 1 else 0
                                if (main != it.main) {
                                    homeActivityModel.saveHomeAppState(this)
                                    binderAdapter.notifyItemChanged(position)
                                }
                                binderAdapter.data.forEachIndexed { index, any ->
                                    if ((any as SoftEntity).appInfo?.main != 1) {
                                        binderAdapter.notifyItemChanged(index)
                                    }
                                }
                            } else {
                                val main = it.offlineMain
                                it.offlineMain = if (size < 6 && main == 0) 1 else 0
                                if (main != it.offlineMain) {
                                    homeActivityModel.saveHomeAppState(this)
                                    binderAdapter.notifyItemChanged(position)
                                }
                                binderAdapter.data.forEachIndexed { index, any ->
                                    if ((any as SoftEntity).appInfo?.offlineMain != 1) {
                                        binderAdapter.notifyItemChanged(index)
                                    }
                                }
                            }
                        }
                    }
                } else { }
            } }
        }

        override fun onLongClick(holder: BinderDataBindingHolder<ItemSoftBinding>, view: View, data: SoftEntity, position: Int): Boolean {
            throttle {
                Log.d(TAG, "onLongClick ${view.hasFocus()} appPermission ${HomeModel.appPermission.value}")
                if (!HomeModel.appPermission.value){
                    return@throttle
                }
                if (!view.hasFocus()) { data.run { if (!isEdit) {
                    if (binding.mainSoftList.visibility != VISIBLE) {
                        if (appInfo?.appType == EmAppType.Third && !isEdit && appInfo?.allowDelete == true) {

                        }
                    }
                    alphaFilterItem(position, 1)
                } } }
            }
            return super.onLongClick(holder, view, data, position)
        }

        @SuppressLint("NewApi", "ClickableViewAccessibility")
        override fun convert(holder: BinderDataBindingHolder<ItemSoftBinding>, data: SoftEntity) {
            holder.dataBinding.info = data
        }

        override fun convert(holder: BinderDataBindingHolder<ItemSoftBinding>, data: SoftEntity, payloads: List<Any>) {
            super.convert(holder, data, payloads)
            holder.run {
                itemView.run {
                    payloads.forEach {
                        when (it as Int) {
                            1 -> alpha = 0.2f
                            2 -> alpha = 1.0f
                            3 -> {
                                dataBinding.ivSelect.visibility = if (data.isShowSelect) VISIBLE else GONE
                                isSelected = data.appInfo?.main == 1 && data.isEdit || !data.isEdit

                            }
                        }
                    }
                }
            }
        }

        override fun onCreateDataBinding(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemSoftBinding {
            return ItemSoftBinding.inflate(layoutInflater, parent, false)
        }
    }

    private fun alphaFilterItem(position: Int, type:Int) {
        binderAdapter.data.forEachIndexed { index, _ ->
            if (index != position) binding.softList.layoutManager?.findViewByPosition(index)?.alpha = if (type == 1)0.2f else 1f
        }
    }

    private inner class  AppMainDecoration: RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)
            val index = parent.getChildAdapterPosition(view)
            outRect.bottom = if (index == 1 || index == 3 || index == 5) 0 else resources.getDimensionPixelSize(R.dimen.dp2)?:0
            outRect.right = if (index == 4 || index == 5) 0 else resources.getDimensionPixelSize(R.dimen.dp2)?:0
        }
    }

    private inner class  AppItemDecoration : RecyclerView.ItemDecoration() {
        private val spanCount = 4
        private val includeEdge = false//includeEdge = true表示整个网格四周也有 4dp 间距（item 与父容器边缘）；若设为 false 则只有 item 之间有间距，边缘紧贴父容器
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)

            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount
            val spacing = resources.getDimensionPixelSize(R.dimen.dp4)

            if (includeEdge) {
                // 每个 item 左右各分一半间距，使相邻边总间距 = spacing
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount

                // 第一行顶部加间距
                if (position < spanCount) {
                    outRect.top = spacing
                }
                // 每行底部都加间距（最后一行也会多出底部间距）
                outRect.bottom = spacing
            } else {
                // 不包含边缘时，只在 item 之间加间距（最左和最右无外边距）
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount
                if (position >= spanCount) {
                    outRect.top = spacing
                }
            }
        }
    }

    private val dragLeftListener = object: OnItemDragListener {

        private var startIndex = -1

        override fun onItemAllowMove(p0: RecyclerView.ViewHolder?, p1: Int): Boolean {
            return (binderMainAdapter.getItem(p1) as SoftEntity).appInfo?.appType != EmAppType.NONE
        }

        override fun onItemDragStart(p0: RecyclerView.ViewHolder?, p1: Int) {
            startIndex = p1
        }

        override fun onItemDragMoving(p0: RecyclerView.ViewHolder?, p1: Int, p2: RecyclerView.ViewHolder?, p3: Int) {
        }

        override fun onItemDragEnd(p0: RecyclerView.ViewHolder?, p1: Int) {
            if (p1 != startIndex) {
                viewModel.updateMainAppOrder(binderMainAdapter.data as ArrayList<SoftEntity>)
            }
        }

        override fun onItemDragMovePos(viewHolder:RecyclerView.ViewHolder,dX:Float, dY:Float, actionState:Int, isCurrentlyActive:Boolean) {
        }
    }

    private val dragListener = object: OnItemDragListener {

        private var startIndex = 0
        private var mDX = 0.0F
        private var mDY = 0.2F

        override fun onItemAllowMove(p0: RecyclerView.ViewHolder?, p1: Int): Boolean {
            return true//viewModel.mainEditVis.value == View.GONE
        }

        override fun onItemDragStart(p0: RecyclerView.ViewHolder?, p1: Int) {
            startIndex = p1
        }

        override fun onItemDragMoving(p0: RecyclerView.ViewHolder?, p1: Int, p2: RecyclerView.ViewHolder?, p3: Int) {}

        override fun onItemDragEnd(p0: RecyclerView.ViewHolder?, p1: Int) {
            lifecycleScope.launch {
                if (startIndex != p1) {
                    viewModel.extractedSortApp(startIndex, p1, binderAdapter.data as MutableList<SoftEntity>)
                }
//                resetSoftListView()
                alphaFilterItem(-1, 0)
            }
        }

        //拖动的Item移动距离改变监听，dx,dy分别为x和y方向的移动距离，初始值0，该函数内尽量不要做耗时操作来源为onChildDraw，耗时操作会影响拖动流畅度
        override fun onItemDragMovePos(viewHolder:RecyclerView.ViewHolder,dX:Float, dY:Float, actionState:Int, isCurrentlyActive:Boolean) {
            mDX = dX
            mDY = dY
            if (abs(dX) > MAX_UNINSTALL_WINDOW_DIS_DISTENCE || abs(dY) > MAX_UNINSTALL_WINDOW_DIS_DISTENCE) {

            }
        }
    }
}