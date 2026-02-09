package com.cn.core.ui.view.recyclerview.adapter.module

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.cn.core.ui.R
import com.cn.core.ui.view.recyclerview.adapter.BaseQuickAdapter
import com.cn.core.ui.view.recyclerview.adapter.dragswipe.DragAndSwipeCallback
import com.cn.core.ui.view.recyclerview.adapter.holder.BaseViewHolder
import com.cn.core.ui.view.recyclerview.adapter.listener.DraggableListenerImp
import com.cn.core.ui.view.recyclerview.adapter.listener.OnItemDragListener
import com.cn.core.ui.view.recyclerview.adapter.listener.OnItemSwipeListener
import java.util.*

/**
 * 需要【拖拽】功能的，[BaseQuickAdapter]继承此接口
 */
interface DraggableModule {
    /**
     * 重写此方法，返回自定义模块
     * @param baseQuickAdapter BaseQuickAdapter<*, *>
     * @return BaseExpandableModule
     */
    fun addDraggableModule(baseQuickAdapter: BaseQuickAdapter<*, *>): BaseDraggableModule {
        return BaseDraggableModule(baseQuickAdapter)
    }
}

open class BaseDraggableModule(private val baseQuickAdapter: BaseQuickAdapter<*, *>) :
    DraggableListenerImp {

    var isDragEnabled = false
    var isSwipeEnabled = false
    var toggleViewId = NO_TOGGLE_VIEW
    lateinit var itemTouchHelper: ItemTouchHelper
    lateinit var itemTouchHelperCallback: DragAndSwipeCallback

    protected var mOnToggleViewTouchListener: OnTouchListener? = null
    protected var mOnToggleViewLongClickListener: OnLongClickListener? = null
    protected var mOnItemDragListener: OnItemDragListener? = null
    protected var mOnItemSwipeListener: OnItemSwipeListener? = null

    init {
        initItemTouch()
    }

    private fun initItemTouch() {
        itemTouchHelperCallback =
            DragAndSwipeCallback(
                this
            )
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
    }

    internal fun initView(holder: BaseViewHolder) {
        if (isDragEnabled) {
            if (hasToggleView()) {
                val toggleView = holder.itemView.findViewById<View>(toggleViewId)
                if (toggleView != null) {
                    toggleView.setTag(R.id.BaseQuickAdapter_viewholder_support, holder)
                    if (isDragOnLongPressEnabled) {
                        toggleView.setOnLongClickListener(mOnToggleViewLongClickListener)
                    } else {
                        toggleView.setOnTouchListener(mOnToggleViewTouchListener)
                    }
                }
            }
        }
    }


    fun attachToRecyclerView(recyclerView: RecyclerView) {
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    /**
     * Is there a toggle view which will trigger drag event.
     */
    open fun hasToggleView(): Boolean {
        return toggleViewId != NO_TOGGLE_VIEW
    }

    /**
     * Set the drag event should be trigger on long press.
     * Work when the toggleViewId has been set.
     *
     */
    open var isDragOnLongPressEnabled = true
        set(value) {
            field = value
            if (value) {
                mOnToggleViewTouchListener = null
                mOnToggleViewLongClickListener = OnLongClickListener { v ->
                    if (isDragEnabled) {
                        itemTouchHelper.startDrag(v.getTag(R.id.BaseQuickAdapter_viewholder_support) as ViewHolder)
                    }
                    true
                }
            } else {
                mOnToggleViewTouchListener = OnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN && !isDragOnLongPressEnabled) {
                        if (isDragEnabled) {
                            itemTouchHelper.startDrag(v.getTag(R.id.BaseQuickAdapter_viewholder_support) as ViewHolder)
                        }
                        true
                    } else {
                        false
                    }
                }
                mOnToggleViewLongClickListener = null
            }
        }


    protected fun getViewHolderPosition(viewHolder: ViewHolder): Int {
        return viewHolder.adapterPosition - baseQuickAdapter.headerLayoutCount
    }

    /************************* Drag *************************/

    open fun onItemDragStart(viewHolder: ViewHolder) {
        mOnItemDragListener?.onItemDragStart(viewHolder, getViewHolderPosition(viewHolder))
    }

    open fun onItemDragMoving(source: ViewHolder, target: ViewHolder) {
        val from = getViewHolderPosition(source)
        val to = getViewHolderPosition(target)
        if (inRange(from) && inRange(to)) {
            if (from < to) {
                for (i in from until to) {
                    Collections.swap(baseQuickAdapter.data, i, i + 1)
                }
            } else {
                for (i in from downTo to + 1) {
                    Collections.swap(baseQuickAdapter.data, i, i - 1)
                }
            }
            baseQuickAdapter.notifyItemMoved(source.adapterPosition, target.adapterPosition)
        }
        mOnItemDragListener?.onItemDragMoving(source, from, target, to)
    }

    open fun onItemAllowMove(viewHolder: ViewHolder): Boolean? {
        return mOnItemDragListener?.onItemAllowMove(viewHolder, getViewHolderPosition(viewHolder))
    }

    open fun onItemDragEnd(viewHolder: ViewHolder) {
        mOnItemDragListener?.onItemDragEnd(viewHolder, getViewHolderPosition(viewHolder))
    }

    open fun onItemDragMovePos(viewHolder: ViewHolder, dX:Float, dY:Float, actionState:Int, isCurrentlyActive:Boolean){
        mOnItemDragListener?.onItemDragMovePos(viewHolder,dX,dY,actionState,isCurrentlyActive)
    }

    /************************* Swipe *************************/

    open fun onItemSwipeStart(viewHolder: ViewHolder) {
        if (isSwipeEnabled) {
            mOnItemSwipeListener?.onItemSwipeStart(viewHolder, getViewHolderPosition(viewHolder))
        }
    }

    open fun onItemSwipeClear(viewHolder: ViewHolder) {
        if (isSwipeEnabled) {
            mOnItemSwipeListener?.clearView(viewHolder, getViewHolderPosition(viewHolder))
        }
    }

    open fun onItemSwiped(viewHolder: ViewHolder) {
        val pos = getViewHolderPosition(viewHolder)
        if (inRange(pos)) {
            baseQuickAdapter.data.removeAt(pos)
            baseQuickAdapter.notifyItemRemoved(viewHolder.adapterPosition)
            if (isSwipeEnabled) {
                mOnItemSwipeListener?.onItemSwiped(viewHolder, pos)
            }
        }
    }

    open fun onItemSwiping(canvas: Canvas?, viewHolder: ViewHolder?, dX: Float, dY: Float, isCurrentlyActive: Boolean) {
        if (isSwipeEnabled) {
            mOnItemSwipeListener?.onItemSwipeMoving(canvas, viewHolder, dX, dY, isCurrentlyActive)
        }
    }

    private fun inRange(position: Int): Boolean {
        return position >= 0 && position < baseQuickAdapter.data.size
    }

    /**
     * 设置监听
     * @param onItemDragListener OnItemDragListener?
     */
    override fun setOnItemDragListener(onItemDragListener: OnItemDragListener?) {
        this.mOnItemDragListener = onItemDragListener
    }

    override fun setOnItemSwipeListener(onItemSwipeListener: OnItemSwipeListener?) {
        this.mOnItemSwipeListener = onItemSwipeListener
    }

    companion object {
        private const val NO_TOGGLE_VIEW = 0
    }


}