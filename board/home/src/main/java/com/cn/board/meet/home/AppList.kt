package com.cn.board.meet.home

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cn.board.database.AppInfo
import com.cn.core.resources.ext.asInt
import androidx.core.graphics.toColorInt
import com.cn.core.utils.TimeUtil
import com.cn.core.utils.TimeUtil.FORMAT_YYYY_MM_DD
import com.cn.core.utils.TimeUtil.timestampToString

/**
 * @author: cn
 * @time: 2026/2/9 17:11
 * @history
 * @description:
 */
class AppList: RecyclerView {

    private lateinit var appAdapter: AppAdapter

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) { initAppList() }

    fun setApps(appList: List<AppInfo>) {
        // 更新应用列表
        appAdapter.submitList(appList)
    }

    fun setSelectedApp(appInfo: AppInfo?) {
        // 更新选中的应用
        appAdapter.setSelectedApp(appInfo)
    }


    private fun initAppList() {
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        appAdapter = AppAdapter()
        adapter = appAdapter
        layoutManager = GridLayoutManager(context, 4)
        addItemDecoration(AppItemDecoration())
    }

    class AppAdapter : Adapter<AppAdapter.AppViewHolder>() {

        private var appList: List<AppInfo> = emptyList()
        private var selectedApp: AppInfo? = null

        fun submitList(list: List<AppInfo>) {
            val oldList = appList
            appList = list
            
            // 使用DiffUtil计算差异并更新列表
            val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldList.size
                override fun getNewListSize(): Int = list.size

                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                    val oldItem = oldList[oldItemPosition]
                    val newItem = list[newItemPosition]
                    
                    // 检查哪些字段发生了变化
                    val changes = mutableMapOf<String, Any>()

                    /*if (oldItem.name != newItem.name) {
                        changes["name"] = newItem.name
                    }

                    if (oldItem.iconRes != newItem.iconRes) {
                        changes["iconRes"] = newItem.iconRes
                    }*/

                    if (oldItem.isSystemApp != newItem.isSystemApp) {
                        changes["isSystemApp"] = newItem.isSystemApp
                    }
                    
                    if (oldItem.lastUsedTime != newItem.lastUsedTime) {
                        changes["lastUsedTime"] = newItem.lastUsedTime
                    }

                    /*if (oldItem.usageCount != newItem.usageCount) {
                        changes["usageCount"] = newItem.usageCount
                    }*/

                    // 如果有变更，返回变更内容，否则返回null
                    return if (changes.isNotEmpty()) changes else null
                }
                
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldList[oldItemPosition]
                    val newItem = list[newItemPosition]
                    return oldItem.packageName == newItem.packageName // 使用name作为唯一标识符
                }
                
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldList[oldItemPosition]
                    val newItem = list[newItemPosition]
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
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return AppViewHolder(view)
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
                        holder.bindPartial(appInfo, isSelected, payload as Map<String, Any>)
                    }
                }
            }
        }

        override fun getItemCount(): Int = appList.size

        class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon = itemView.findViewById<View>(R.id.app_icon)
            private val name = itemView.findViewById<android.widget.TextView>(R.id.app_name)
            private val lastUsed = itemView.findViewById<android.widget.TextView>(R.id.app_last_used_time)
            private val system = itemView.findViewById<android.widget.TextView>(R.id.app_system)

            fun bind(appInfo: AppInfo, isSelected: Boolean = false) {
                // 尝试加载应用的实际图标（使用同步版本，避免主线程阻塞）
                try {
                    val appIcon = AppUtil.getAppIcon(itemView.context, appInfo.packageName)
                    (icon as android.widget.ImageView).setImageDrawable(appIcon)
                } catch (e: Exception) {
                    // 发生异常时，使用默认图标
                    (icon as android.widget.ImageView).setImageResource(R.mipmap.ic_launcher)
                }
                try {
                    val appName = AppUtil.getAppName(itemView.context, appInfo.packageName)
                    name.text = appName
                } catch (e: Exception) {
                    // 发生异常时，使用默认图标
                    name.text = "Unknown"
                }
                lastUsed.text = timestampToString(appInfo.lastUsedTime)
                // 为系统应用添加标识
                if (appInfo.isSystemApp) {
                    system.visibility = View.VISIBLE
//                    name.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.star_big_on, 0)
//                    name.compoundDrawablePadding = 4
                } else {
                    system.visibility = View.GONE
//                    name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
                
                // 更新选中状态的UI
                itemView.setBackgroundColor(
                    if (isSelected) {
                        "#E0E0E0".toColorInt() // 选中状态
                    } else {
                        android.graphics.Color.WHITE // 未选中状态
                    }
                )
            }

            fun bindPartial(appInfo: AppInfo, isSelected: Boolean = false, payload: Map<String, Any>) {
                // 只更新payload中指定的字段
                for ((key, value) in payload) {
                    when (key) {
                        "name" -> {
                            try {
                                val appName = AppUtil.getAppName(itemView.context, appInfo.packageName)
                                name.text = appName
                            } catch (e: Exception) {
                                // 发生异常时，使用默认图标
                                name.text = "Unknown"
                            }
                        }
                        "iconRes" -> {
                            // 尝试加载应用的实际图标（使用同步版本，避免主线程阻塞）
                            try {
                                val appIcon = AppUtil.getAppIcon(itemView.context, appInfo.packageName)
                                (icon as android.widget.ImageView).setImageDrawable(appIcon)
                            } catch (e: Exception) {
                                // 发生异常时，使用默认图标
                                (icon as android.widget.ImageView).setImageResource(R.mipmap.ic_launcher)
                            }
                        }
                        "isSystemApp" -> {
                            val isSystem = value as Boolean
                            if (isSystem) {
                                name.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.star_big_on, 0)
                                name.compoundDrawablePadding = 4
                            } else {
                                name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                            }
                        }
                        // lastUsedTime和usageCount字段在UI上没有直接显示，所以不需要更新
                    }
                }

                // 始终更新选中状态，因为它可能会变化
                itemView.setBackgroundColor(
                    if (isSelected) {
                        "#E0E0E0".toColorInt() // 选中状态
                    } else {
                        android.graphics.Color.TRANSPARENT // 未选中状态
                    }
                )
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