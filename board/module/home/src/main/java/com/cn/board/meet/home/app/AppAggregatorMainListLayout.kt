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
import com.cn.board.database.AppInfo
import com.cn.board.database.EmAppType
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
class AppAggregatorMainListLayout: RecyclerView {

    companion object {
        private const val MAX_UNINSTALL_WINDOW_DIS_DISTENCE = 10
        /** 首页主管理区固定总槽位数：2 行 × 3 列 = 6 个 */
        private const val MAIN_SLOT_COUNT = 6
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
        layoutManager = GridLayoutManager(context, 2, HORIZONTAL, false)
//        addItemDecoration(AppItemDecoration())
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
        val data = list.toMutableList().apply {
            // 主管理区固定 6 个槽位：真实应用不足时，用占位空槽补齐，保持 2×3 的网格始终满格显示
            repeat((MAIN_SLOT_COUNT - size).coerceAtLeast(0)) {
                add(SoftEntity(isPlaceholder = true))
            }
        }
        mAdapter.setDiffNewData(data.onEach {
            Log.d(AppAggregatorMainListLayout::class.simpleName, "submit: $it")
        }.toMutableList())
    }

    fun enableManager(bManager: Boolean) {
        this.bManager = bManager
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val position = getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue
            val item = mAdapter.data.getOrNull(position) as? SoftEntity ?: continue
            if (item.isPlaceholder) continue
            child.findViewById<ImageView>(R.id.home_aggregator_app_choice).visibility = VISIBLE
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
            with(holder.dataBinding) {
                if (data.isPlaceholder) {
                    // 占位空槽：居中显示红色箭头占位图标，隐藏名称/选择角标/第三方角标，且不可交互
                    homeAggregatorAppIcon.visibility = GONE
                    homeAggregatorThirdAppIcon.visibility = GONE
                    homeAggregatorAppName.visibility = GONE
                    homeAggregatorAppChoice.visibility = GONE
                    return@with
                }
                // 真实应用：恢复可见性
                homeAggregatorAppIcon.visibility = when(data.appInfo?.appType) {
                    EmAppType.tp -> VISIBLE
                    else -> GONE
                }
                homeAggregatorThirdAppIcon.visibility = when(data.appInfo?.appType) {
                    EmAppType.Third -> VISIBLE
                    else -> GONE
                }
                homeAggregatorAppName.visibility = VISIBLE

                val appInfo = data.appInfo
                homeAggregatorAppName.text = appInfo?.name ?: ""
                // 恢复系统应用默认图标（避免复用占位空槽时仍显示红色箭头）
                if (appInfo?.appType == EmAppType.tp) {
                    homeAggregatorAppIcon.setImageResource(android.R.drawable.ic_menu_camera)
                }
                // bitmap 为聚合阶段异步加载的图标（第三方 App），有则显示；无则沿用布局默认图标
                data.bitmap?.let { homeAggregatorThirdAppIcon.setImageBitmap(it) }
                homeAggregatorAppChoice.setImageResource(R.drawable.icon_app_choiced)
                homeAggregatorAppChoice.visibility = VISIBLE
                root.isEnabled = true
                root.isFocusable = true
                root.isFocusableInTouchMode = false
            }
        }

        override fun onClick(
            holder: BinderDataBindingHolder<ItemAppBinding>,
            view: View,
            data: SoftEntity,
            position: Int
        ) {
            if (data.isPlaceholder) return
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
            if (data.isPlaceholder) return false
            return super.onLongClick(holder, view, data, position)
        }
    }

    private val dragListener = object: OnItemDragListener {
        private var mDX = 0.0F
        private var mDY = 0.2F

        override fun onItemAllowMove(p0: ViewHolder?, p1: Int): Boolean {
            // 占位空槽不允许被拖拽，防止破坏固定 2×3 网格布局
            return mAdapter.data.getOrNull(p1)?.let { it is SoftEntity && !it.isPlaceholder } ?: false
        }

        override fun onItemDragStart(p0: ViewHolder?, p1: Int) {
        }

        override fun onItemDragMoving(p0: ViewHolder?, p1: Int, p2: ViewHolder?, p3: Int) {}

        override fun onItemDragEnd(p0: ViewHolder?, p1: Int) {
            // 拖拽过程中 onItemDragMoving 已把 mAdapter.data 交换为新顺序，
            // 这里过滤掉占位空槽，只把真实应用的新顺序回调给上层持久化 + 同步 HomeModel.appListFlow
            val order = mAdapter.data.filterIsInstance<SoftEntity>().filter { !it.isPlaceholder }
            order.forEach {
                Log.d(AppAggregatorMainListLayout::class.simpleName, "onItemDragEnd: $it")
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
        // GridLayoutManager 为 HORIZONTAL 2 列，装饰器的 spanCount 必须保持一致，否则行内/行间间距会错位
        private val spanCount = 2
        private val includeEdge = false
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
            super.getItemOffsets(outRect, view, parent, state)

            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
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