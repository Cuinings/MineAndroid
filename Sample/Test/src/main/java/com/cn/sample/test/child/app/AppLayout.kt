package com.cn.sample.test.child.app

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.IntDef
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.withStyledAttributes
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.cn.sample.test.R

/**
 * @Author: CuiNing
 * @Time: 2025/12/2 11:01
 * @Description:
 */
class AppLayout: ConstraintLayout {

    companion object {
        val TAG = AppLayout::class.simpleName

        const val MAX_ROW_1: Int = 1
        const val MAX_ROW_2: Int = 2

        const val MAX_COLUMNS_3: Int = 3
        const val MAX_COLUMNS_6: Int = 6
    }
    @IntDef( MAX_ROW_1, MAX_ROW_2)
    @Retention(AnnotationRetention.SOURCE)
    annotation class AppLayoutRow
    @IntDef(MAX_COLUMNS_3, MAX_COLUMNS_6, )
    @Retention(AnnotationRetention.SOURCE)
    annotation class AppLayoutColumns

    @AppLayoutRow
    var maxRows = MAX_ROW_2
    @AppLayoutColumns
    var maxColumns = MAX_COLUMNS_3
    var itemMargin = 0
    var itemWidth = 8
    var itemHeight = 8
    var itemIconSize = 48
    var itemTextSize = 6f
    var itemCornerRadius = 6f

    var callback: AppLayoutCallback? = null

