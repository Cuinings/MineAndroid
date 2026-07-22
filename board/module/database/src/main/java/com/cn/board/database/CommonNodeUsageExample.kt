//package com.cn.board.database
//
//import android.app.Application
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.repeatOnLifecycle
//import androidx.lifecycle.viewModelScope
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.flow.flatMapLatest
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.launch
//
////╔══════════════════════════════════════════════════════════════════════════╗
////║  第一步：实现远程数据源                                                  ║
////╟──────────────────────────────────────────────────────────────────────────╢
////║  这是唯一需要你实现的接口。将各类型通知的部门/用户数据转成 CommonNode。     ║
////╚══════════════════════════════════════════════════════════════════════════╝
//
//class MyRemoteDataSource : CommonNodeRemoteDataSource {
//
//    override suspend fun fetchNodes(resourceType: Int): List<CommonNode> {
//        // 伪代码：根据类型调用不同 API，这里用模拟数据演示
//        val deptList = when (resourceType) {
//            1 -> listOf(
//                Triple("",   "dept_root",  "总公司"),
//                Triple("dept_root", "dept_a", "研发部"),
//                Triple("dept_root", "dept_b", "市场部"),
//            )
//            2 -> listOf(
//                Triple("",   "group_x", "项目组X"),
//                Triple("group_x", "group_y", "项目组Y"),
//            )
//            else -> emptyList()
//        }
//        val userList = when (resourceType) {
//            1 -> listOf("user_1" to "dept_a", "user_2" to "dept_a", "user_3" to "dept_b")
//            2 -> listOf("user_4" to "group_x", "user_5" to "group_y")
//            else -> emptyList()
//        }
//
//        // — 构造 GROUP 节点（部门）—
//        val deptNodes = deptList.map { (parentId, deptId, name) ->
//            CommonNode(
//                id = CommonNode.groupId(resourceType, deptId),  // "1_DEPT_dept_a"
//                nodeType = NodeType.GROUP,
//                resourceType = resourceType,
//                parentId = CommonNode.groupId(resourceType, parentId).takeIf { parentId.isNotEmpty() },
//                deptId = deptId,
//                name = name,
//                userId = null
//            )
//        }
//
//        // — 构造 MEMBER 节点（用户），parentId 指向所属部门 —
//        val memberNodes = userList.map { (userId, deptId) ->
//            CommonNode(
//                id = CommonNode.memberId(resourceType, userId),  // "1_USER_user_1"
//                nodeType = NodeType.MEMBER,
//                resourceType = resourceType,
//                parentId = CommonNode.groupId(resourceType, deptId), // 挂到部门下
//                deptId = deptId,    // 归属哪个部门
//                name = "用户-$userId",
//                userId = userId
//            )
//        }
//        return deptNodes + memberNodes
//    }
//}
//
//
////╔══════════════════════════════════════════════════════════════════════════╗
////║  第二步：初始化 Repository（Application 级别单例）                       ║
////╚══════════════════════════════════════════════════════════════════════════╝
//
//object RepoProvider {
//
//    private var instance: CommonNodeRepository? = null
//
//    fun init(app: Application) {
//        DatabaseManager.initDatabase(app)
//        instance = CommonNodeRepository(
//            dao = DatabaseManager.getCommonNodeDao(),
//            remote = MyRemoteDataSource()
//        )
//    }
//
//    fun get(): CommonNodeRepository = instance
//        ?: throw IllegalStateException("先调用 RepoProvider.init()")
//}
//
//
////╔══════════════════════════════════════════════════════════════════════════╗
////║  第三步：ViewModel — Flow 驱动 UI                                       ║
////╚══════════════════════════════════════════════════════════════════════════╝
//
//data class OrgUiState(
//    /** 部门 + 用户的扁平列表（RecyclerView 直接使用） */
//    val nodes: List<CommonNode> = emptyList(),
//    /** 部门ID → 成员数，用于部门行右侧角标 */
//    val deptCounts: Map<String, Int> = emptyMap(),
//    /** 当前选中的资源类型 */
//    val currentType: Int = 1,
//    val isLoading: Boolean = false
//)
//
//class OrgViewModel(app: Application) : AndroidViewModel(app) {
//
//    private val repo = RepoProvider.get()
//
//    private val _currentType = MutableStateFlow(1)
//    val currentType: StateFlow<Int> = _currentType.asStateFlow()
//
//    private val _isLoading = MutableStateFlow(false)
//
//    /** nodes 和 deptCounts 都跟随 currentType 自动切换 */
//    val uiState: StateFlow<OrgUiState> = combine(
//        // Flow1: 节点列表
//        _currentType.flatMapLatest { type -> repo.observeByResourceType(type) },
//        // Flow2: 部门计数
//        _currentType.flatMapLatest { type -> repo.observeDeptMemberCounts(type) },
//        // Flow3: 加载状态
//        _isLoading,
//        // Flow4: 当前类型
//        _currentType
//    ) { nodes, counts, loading, type ->
//        OrgUiState(nodes, counts, type, loading)
//    }.stateIn(
//        scope = viewModelScope,
//        started = SharingStarted.WhileSubscribed(5000),
//        initialValue = OrgUiState()
//    )
//
//    // ── Actions ──
//
//    /** 打开某个资源类型，本地优先 */
//    fun loadType(type: Int) {
//        _currentType.value = type
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                repo.loadIfNeeded(type)   // Room有数据→直接返回，无数据→请求网络
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    /** 强制刷新 */
//    fun refresh() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try { repo.refresh(currentType.value) }
//            finally { _isLoading.value = false }
//        }
//    }
//
//    /** 处理推送 CRUD 通知 */
//    fun onCrudNotification(node: CommonNode, op: CrudOperation) {
//        viewModelScope.launch { repo.handleNotification(node, op) }
//    }
//
//    // ── 树形展开用 ──
//    fun childrenOf(parentId: String): Flow<List<CommonNode>> = repo.observeChildren(parentId)
//
//    // ── 并发统计多个部门（一次性查询） ──
//    fun countDeptsConcurrent(deptIds: List<String>) {
//        viewModelScope.launch {
//            val results = repo.getDeptMemberCountsConcurrent(deptIds)
//            Log.d("OrgVM", "并发统计: $results")
//        }
//    }
//}
//
//
////╔══════════════════════════════════════════════════════════════════════════╗
////║  第四步：Fragment — 观察 StateFlow，驱动 RecyclerView                   ║
////╚══════════════════════════════════════════════════════════════════════════╝
//
//class OrgFragment : Fragment() {
//
//    private val vm: OrgViewModel by viewModels {
//        ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
//    }
//
//    private val adapter = OrgAdapter()
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // RecyclerView 设置
//        // binding.recycler.layoutManager = LinearLayoutManager(requireContext())
//        // binding.recycler.adapter = adapter
//
//        // — 关键：生命周期安全地收集 StateFlow —
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                vm.uiState.collect { state ->
//                    adapter.submitList(state.nodes)
//                    // 可选：更新部门角标
//                    // adapter.updateDeptCounts(state.deptCounts)
//                }
//            }
//        }
//
//        // 初始化加载类型1
//        vm.loadType(1)
//
//        // 切换到类型2的按钮
//        // binding.btnType2.setOnClickListener { vm.loadType(2) }
//        // binding.btnType3.setOnClickListener { vm.loadType(3) }
//        // binding.swipeRefresh.setOnRefreshListener { vm.refresh() }
//    }
//}
//
//
////╔══════════════════════════════════════════════════════════════════════════╗
////║  第五步：RecyclerView Adapter — 分行分组和成员                          ║
////╚══════════════════════════════════════════════════════════════════════════╝
//
//class OrgAdapter : ListAdapter<CommonNode, RecyclerView.ViewHolder>(OrgDiffCallback()) {
//
//    companion object {
//        const val VIEW_TYPE_GROUP = 0
//        const val VIEW_TYPE_MEMBER = 1
//    }
//
//    override fun getItemViewType(position: Int): Int = when (getItem(position).nodeType) {
//        NodeType.GROUP -> VIEW_TYPE_GROUP
//        NodeType.MEMBER -> VIEW_TYPE_MEMBER
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        return when (viewType) {
//            VIEW_TYPE_GROUP -> GroupViewHolder(parent)
//            VIEW_TYPE_MEMBER -> MemberViewHolder(parent)
//            else -> throw IllegalArgumentException()
//        }
//    }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val node = getItem(position)
//        when (holder) {
//            is GroupViewHolder -> holder.bind(node)
//            is MemberViewHolder -> holder.bind(node)
//        }
//    }
//
//    // 简化的 ViewHolder（实际实现根据你的布局调整）
//    class GroupViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(plainView(parent)) {
//        fun bind(node: CommonNode) { /* 显示部门名 + 展开/折叠箭头 */ }
//    }
//    class MemberViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(plainView(parent)) {
//        fun bind(node: CommonNode) { /* 显示用户名 */ }
//    }
//
//    private fun plainView(parent: ViewGroup): View =
//        LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
//}
//
//class OrgDiffCallback : DiffUtil.ItemCallback<CommonNode>() {
//    override fun areItemsTheSame(old: CommonNode, new: CommonNode) = old.id == new.id
//    override fun areContentsTheSame(old: CommonNode, new: CommonNode) = old == new
//}
//
//
////╔══════════════════════════════════════════════════════════════════════════╗
////║  第六步：CRUD 通知处理 — 来自推送/WebSocket 等                          ║
////╚══════════════════════════════════════════════════════════════════════════╝
//
///**
// * 通知处理示例 — 在推送接收器中调用 ViewModel.onCrudNotification()
// *
// * 当收到类型1的新增通知:
// *   { type: 1, op: "CREATE", parentId: "dept_root", deptId: "dept_c", name: "设计部" }
// *
// * 转为 CommonNode:
// *   CommonNode(
// *     id = CommonNode.groupId(1, "dept_c"),
// *     nodeType = NodeType.GROUP,
// *     resourceType = 1,
// *     parentId = CommonNode.groupId(1, "dept_root"),
// *     deptId = "dept_c",
// *     name = "设计部",
// *     userId = null
// *   )
// *
// * 然后:
// *   viewModel.onCrudNotification(node, CrudOperation.CREATE)
// *
// * Room Flow 自动发射 → StateFlow 更新 → RecyclerView 自动刷新（DiffUtil 局部更新）
// */
//fun handleNotificationExample(vm: OrgViewModel, notification: Map<String, Any>) {
//    val type = notification["type"] as Int
//    val opStr = notification["op"] as String
//    val op = CrudOperation.valueOf(opStr)
//
//    val node = CommonNode(
//        id = CommonNode.groupId(type, notification["deptId"] as String),
//        nodeType = NodeType.GROUP,
//        resourceType = type,
//        parentId = (notification["parentId"] as? String)?.let {
//            CommonNode.groupId(type, it)
//        },
//        deptId = notification["deptId"] as String,
//        name = notification["name"] as String,
//        userId = null
//    )
//
//    vm.onCrudNotification(node, op)
//}
//
//
////╔══════════════════════════════════════════════════════════════════════════╗
////║  第七步：Application 初始化                                             ║
////╚══════════════════════════════════════════════════════════════════════════╝
//
//class ExampleApplication : Application() {
//    override fun onCreate() {
//        super.onCreate()
//        RepoProvider.init(this)
//    }
//}
