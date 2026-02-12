package com.cn.board.meet.home

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.cn.board.database.AppInfo
import com.cn.core.resources.ext.asInt
import com.cn.core.utils.TimeUtil.timestampToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: cn
 * @time: 2026/2/9 17:11
 * @history
 * @description:
 */
class AppList: RecyclerView {

    private lateinit var appAdapter: AppAdapter
    private var spanCount = 4 // 默认网格列数
//    private var selectedBackgroundColor = "#E0E0E0".toColorInt() // 默认选中背景色
//    private var normalBackgroundColor = android.graphics.Color.WHITE // 默认正常背景色
    private var currentAppList: List<AppInfo> = emptyList()

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        initAppList()
    }

    fun setApps(appList: List<AppInfo>) {
        // 更新应用列表
        currentAppList = appList
        appAdapter.submitList(appList)
    }

    fun setSelectedApp(appInfo: AppInfo?) {
        // 更新选中的应用
        appAdapter.setSelectedApp(appInfo)
    }

    /**
     * 设置网格列数
     * @param count 列数
     */
    fun setSpanCount(count: Int) {
        if (count > 0) {
            spanCount = count
            (layoutManager as? GridLayoutManager)?.spanCount = count
        }
    }

    /**
     * 设置应用点击监听器
     * @param listener 点击监听器
     */
    fun setAppClickListener(listener: AppAdapter.AppClickListener?) {
        appAdapter.setAppClickListener(listener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清空应用列表，帮助垃圾回收
        currentAppList = emptyList()
        // 清空适配器资源
        appAdapter.clear()
        Log.d("AppList", "清理资源")
    }

    /**
     * 清理资源，避免内存泄漏
     */
    fun clear() {
        // 清空应用列表
        currentAppList = emptyList()
        // 清空适配器资源
        appAdapter.clear()
        Log.d("AppList", "AppList资源已清理")
    }

    private fun initAppList() {
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        appAdapter = AppAdapter()
        adapter = appAdapter
        layoutManager = GridLayoutManager(context, spanCount)
        addItemDecoration(AppItemDecoration())

        // 设置拖拽排序监听器
        appAdapter.setOnItemMoveListener(object : AppAdapter.OnItemMoveListener {
            override fun onItemMove(fromPosition: Int, toPosition: Int) {
                Log.d("AppList", "项目移动: $fromPosition -> $toPosition")
            }

            override fun onSortCompleted(sortedList: List<AppInfo>) {
                Log.d("AppList", "排序完成，应用数量: ${sortedList.size}")
                // 更新应用排序顺序
                AppUtil.updateAppSortOrder(sortedList)
            }
        })

        // 添加拖拽排序功能
        setupItemTouchHelper()
    }

    /**
     * 设置ItemTouchHelper，支持拖拽排序
     */
    private fun setupItemTouchHelper() {
        val callback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                // 支持上下左右拖拽
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                return makeMovementFlags(dragFlags, 0)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                if (fromPosition < 0 || toPosition < 0) {
                    return false
                }
                return appAdapter.onItemMove(fromPosition, toPosition)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 不支持滑动删除
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                // 当拖拽开始时，可以添加视觉效果
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // 当拖拽结束时，恢复视觉效果
                viewHolder.itemView.alpha = 1.0f
                // 通知排序完成
                appAdapter.onSortCompleted()
            }
        }

        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(this)
    }

    /**
     * 设置拖拽排序监听器
     * @param listener 监听器
     */
    fun setOnItemMoveListener(listener: AppAdapter.OnItemMoveListener?) {
        appAdapter.setOnItemMoveListener(listener)
    }

    /**
     * 设置应用排序监听器
     * @param listener 排序完成时的回调
     */
    fun setAppSortListener(listener: (List<AppInfo>) -> Unit) {
        appAdapter.setOnItemMoveListener(object : AppAdapter.OnItemMoveListener {
            override fun onItemMove(fromPosition: Int, toPosition: Int) {
                Log.d("AppList", "项目移动: $fromPosition -> $toPosition")
            }

            override fun onSortCompleted(sortedList: List<AppInfo>) {
                Log.d("AppList", "排序完成，应用数量: ${sortedList.size}")
                // 回调给监听器
                listener(sortedList)
                // 更新应用排序顺序
                AppUtil.updateAppSortOrder(sortedList)
            }
        })
    }

    class AppAdapter() : Adapter<AppAdapter.AppViewHolder>() {

        private var appList: MutableList<AppInfo> = mutableListOf()
        private var selectedApp: AppInfo? = null
        private var appClickListener: AppClickListener? = null
        private var onItemMoveListener: OnItemMoveListener? = null

        /**
         * 拖拽排序监听器接口
         */
        interface OnItemMoveListener {
            /**
             * 项目移动时回调
             * @param fromPosition 起始位置
             * @param toPosition 目标位置
             */
            fun onItemMove(fromPosition: Int, toPosition: Int)

            /**
             * 排序完成时回调
             * @param sortedList 排序后的列表
             */
            fun onSortCompleted(sortedList: List<AppInfo>)
        }

        /**
         * 设置拖拽排序监听器
         * @param listener 监听器
         */
        fun setOnItemMoveListener(listener: OnItemMoveListener?) {
            onItemMoveListener = listener
        }

        /**
         * 处理项目移动
         * @param fromPosition 起始位置
         * @param toPosition 目标位置
         * @return 是否移动成功
         */
        fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    appList[i] = appList.set(i + 1, appList[i])
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    appList[i] = appList.set(i - 1, appList[i])
                }
            }
            notifyItemMoved(fromPosition, toPosition)
            onItemMoveListener?.onItemMove(fromPosition, toPosition)
            return true
        }

        /**
         * 排序完成
         */
        fun onSortCompleted() {
            onItemMoveListener?.onSortCompleted(appList)
        }

        /**
         * 设置应用点击监听器
         * @param listener 点击监听器
         */
        fun setAppClickListener(listener: AppClickListener?) {
            appClickListener = listener
        }

        /**
         * 清理资源，避免内存泄漏
         */
        fun clear() {
            appClickListener = null
            selectedApp = null
            // 清空应用列表，帮助垃圾回收
            appList = mutableListOf()
            Log.d("AppList", "AppAdapter资源已清理")
        }

        /**
         * 应用点击监听器接口
         */
        interface AppClickListener {
            /**
             * 应用被点击
             * @param appInfo 被点击的应用信息
             */
            fun onAppClick(appInfo: AppInfo)
        }

        fun submitList(list: List<AppInfo>) {
            val oldList = appList
            appList = list.toMutableList()

            // 使用DiffUtil计算差异并更新列表
            val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldList.size
                override fun getNewListSize(): Int = appList.size

                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                    val oldItem = oldList[oldItemPosition]
                    val newItem = appList[newItemPosition]

                    // 检查哪些字段发生了变化
                    val changes = mutableMapOf<String, Any>()

                    if (oldItem.isSystemApp != newItem.isSystemApp) {
                        changes["isSystemApp"] = newItem.isSystemApp
                    }

                    if (oldItem.lastUsedTime != newItem.lastUsedTime) {
                        changes["lastUsedTime"] = newItem.lastUsedTime
                    }

                    // 如果有变更，返回变更内容，否则返回null
                    return if (changes.isNotEmpty()) changes else null
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldList[oldItemPosition]
                    val newItem = appList[newItemPosition]
                    return oldItem.packageName == newItem.packageName // 使用packageName作为唯一标识符
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldList[oldItemPosition]
                    val newItem = appList[newItemPosition]
                    return oldItem == newItem // 使用data class的equals方法
                }
            })

            diffResult.dispatchUpdatesTo(this)
        }

        fun setSelectedApp(appInfo: AppInfo?) {
            val oldSelectedApp = selectedApp
            selectedApp = appInfo

            // 只更新受影响的项
            if (oldSelectedApp != null) {
                val oldPosition = appList.indexOf(oldSelectedApp)
                if (oldPosition != -1) {
                    notifyItemChanged(oldPosition)
                }
            }

            if (appInfo != null) {
                val newPosition = appList.indexOf(appInfo)
                if (newPosition != -1) {
                    notifyItemChanged(newPosition)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return AppViewHolder(view,appClickListener)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val appInfo = appList[position]
            val isSelected = appInfo == selectedApp
            holder.bind(appInfo, isSelected)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) {
                // 如果没有payload，调用默认的绑定方法
                super.onBindViewHolder(holder, position, payloads)
            } else {
                // 使用payloads进行部分更新
                val appInfo = appList[position]
                val isSelected = appInfo == selectedApp

                // 处理每个payload
                for (payload in payloads) {
                    if (payload is Map<*, *>) {
                        val stringKeyMap = payload.filterKeys { it is String }
                            .mapKeys { it.key as String }
                            .mapValues { it.value as Any }
                        holder.bindPartial(appInfo, isSelected, stringKeyMap)
                    }
                }
            }
        }

        override fun getItemCount(): Int = appList.size

        override fun getItemId(position: Int): Long {
            return appList[position].id.toLong()
        }

        override fun onViewRecycled(holder: AppViewHolder) {
            super.onViewRecycled(holder)
            // 取消所有协程，避免内存泄漏
            holder.clear()
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            super.onDetachedFromRecyclerView(recyclerView)
            // 清理资源，避免内存泄漏
            clear()
        }

        class AppViewHolder(
            itemView: View,
            private val appClickListener: AppClickListener?
        ) : ViewHolder(itemView), CoroutineScope {
            private val job = Job()
            override val coroutineContext = job + Dispatchers.Main

            private val icon = itemView.findViewById<View>(R.id.app_icon)
            private val name = itemView.findViewById<android.widget.TextView>(R.id.app_name)
            private val lastUsed = itemView.findViewById<android.widget.TextView>(R.id.app_last_used_time)
            private val system = itemView.findViewById<android.widget.TextView>(R.id.app_system)

            init {
                // 设置点击事件
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val appInfo = (itemView.tag as? AppInfo)
                        appInfo?.let {info ->
                            appClickListener?.onAppClick(info)
                        }
                    }
                }
            }

            fun bind(appInfo: AppInfo, isSelected: Boolean = false) {
                // 设置应用信息为tag，用于点击事件
                itemView.tag = appInfo
                // 更新系统应用标识
                updateSystemAppIndicator(appInfo.isSystemApp)
                // 更新选中状态
                updateSelectedState(isSelected)
                //加载应用图标和名称
                loadAppInfoAsync(appInfo)
            }

            fun bindPartial(appInfo: AppInfo, isSelected: Boolean = false, payload: Map<String, Any>) {
                // 只更新payload中指定的字段
                for ((key, value) in payload) {
                    when (key) {
                        "name" -> {
                            // 直接显示，不显示"加载中..."
                            name.text = value as String
                        }
                        "iconRes" -> {
                            // 直接显示，不显示占位符
                            (icon as android.widget.ImageView).setImageDrawable(value as android.graphics.drawable.Drawable)
                        }
                        "isSystemApp" -> {
                            val isSystem = value as Boolean
                            updateSystemAppIndicator(isSystem)
                        }
                        "lastUsedTime" -> {
                            lastUsed.text = timestampToString(value as Long)
                        }
                    }
                }
                // 最终更新选中状态，因为它可能会变化
                updateSelectedState(isSelected)
            }
            
            /**
             * 更新系统应用标识
             * @param isSystemApp 是否为系统应用
             */
            private fun updateSystemAppIndicator(isSystemApp: Boolean) {
                if (isSystemApp) {
                    system.visibility = View.VISIBLE
                } else {
                    system.visibility = View.GONE
                }
            }
            
            /**
             * 更新选中状态
             * @param isSelected 是否选中
             */
            private fun updateSelectedState(isSelected: Boolean) {

            }
            
            private fun loadAppInfoAsync(appInfo: AppInfo) {
                launch {
                    Log.d("AppList", "开始异步加载应用信息: ${appInfo.packageName}")
                    // 异步加载应用图标
                    val appIcon = try {
                        AppUtil.getAppIconAsync(itemView.context, appInfo.packageName)
                    } catch (e: Exception) {
                        Log.e("AppList", "加载应用图标失败: ${appInfo.packageName}", e)
                        null
                    }
                    
                    // 异步加载应用名称
                    val appName = try {
                        AppUtil.getAppNameAsync(itemView.context, appInfo.packageName)
                    } catch (e: Exception) {
                        Log.e("AppList", "加载应用名称失败: ${appInfo.packageName}", e)
                        "Unknown"
                    }
                    
                    // 在主线程更新UI
                    withContext(Dispatchers.Main) {
                        try {
                            // 缓存加载的结果
                            (icon as android.widget.ImageView).setImageDrawable(appIcon ?: itemView.context.getDrawable(R.mipmap.ic_launcher))
                            name.text = appName
                            Log.d("AppList", "应用信息加载完成: ${appInfo.packageName} -> $appName")
                        } catch (e: Exception) {
                            Log.e("AppList", "更新UI失败: ${appInfo.packageName}", e)
                        }
                    }
                }
            }
            
            fun clear() {
                // 取消所有协程，避免内存泄漏
                Log.d("AppList", "取消ViewHolder协程: ${adapterPosition}")
                cancel()
                // 清除引用，帮助垃圾回收
                itemView.tag = null
                // 不清除缓存字段，因为它们应该在 ViewHolder 生命周期内保留
                // 这样可以避免滚动时重复加载应用信息
            }
        }
    }

    // 设置item间距的ItemDecoration
    inner class AppItemDecoration : ItemDecoration() {

        private val spacing by lazy { R.dimen.dp2.asInt(resources) }

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: State
        ) {
            outRect.top = 0
            outRect.left = 0
            outRect.right = spacing
            outRect.bottom = spacing
        }
    }
}