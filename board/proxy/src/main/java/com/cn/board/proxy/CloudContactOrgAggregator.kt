//package com.cn.board.proxy
//
//import android.annotation.SuppressLint
//import com.tp.business.bean.callback.im.MTWbParseKedaDeptListSecBean
//import com.tp.business.bean.callback.im.MTWbParseKedaDeptSec
//import com.tp.business.bean.callback.im.MTWbParse_Keda_User
//import com.tp.business.bean.callback.im.MTWbParse_Keda_UserList_Bean
//import com.tp.business.bean.callback.im.UserDomain
//import com.tp.business.bean.callback.im.UserDomainEntity
//import com.tp.conference.app.iac
//import com.tp.conference.common.CommonNode
//import com.tp.conference.common.NodeErr
//import com.tp.conference.common.NodeResourceType
//import com.tp.conference.common.NodeType
//import com.tp.conference.confcontrol.aggregator.callback.CloudOrgAggregatorCallback
//import com.tp.conference.confcontrol.contract.bean.GroupNode
//import com.tp.conference.confcontrol.state.State.checkIacBinder
//import com.tp.conference.util.PinYinUtil
//import com.tp.log.TPLog
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import kotlinx.coroutines.withContext
//import kotlinx.coroutines.yield
//import java.util.Collections
//import java.util.concurrent.ConcurrentHashMap
//import java.util.concurrent.ConcurrentLinkedDeque
//import java.util.concurrent.atomic.AtomicBoolean
//import java.util.concurrent.atomic.AtomicInteger
//import java.util.concurrent.atomic.AtomicReference
//import java.util.*
//
///**
// * @author: cn
// * @time: 2026/6/16 16:17
// * @description: 云端组织架构聚合器
// */
//@SuppressLint("NewApi")
//class CloudContactOrgAggregator(
//    val callback: CloudOrgAggregatorCallback? = null
//) : IAggregator {
//
//    companion object {
//        const val CLOUD_CONTACT_ROOT = "0"
//        private const val BATCH_SIZE = 300               // 批量写入阈值
//        private const val PROGRESS_INTERVAL_MS = 80L     // 进度回调最小间隔
//    }
//
//    // 线程安全的有序节点存储
//    private val nodes = ConcurrentHashMap<String, CommonNode>()
//    private val nodesOrder = ConcurrentLinkedDeque<String>()
//
//    // 子节点索引（增量维护）
//    private val childrenMap = ConcurrentHashMap<String, MutableList<String>>()
//
//    private val userDomainQueue = ConcurrentLinkedDeque<UserDomain>()
//
//    val userDomainDepartmentsRequestDeque = ConcurrentLinkedDeque<UserDomainEntity>()
//    @Volatile var currentDepartmentRequestMoid: String = ""
//
//    // 公司/用户队列（非线程安全，需加锁保护）
//    private val companyQueues = LinkedHashMap<String, ConcurrentLinkedDeque<MTWbParseKedaDeptListSecBean>>()
//    private val userQueues = LinkedHashMap<String, ConcurrentLinkedDeque<MTWbParse_Keda_UserList_Bean>>()
//    private val queuesLock = Any()
//
//    private var addGroupFin = AtomicBoolean(false)
//    private var addMemberFin = AtomicBoolean(false)
//
//    private var requestTime = System.currentTimeMillis()
//    private var currentNode = AtomicReference<CommonNode?>()
//    private var totalCount = AtomicInteger(0)
//    private var processedCount = AtomicInteger(0)
//    @Volatile private var keyword: String = ""
//
//    // 进度节流
//    private var lastProgressTime = 0L
//
//    /** 防重入：序列化 refresh / processGroup，杜绝 clear-vs-process 竞态 */
//    private val refreshMutex = Mutex()
//
//    override fun clear() {
//        totalCount.set(0)
//        processedCount.set(0)
//        requestTime = System.currentTimeMillis()
//        addGroupFin.compareAndSet(true, false)
//        addMemberFin.compareAndSet(true, false)
//        userDomainQueue.clear()
//        synchronized(queuesLock) {
//            companyQueues.clear()
//            userQueues.clear()
//        }
//        nodes.clear()
//        nodesOrder.clear()
//        childrenMap.clear()
//    }
//
//    suspend fun refresh() = refreshMutex.withLock {
//        checkIacBinder()
//        clear()
//        callback?.onProgress(0)
//        iac.nvRmtContactCtrlApi.getGroupReq()
//    }
//
//    suspend fun processGroup(node: GroupNode?) = refreshMutex.withLock {
//        TPLog.printDetail("init:${node?.toString()}")
//        clear()
//    }
//
//    suspend fun addUserDomain(value: UserDomain?) {
//        value?.let { userDomainQueue.push(it) }
//    }
//
//    suspend fun processUserDomain() {
//        while (userDomainQueue.isNotEmpty()) {
//            val domain = userDomainQueue.poll()?.AssParam?.atUserDomain ?: continue
//            domain.forEach { userDomain ->
//                TPLog.printDetail("processUserDomain:$userDomain")
//                val key = CLOUD_CONTACT_ROOT.plus(userDomain.achMoid)
//                val rootNode = CommonNode(
//                    id = key,
//                    parentId = userDomain.achParentId.ifBlank { "-1" },
//                    name = userDomain.achName,
//                    nodeType = NodeType.GROUP,
//                    nodeResourceType = NodeResourceType.CONTACT_CLOUD
//                )
//                val isNewRoot = nodes.putIfAbsent(key, rootNode) == null
//                if (isNewRoot) {
//                    nodesOrder.addLast(key)
//                    childrenMap.computeIfAbsent(rootNode.parentId) {
//                        Collections.synchronizedList(mutableListOf())
//                    }.add(key)
//                } else {
//                    nodes[key] = rootNode
//                }
//                currentNode.set(rootNode)
//                synchronized(queuesLock) {
//                    companyQueues[userDomain.achMoid] = ConcurrentLinkedDeque()
//                    userQueues[userDomain.achMoid] = ConcurrentLinkedDeque()
//                }
//                userDomainDepartmentsRequestDeque.push(userDomain)
//            }
//            getUserDomainDepartmentsAndUsers()
//        }
//    }
//
//    suspend fun getUserDomainDepartmentsAndUsers() {
//        addGroupFin.compareAndSet(true, false)
//        addMemberFin.compareAndSet(true, false)
//        TPLog.printDetail("getUserDomainDepartments -> ${userDomainDepartmentsRequestDeque.size}")
//        if (userDomainDepartmentsRequestDeque.isNotEmpty()) {
//            userDomainDepartmentsRequestDeque.pop().let {
//                currentDepartmentRequestMoid = it.achMoid
//            }
//        } else {
//            process()
//            callback?.apply {
//                currentNode.get()?.let { onCloudContactRootNode(it) }
//                onProcessSnapshot(this@CloudContactOrgAggregator, keyword)
//            }?.onProgress(100)
//        }
//    }
//
//    suspend fun addCompanyList(bean: MTWbParseKedaDeptListSecBean?) {
//        bean?.let {
//            totalCount.addAndGet(it.assParam?.atDepartment?.size ?: 0)
//            synchronized(queuesLock) {
//                companyQueues[currentDepartmentRequestMoid]?.push(it)
//            }
//        }
//    }
//
//    suspend fun addCompanyListFin() {
//        addGroupFin.compareAndSet(false, true)
//        if (addGroupFin.get() && addMemberFin.get()) getUserDomainDepartmentsAndUsers()
//    }
//
//    suspend fun addUserList(bean: MTWbParse_Keda_UserList_Bean?) {
//        bean?.let {
//            totalCount.addAndGet(it.assParam?.atUser?.size ?: 0)
//            synchronized(queuesLock) {
//                userQueues[currentDepartmentRequestMoid]?.push(it)
//            }
//        }
//    }
//
//    suspend fun addUserListFin() {
//        addMemberFin.compareAndSet(false, true)
//        if (addGroupFin.get() && addMemberFin.get()) getUserDomainDepartmentsAndUsers()
//    }
//
//    private var progress = 0
//        set(value) {
//            value.takeIf { it != field }?.let {
//                field = value
//                callback?.onProgress(it)
//            }
//        }
//
//    @SuppressLint("NewApi")
//    private suspend fun process() = withContext(Dispatchers.Default) {
//        // 1. 拷贝队列快照（在锁内）
//        val companySnapshot: List<Map.Entry<String, ConcurrentLinkedDeque<MTWbParseKedaDeptListSecBean>>>
//        val userSnapshot: List<Map.Entry<String, ConcurrentLinkedDeque<MTWbParse_Keda_UserList_Bean>>>
//        synchronized(queuesLock) {
//            companySnapshot = ArrayList(companyQueues.entries)
//            userSnapshot = ArrayList(userQueues.entries)
//        }
//
//        // 临时缓冲区
//        val pending = mutableListOf<Pair<String, CommonNode>>()
//        var counter = 0
//
//        // 2. 处理部门（批量写入）
//        companySnapshot.reversed().forEach { (key, value) ->
//            while (value.isNotEmpty()) {
//                val items = mutableListOf<MTWbParseKedaDeptSec>()
//                synchronized(queuesLock) {
//                    val bean = value.removeLast()?.assParam?.atDepartment
//                    if (bean != null) items.addAll(bean)
//                }
//                items.forEach { dept ->
//                    val deptKey = dept.dwDepartmentId.toString().plus(key)
//                    val parentKey = dept.dwParentId.toString().plus(key)
//                    pending.add(deptKey to CommonNode(
//                        id = deptKey,
//                        parentId = parentKey,
//                        name = dept.achDepartmentName,
//                        nodeType = NodeType.GROUP,
//                        nodeResourceType = NodeResourceType.CONTACT_CLOUD
//                    ))
//                    counter++
//                    processedCount.incrementAndGet()
//                    if (counter >= BATCH_SIZE) {
//                        flushBatch(pending)
//                        pending.clear()
//                        counter = 0
//                        reportProgress()
//                        yield()
//                    }
//                }
//            }
//        }
//
//        // 3. 处理用户（批量写入）
//        userSnapshot.reversed().forEach { (key, value) ->
//            while (value.isNotEmpty()) {
//                val items = mutableListOf<MTWbParse_Keda_User>()
//                synchronized(queuesLock) {
//                    val bean = value.removeLast()?.assParam?.atUser
//                    if (bean != null) items.addAll(bean)
//                }
//                items.forEach { user ->
//                    val userKey = "${user.achName}_${user.achMoid}"
//                    val parentKey = user.dwDepartmentID.toString().plus(key)
//                    pending.add(userKey to CommonNode(
//                        id = userKey,
//                        parentId = parentKey,
//                        name = user.achName,
//                        e164 = user.achE164,
//                        firstPy = PinYinUtil.getFirstSpell_TS(user.achName),
//                        allPy = PinYinUtil.getFullSpell(user.achName),
//                        nodeType = NodeType.MEMBER,
//                        nodeResourceType = NodeResourceType.CONTACT_CLOUD
//                    ))
//                    counter++
//                    processedCount.incrementAndGet()
//                    if (counter >= BATCH_SIZE) {
//                        flushBatch(pending)
//                        pending.clear()
//                        counter = 0
//                        reportProgress()
//                        yield()
//                    }
//                }
//            }
//        }
//
//        // 4. 刷入剩余节点
//        if (pending.isNotEmpty()) {
//            flushBatch(pending)
//            pending.clear()
//        }
//
//        // 5. 延迟计算 memberCount（从 MEMBER 向上累加）
//        computeMemberCounts()
//
//        // 6. 索引已由 flushBatch 增量维护，无需全量重建
//        // （保留空方法以防外部调用）
//    }
//
//    /**
//     * 批量写入节点并增量维护 childrenMap。
//     *
//     * 去重策略：
//     *  - 同一 key 首次写入 → 正常写入 nodes / nodesOrder / childrenMap
//     *  - 同一 key 再次写入 → 仅更新 nodes（覆盖内容），不重复追加 nodesOrder / childrenMap
//     */
//    private fun flushBatch(pending: List<Pair<String, CommonNode>>) {
//        pending.forEach { (key, node) ->
//            val isNew = nodes.putIfAbsent(key, node) == null
//            if (isNew) {
//                // 首次写入：维护顺序表和父子索引
//                nodesOrder.addLast(key)
//                childrenMap.computeIfAbsent(node.parentId) {
//                    Collections.synchronizedList(mutableListOf())
//                }.add(key)
//            } else {
//                // 重复 key：仅更新内容，不修改索引
//                nodes[key] = node
//            }
//        }
//    }
//
//    /**
//     * 延迟计算所有节点的 memberCount（MEMBER 数量）
//     */
//    private fun computeMemberCounts() {
//        nodes.values.forEach { node ->
//            if (node.nodeType == NodeType.MEMBER) {
//                var curId = node.parentId
//                while (curId.isNotEmpty()) {
//                    nodes.computeIfPresent(curId) { _, n ->
//                        n.copy(memberCount = n.memberCount + 1)
//                    }
//                    curId = nodes[curId]?.parentId ?: ""
//                }
//            }
//        }
//    }
//
//    /**
//     * 进度回调节流
//     */
//    private fun reportProgress() {
//        val now = System.currentTimeMillis()
//        if (now - lastProgressTime < PROGRESS_INTERVAL_MS) return
//        lastProgressTime = now
//        val total = totalCount.get()
//        if (total <= 0) return
//        val p = (processedCount.get().toFloat() / total * 100f).toInt()
//        progress = p
//    }
//
//    // 保留空实现，兼容旧的外部调用
//    @SuppressLint("NewApi")
//    private suspend fun rebuildChildrenIndex() {
//        // 已由增量维护替代
//    }
//
//    @SuppressLint("NewApi")
//    fun incrementAncestorsCount(id: String) {
//        // 此方法在延迟计数模式下不再需要，但保留以供外部调用
//        nodes.computeIfPresent(id) { _, node ->
//            node.copy(memberCount = node.memberCount + 1)
//        }
//        nodes[id]?.let {
//            if (it.parentId == "-1") return   // 到达根节点（parentId 即 achParentId.ifBlank{"-1"}）
//            incrementAncestorsCount(it.parentId)
//        }
//    }
//
//    @SuppressLint("NewApi")
//    private fun decrementAncestorsCount(id: String) {
//        nodes.computeIfPresent(id) { _, node ->
//            val newCount = node.memberCount - 1
//            node.copy(memberCount = if (newCount < 0) 0 else newCount)
//        }
//        nodes[id]?.let {
//            if (it.parentId == "-1") return   // 到达根节点
//            decrementAncestorsCount(it.parentId)
//        }
//    }
//
//    fun snapshot(): MutableList<CommonNode> {
//        val parentId = currentNode.get()?.id
//        return nodesOrder.mapNotNull { id ->
//            val node = nodes[id]
//            if (node != null && (parentId == null || node.parentId == parentId)) node else null
//        }.sort().toMutableList()
//    }
//
//    fun snapshotBySelected(): MutableList<CommonNode> {
//        return nodesOrder.mapNotNull { id ->
//            val node = nodes[id]
//            if (node?.nodeType == NodeType.MEMBER && node.selected) node else null
//        }.toMutableList()
//    }
//
//    private fun List<CommonNode>.sort(): List<CommonNode> = this
//
//    suspend fun selectedAllNode(selected: Boolean, max: Int): NodeErr {
//        TPLog.printDetail("selectedAllNode:$selected")
//        currentNode.get()?.let {
//            return selectedNode(it, selected, max)
//        } ?: TPLog.printError("selectedAllNode err of current node is null")
//        return NodeErr.NONE
//    }
//
//    private val selectionLock = Mutex()
//
//    suspend fun selectedNode(
//        node: CommonNode,
//        selected: Boolean,
//        maxSelectionCount: Int = 191
//    ): NodeErr = withContext(Dispatchers.Default) {
//        selectionLock.withLock {
//            TPLog.printDetail("selectedNode:$node, selected:$selected, max:$maxSelectionCount")
//
//            if (childrenMap.isEmpty()) {
//                TPLog.printError("childrenMap not ready, ignore selection")
//                return@withContext NodeErr.NONE
//            }
//
//            if (selected) {
//                // 计算当前已选中的 MEMBER 数量
//                val currentlySelectedMembers = nodes.values.count { it.selected && it.nodeType == NodeType.MEMBER }
//                TPLog.printDetail("currentlySelectedMembers: $currentlySelectedMembers, max: $maxSelectionCount")
//
//                if (currentlySelectedMembers >= maxSelectionCount) {
//                    TPLog.printError("already reached max selection count")
//                    return@withContext NodeErr.ERR_MAX_CLOUD_CONTACT
//                }
//
//                val currentNode = nodes[node.id] ?: return@withContext NodeErr.NONE
//
//                // 如果选中空GROUP，直接返回
//                if (currentNode.nodeType == NodeType.GROUP && currentNode.memberCount == 0) {
//                    TPLog.printDetail("empty group, skip selection")
//                    return@withContext NodeErr.NONE
//                }
//
//                // 如果节点已经是选中状态，且子节点有未选中的，继续向下选中
//                val needContinue = currentNode.selected &&
//                        currentNode.memberCount != currentNode.memberSelectedCount
//
//                val idsToUpdate = mutableSetOf<String>()
//                var remainingSlots = maxSelectionCount - currentlySelectedMembers
//                TPLog.printDetail("remainingSlots: $remainingSlots")
//
//                val queue = ArrayDeque<String>()
//                if (needContinue) {
//                    // 继续选中子节点
//                    childrenMap[node.id]?.forEach { queue.add(it) }
//                } else {
//                    // 选中当前节点及其子节点
//                    queue.add(node.id)
//                }
//
//                // 使用BFS遍历节点
//                while (queue.isNotEmpty() && remainingSlots > 0) {
//                    val currentId = queue.removeFirst()
//                    val n = nodes[currentId] ?: continue
//
//                    // 跳过空的GROUP
//                    if (n.nodeType == NodeType.GROUP && n.memberCount == 0) continue
//
//                    // 如果节点未选中
//                    if (!n.selected) {
//                        idsToUpdate.add(currentId)
//
//                        // 只有MEMBER节点占用名额
//                        if (n.nodeType == NodeType.MEMBER) {
//                            remainingSlots--
//                            TPLog.printDetail("selecting member: ${n.name}, remainingSlots: $remainingSlots")
//                        }
//                    }
//
//                    // 如果还有名额，继续遍历子节点
//                    if (remainingSlots > 0) {
//                        childrenMap[currentId]?.forEach { queue.add(it) }
//                    }
//                }
//
//                // 检查是否超过限制
//                val membersToSelect = idsToUpdate.count { nodes[it]?.nodeType == NodeType.MEMBER }
//                if (currentlySelectedMembers + membersToSelect > maxSelectionCount) {
//                    TPLog.printError("would exceed max selection count: $currentlySelectedMembers + $membersToSelect > $maxSelectionCount")
//                    return@withContext NodeErr.ERR_MAX_CLOUD_CONTACT
//                }
//
//                // 执行更新
//                var actuallySelectedCount = 0
//                idsToUpdate.forEach { id ->
//                    nodes.computeIfPresent(id) { _, oldNode ->
//                        if (!oldNode.selected) {
//                            val updatedNode = oldNode.copy(selected = true)
//                            if (updatedNode.nodeType == NodeType.MEMBER) {
//                                actuallySelectedCount++
//                                updateAncestorsSelectedCount(updatedNode.parentId, 1)
//                            }
//                            updatedNode
//                        } else oldNode
//                    }
//                }
//
//                TPLog.printDetail("actually selected $actuallySelectedCount members")
//
//                // 再次检查最终数量
//                val finalSelectedCount = nodes.values.count { it.selected && it.nodeType == NodeType.MEMBER }
//                if (finalSelectedCount > maxSelectionCount) {
//                    TPLog.printError("final count exceeds max: $finalSelectedCount > $maxSelectionCount")
//                    // 回滚操作
//                    idsToUpdate.forEach { id ->
//                        nodes.computeIfPresent(id) { _, oldNode ->
//                            if (oldNode.selected) {
//                                if (oldNode.nodeType == NodeType.MEMBER) {
//                                    updateAncestorsSelectedCount(oldNode.parentId, -1)
//                                }
//                                oldNode.copy(selected = false)
//                            } else oldNode
//                        }
//                    }
//                    return@withContext NodeErr.ERR_MAX_CLOUD_CONTACT
//                }
//
//            } else {
//                // 取消选中模式（不受名额限制）
//                val idsToCancel = mutableSetOf<String>()
//                val queue = ArrayDeque<String>().apply { add(node.id) }
//
//                while (queue.isNotEmpty()) {
//                    val currentId = queue.removeFirst()
//                    val n = nodes[currentId] ?: continue
//                    if (n.selected) {
//                        idsToCancel.add(currentId)
//                    }
//                    childrenMap[currentId]?.forEach { queue.add(it) }
//                }
//
//                idsToCancel.forEach { id ->
//                    nodes.computeIfPresent(id) { _, oldNode ->
//                        if (oldNode.selected) {
//                            if (oldNode.nodeType == NodeType.MEMBER) {
//                                updateAncestorsSelectedCount(oldNode.parentId, -1)
//                            }
//                            oldNode.copy(selected = false)
//                        } else oldNode
//                    }
//                }
//            }
//        }
//        callback?.onProcessSnapshot(this@CloudContactOrgAggregator, keyword)
//        return@withContext NodeErr.NONE
//    }
//
//    @SuppressLint("NewApi")
//    private fun updateAncestorsSelectedCount(startId: String, delta: Int) {
//        var currentId = startId
//        while (currentId.isNotEmpty()) {
//            val node = nodes[currentId] ?: break
//            val newCount = (node.memberSelectedCount + delta).coerceAtLeast(0)
//            var newSelected = node.selected
//
//            if (node.nodeType == NodeType.GROUP) {
//                when {
//                    delta > 0 && newCount > 0 && !node.selected -> {
//                        // GROUP 自动选中，但不占用名额
//                        newSelected = true
//                    }
//                    delta < 0 && newCount == 0 && node.selected -> newSelected = false
//                }
//            }
//
//            nodes[currentId] = node.copy(
//                memberSelectedCount = newCount,
//                selected = newSelected
//            )
//
//            if (node.parentId == "-1") break   // 到达根节点
//            currentId = node.parentId
//        }
//    }
//
//    suspend fun searchMember(keyword: String): MutableList<CommonNode> = withContext(Dispatchers.Default) {
//        TPLog.printDetail("searchMember:$keyword")
//        this@CloudContactOrgAggregator.keyword = keyword
//        val upperValue = keyword.uppercase()
//        // 线性扫描（如需极致性能可在此处加入拼音索引）
//        nodesOrder.mapNotNull { id ->
//            val node = nodes[id]
//            if (node != null &&
//                node.nodeType == NodeType.MEMBER &&
//                node.name != CLOUD_CONTACT_ROOT &&
//                (node.name.uppercase().contains(upperValue) ||
//                        node.e164.uppercase().contains(upperValue) ||
//                        node.ip.uppercase().contains(upperValue) ||
//                        node.firstPy.uppercase().contains(upperValue) ||
//                        node.allPy.uppercase().contains(upperValue))
//            ) node else null
//        }.sort().toMutableList()
//    }
//
//    fun enterGroup(node: CommonNode) {
//        currentNode.set(node)
//        callback?.onProcessSnapshot(this, keyword)
//    }
//
//    suspend fun checkRoot() {
//        if (null == currentNode.get()) refresh()
//    }
//}
