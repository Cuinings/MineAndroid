//package com.cn.board.database
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.flatMapLatest
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.launch
//
///**
// * CommonNode ViewModel — Flow 驱动 UI + 并发统计。
// *
// * 核心链路:
// *   1. resourceType 变化 → flatMapLatest → Room Flow → StateFlow → UI
// *   2. loadIfNeeded() → Room 有数据直接返回，无数据自动请求网络
// *   3. 部门成员计数 — Flow 跟随 resourceType 自动更新
// *
// * 使用:
// *   viewModel.nodes.collect { adapter.submitList(it) }
// *   viewModel.deptCounts.collect { map -> badgeView.updateCounts(map) }
// */
//class CommonNodeViewModel(
//    private val repository: CommonNodeRepository
//) : ViewModel() {
//
//    private val _currentResourceType = MutableStateFlow(ResourceType.TYPE_1.code)
//    val currentResourceType: StateFlow<Int> = _currentResourceType.asStateFlow()
//
//    // ═══════════════════════════════════════════
//    // Flow 驱动 — flatMapLatest 响应资源类型切换
//    // ═══════════════════════════════════════════
//
//    /** 当前类型的所有节点（GROUP + MEMBER），自动跟随 resourceType 切换 */
//    val nodes: StateFlow<List<CommonNode>> = _currentResourceType
//        .flatMapLatest { resourceType ->
//            repository.observeByResourceType(resourceType)
//        }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
//
//    /** 部门成员计数，自动跟随 resourceType 切换 */
//    val deptCounts: StateFlow<Map<String, Int>> = _currentResourceType
//        .flatMapLatest { resourceType ->
//            repository.observeDeptMemberCounts(resourceType)
//        }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
//
//    private val _isLoading = MutableStateFlow(false)
//    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
//
//    // ═══════════════════════════════════════════
//    // Action
//    // ═══════════════════════════════════════════
//
//    /** 切换资源类型 — 本地优先，Room 有数据直接返回 Flow */
//    fun loadData(resourceType: Int) {
//        _currentResourceType.value = resourceType
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                repository.loadIfNeeded(resourceType)
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    /** 强制从网络刷新 */
//    fun refresh() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                repository.refresh(_currentResourceType.value)
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    /** CRUD 通知处理 */
//    fun handleNotification(node: CommonNode, operation: CrudOperation) {
//        viewModelScope.launch {
//            repository.handleNotification(node, operation)
//        }
//    }
//
//    fun handleNotifications(
//        resourceType: Int,
//        ops: List<Pair<CommonNode, CrudOperation>>
//    ) {
//        viewModelScope.launch {
//            repository.handleNotifications(resourceType, ops)
//        }
//    }
//
//    // ═══════════════════════════════════════════
//    // 树形展开
//    // ═══════════════════════════════════════════
//
//    fun childrenOf(parentId: String): Flow<List<CommonNode>> =
//        repository.observeChildren(parentId)
//
//    fun rootDepts(resourceType: Int): Flow<List<CommonNode>> =
//        repository.observeRootDepts(resourceType)
//}