    private var itemValueCache = ArrayList<AppEntity>()

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    @SuppressLint("NewApi", "CustomViewStyleable", "UseKtx")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        isFocusable = false
        isFocusableInTouchMode = false
        clipChildren = false
        clipToOutline = false
        clipToPadding = false
        context.withStyledAttributes(attrs, R.styleable.AppLayout) {
            maxColumns = getInteger(R.styleable.AppLayout_maxColumns, maxColumns)
            maxRows = getInteger(R.styleable.AppLayout_maxRows, maxRows)
            itemMargin = getDimensionPixelSize(R.styleable.AppLayout_itemMargin, itemMargin)
            itemWidth = getDimensionPixelSize(R.styleable.AppLayout_itemWidth, itemWidth)
            itemHeight = getDimensionPixelSize(R.styleable.AppLayout_itemHeight, itemHeight)
            itemIconSize = getDimensionPixelSize(R.styleable.AppLayout_itemIconSize, itemIconSize)
            itemTextSize = getDimension(R.styleable.AppLayout_itemTextSize, itemTextSize)
            itemCornerRadius = getDimension(R.styleable.AppLayout_itemCornerRadius, itemCornerRadius)
        }
    }

    fun setMaxColumnsAndRows(@AppLayoutColumns columns: Int, @AppLayoutRow rows: Int) {
        this@AppLayout.maxColumns = columns
        this@AppLayout.maxRows = rows
        repeat(childCount) {
            updateItemLocation(it)
        }
    }

    fun setItems(entities: MutableList<AppEntity>) {
        removeAll()
        entities.forEach { addItem(it) }
    }

    fun addItem(entity: AppEntity) {
        childCount.takeIf {
            it < maxRows * maxColumns && !itemValueCache.contains(entity)
        }?.let {
            itemValueCache.add(entity)
            AppItemLayout(context).apply {
                id = entity.hashCode()
                tag = entity
                isFocusable = true
                layoutParams = LayoutParams(itemWidth, itemHeight)
                cornerRadius = itemCornerRadius
                setOnClickListener {
                    callback?.onCall(it.tag as AppEntity)
                }
                setIconWh(itemIconSize, itemIconSize)
                nameSize = itemTextSize
                setName(entity.name?:"")
                setIcon(R.drawable.ic_launcher_foreground)
            }.let {
                addView(it)
                updateItemLocation(itemValueCache.size - 1)
            }
        }
    }

    fun removeItem(entity: AppEntity) {
        itemValueCache.remove(entity)
        findViewById<AppItemLayout>(entity.hashCode()).let {
            removeView(it)
            repeat(childCount) {
                updateItemLocation(it)
            }
        }
        Log.d(TAG, "removeItem:$childCount, $entity")
    }

    fun removeAll() {
        itemValueCache.clear()
        removeAllViews()
    }

    private fun updateItemLocation(index: Int) {
        Log.d(TAG, "updateItemLocation: $index, ")
        ConstraintSet().apply {
            clone(this@AppLayout)
            getChildAt(index)?.let { view ->
                when(index) {
                    0 -> processIndex0Item(view, index)
                    1 -> processIndex1Item(view, index)
                    else -> when(index % 2 == 0) {
                        true -> processIndex2Item(view, index)
                        false -> processIndex3Item(view, index)
                    }
                }
            }
            TransitionManager.beginDelayedTransition(this@AppLayout, AutoTransition())
        }.applyTo(this@AppLayout)
    }

    private fun ConstraintSet.processIndex3Item(
        view: View,
        index: Int
    ) {
        when(maxRows) {
            MAX_ROW_1 -> {
                setMargin(view.id, ConstraintSet.START, itemMargin)
                setMargin(view.id, ConstraintSet.TOP, 0)
                setMargin(view.id, ConstraintSet.END, 0)
                setMargin(view.id, ConstraintSet.BOTTOM, 0)

                connect(view.id, ConstraintSet.START, itemValueCache[index - 1].hashCode(), ConstraintSet.END)
                connect(view.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            }
            MAX_ROW_2 -> {
                setMargin(view.id, ConstraintSet.START, 0)
                setMargin(view.id, ConstraintSet.TOP, itemMargin)
                setMargin(view.id, ConstraintSet.END, 0)
                setMargin(view.id, ConstraintSet.BOTTOM, 0)

                connect(view.id, ConstraintSet.START, itemValueCache[index - 1].hashCode(), ConstraintSet.START)
                connect(view.id, ConstraintSet.TOP, itemValueCache[index - 1].hashCode(), ConstraintSet.BOTTOM)
            }
        }
    }

    private fun ConstraintSet.processIndex2Item(
        view: View,
        index: Int
    ) {
        when(maxRows) {
            MAX_ROW_1 -> {
                setMargin(view.id, ConstraintSet.START, itemMargin)
                setMargin(view.id, ConstraintSet.TOP, 0)
                setMargin(view.id, ConstraintSet.END, 0)
                setMargin(view.id, ConstraintSet.BOTTOM, 0)

                connect(view.id, ConstraintSet.START, itemValueCache[index - 1].hashCode(), ConstraintSet.END)
                connect(view.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            }
            MAX_ROW_2 -> {
                setMargin(view.id, ConstraintSet.START, itemMargin)
                setMargin(view.id, ConstraintSet.TOP, 0)
                setMargin(view.id, ConstraintSet.END, 0)
                setMargin(view.id, ConstraintSet.BOTTOM, 0)

                connect(view.id, ConstraintSet.START, itemValueCache[index - 1].hashCode(), ConstraintSet.END)
                connect(view.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            }
        }
    }

    private fun ConstraintSet.processIndex1Item(
        view: View,
        index: Int
    ) {
        when(maxRows) {
            MAX_ROW_1 -> {
                setMargin(view.id, ConstraintSet.START, itemMargin)
                setMargin(view.id, ConstraintSet.TOP, 0)
                setMargin(view.id, ConstraintSet.END, 0)
                setMargin(view.id, ConstraintSet.BOTTOM, 0)

                connect(view.id, ConstraintSet.START, itemValueCache[index - 1].hashCode(), ConstraintSet.END)
                connect(view.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            }
            MAX_ROW_2 -> {
                setMargin(view.id, ConstraintSet.START, 0)
                setMargin(view.id, ConstraintSet.TOP, itemMargin)
                setMargin(view.id, ConstraintSet.END, 0)
                setMargin(view.id, ConstraintSet.BOTTOM, 0)

                connect(view.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(view.id, ConstraintSet.TOP, itemValueCache[index - 1].hashCode(), ConstraintSet.BOTTOM)
            }
            else -> {
            }
        }
    }

    private fun ConstraintSet.processIndex0Item(
        view: View,
        index: Int
    ) {
        setMargin(view.id, ConstraintSet.START, 0)
        setMargin(view.id, ConstraintSet.TOP, 0)
        setMargin(view.id, ConstraintSet.END, 0)
        setMargin(view.id, ConstraintSet.BOTTOM, 0)

        connect(view.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(view.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
    }

    interface AppLayoutCallback {
        fun onCall(entity: AppEntity)
    }

}




