package com.cn.board.meet.home.app

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cn.board.meet.home.R
import com.cn.board.meet.home.databinding.ItemAppBinding
import com.cn.core.ui.view.recyclerview.adapter.BaseDragBinderAdapter
import com.cn.core.ui.view.recyclerview.adapter.binder.QuickDataBindingItemBinder
import com.cn.core.ui.view.recyclerview.adapter.listener.OnItemDragListener
import kotlin.math.abs

/**
 * @author: cn
 * @time: 2025/12/23 17:54
 * @history
 * @description:
 */
class AppAggregatorListLayout: RecyclerView {

    companion object {
        private const val MAX_UNINSTALL_WINDOW_DIS_DISTENCE = 10
    }

    private var bManager = false

    /** 拖拽结束、顺序变化后的回调，由上层（Fragment）负责持久化与同步数据源 */
    interface OnOrderChangedListener {
        fun onOrderChanged(order: List<SoftEntity>)
    }

    var onOrderChangedListener: OnOrderChangedListener? = null

    private val mAdapter = BaseDragBinderAdapter()

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        layoutManager = GridLayoutManager(context, 4)
        addItemDecoration(AppItemDecoration())
        adapter = mAdapter.apply {
            addItemBinder(ItemAppBinder(), SoftEntity.DIFF_CALLBACK)
            draggableModule.run {
                isDragEnabled = true
                setOnItemDragListener(dragListener)
            }
        }
    }

    var online = true

    fun submit(list: MutableList<SoftEntity>) {
        mAdapter.setDiffNewData(list.onEach {
            Log.d(AppAggregatorListLayout::class.simpleName, "submit: $it")
        }.toMutableList())
    }

    fun enableManager(bManager: Boolean) {
        this.bManager = bManager
        repeat(mAdapter.data.size) {
            getChildAt(it).findViewById<ImageView>(R.id.home_aggregator_app_choice).visibility = if (bManager) VISIBLE else GONE
        }
    }

    private inner class ItemAppBinder: QuickDataBindingItemBinder<SoftEntity, ItemAppBinding>() {

        override fun onCreateDataBinding(
            layoutInflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int
        ): ItemAppBinding = ItemAppBinding.inflate(layoutInflater, parent, false)

        override fun convert(
            holder: BinderDataBindingHolder<ItemAppBinding>,
            data: SoftEntity
        ) {
            val appInfo = data.appInfo
            // 以「类型 + 包名」作为 key 记录 binding，供后续按 App 定位/更新视图
            with(holder.dataBinding) {
                homeAggregatorAppName.text = appInfo?.name ?: ""
                // bitmap 为聚合阶段异步加载的图标（第三方 App），有则显示；无则沿用布局默认图标
                data.bitmap?.let { homeAggregatorAppIcon.setImageBitmap(it) }
                val choice = if(online) data.appInfo?.main == 1 else data.appInfo?.offlineMain == 1
                homeAggregatorAppChoice.setImageResource(
                    if (choice) R.drawable.icon_app_choiced else R.drawable.icon_app_choice
                )
            }
        }

        override fun onClick(
            holder: BinderDataBindingHolder<ItemAppBinding>,
            view: View,
            data: SoftEntity,
            position: Int
        ) {
            super.onClick(holder, view, data, position)
        }

        override fun onChildClick(
            holder: BinderDataBindingHolder<ItemAppBinding>,
            view: View,
            data: SoftEntity,
            position: Int
        ) = with(holder.dataBinding) {
        }

        override fun onLongClick(
            holder: BinderDataBindingHolder<ItemAppBinding>,
            view: View,
            data: SoftEntity,
            position: Int
        ): Boolean {
            return super.onLongClick(holder, view, data, position)
        }
    }

    private val dragListener = object: OnItemDragListener {
        private var mDX = 0.0F
        private var mDY = 0.2F

        override fun onItemAllowMove(p0: ViewHolder?, p1: Int): Boolean {
            return true
        }

        override fun onItemDragStart(p0: ViewHolder?, p1: Int) {
        }

        override fun onItemDragMoving(p0: ViewHolder?, p1: Int, p2: ViewHolder?, p3: Int) {}

        override fun onItemDragEnd(p0: ViewHolder?, p1: Int) {
            // 拖拽过程中 onItemDragMoving 已把 mAdapter.data 交换为新顺序，
            // 这里取出当前顺序并回调给上层持久化 + 同步 HomeModel.appListFlow
            val order = mAdapter.data.filterIsInstance<SoftEntity>()
            order.forEach {
                Log.d(AppAggregatorListLayout::class.simpleName, "onItemDragEnd: $it")
            }
            if (order.isNotEmpty()) {
                onOrderChangedListener?.onOrderChanged(order)
            }
        }

        override fun onItemDragMovePos(viewHolder: ViewHolder, dX:Float, dY:Float, actionState:Int, isCurrentlyActive:Boolean) {
            mDX = dX
            mDY = dY
            if (abs(dX) > MAX_UNINSTALL_WINDOW_DIS_DISTENCE || abs(dY) > MAX_UNINSTALL_WINDOW_DIS_DISTENCE) {
            }
        }
    }



    private inner class  AppItemDecoration : ItemDecoration() {
        private val spanCount = 4
        private val includeEdge = false
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
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
}