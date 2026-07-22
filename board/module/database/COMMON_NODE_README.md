# CommonNode 聚合系统

将多个独立数据源（类型1/2/3...）的部门+用户 CRUD 通知，聚合成统一的 `CommonNode`，基于 **Room 本地优先 + Kotlin Flow 自动驱动** 的响应式架构。

---

## 目录

- [1. 架构概览](#1-架构概览)
- [2. 数据模型](#2-数据模型)
- [3. Room 表结构](#3-room-表结构)
- [4. 快速开始](#4-快速开始)
- [5. API 参考](#5-api-参考)
  - [5.1 CommonNodeDao](#51-commonnodedao)
  - [5.2 CommonNodeRepository](#52-commonnoderepository)
  - [5.3 CommonNodeViewModel](#53-commonnodeviewmodel)
- [6. 数据流模式](#6-数据流模式)
- [7. CRUD 通知处理](#7-crud-通知处理)
- [8. 并发部门统计](#8-并发部门统计)
- [9. 树形展开](#9-树形展开)
- [10. 文件清单](#10-文件清单)

---

## 1. 架构概览

```
┌───────────┐  ┌───────────┐  ┌───────────┐
│ 类型1通知  │  │ 类型2通知  │  │ 类型3通知  │   ← 独立 CRUD 通知源
│ parentId  │  │ parentId  │  │ parentId  │
│ deptId    │  │ deptId    │  │ deptId    │
│ name      │  │ name      │  │ name      │
└─────┬─────┘  └─────┬─────┘  └─────┬─────┘
      │              │              │
      └──────────────┼──────────────┘
                     ▼
         ┌───────────────────────┐
         │ CommonNodeRepository  │  ← 本地优先 + Mutex 并发安全
         │  - loadIfNeeded()     │
         │  - handleNotification()│
         │  - observeDeptCounts() │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │   Room (common_node)  │  ← Single Source of Truth
         │   Flow 自动发射       │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │   ViewModel           │  ← flatMapLatest 跟随类型切换
         │   StateFlow → UI      │
         └───────────────────────┘
```

**核心设计原则：**

- **本地优先**：所有 `observe*()` 方法直接返回 Room Flow，数据在 Room 变化时自动发射，无需手动拉取
- **懒加载**：`loadIfNeeded()` 检查 Room，有数据直接返回，无数据才请求网络
- **并发安全**：每个 `resourceType` 独立 Mutex，刷新和通知写入互斥
- **类型切换**：`flatMapLatest` 自动跟随 `resourceType` 切换数据源

---

## 2. 数据模型

### 2.1 枚举定义

```kotlin
// 枚举1：区分分组还是成员
enum class NodeType {
    GROUP,   // 部门节点
    MEMBER   // 用户节点
}

// 枚举2：区分来自哪个数据源（类型1/2/3...）
enum class ResourceType(val code: Int, val label: String) {
    TYPE_1(1, "类型1"),
    TYPE_2(2, "类型2"),
    TYPE_3(3, "类型3"),
    // 按需扩展...
    companion object {
        fun fromCode(code: Int): ResourceType = ...
    }
}
```

### 2.2 CommonNode 实体

```kotlin
@Entity(tableName = "common_node", indices = [...])
data class CommonNode(
    @PrimaryKey val id: String,       // 复合主键，见下方 ID 规则
    val nodeType: NodeType,           // GROUP | MEMBER
    val resourceType: Int,            // 1 | 2 | 3 ...
    val parentId: String?,            // 父节点 ID（GROUP=上级部门，MEMBER=所属部门）
    val deptId: String?,              // 部门 ID（GROUP=自身，MEMBER=归属部门）
    val name: String,                 // 部门名 或 用户名
    val userId: String?,              // 用户 ID（仅 MEMBER 非空）
    val lastUpdated: Long = System.currentTimeMillis()
)
```

**ID 生成规则（重要）：**

| 节点类型 | ID 格式 | 示例 |
|---------|---------|------|
| GROUP | `{resourceType}_DEPT_{deptId}` | `1_DEPT_dept_root` |
| MEMBER | `{resourceType}_USER_{userId}` | `1_USER_zhangsan` |

```kotlin
// 使用 companion 方法生成，永远不要手动拼字符串
CommonNode.groupId(1, "dept_a")    // → "1_DEPT_dept_a"
CommonNode.memberId(1, "user_42")  // → "1_USER_user_42"
```

### 2.3 parentId 规则

- **GROUP 节点**：`parentId` = 上级部门 ID（根部门为 `null`）
- **MEMBER 节点**：`parentId` = 所属部门的 GROUP ID

这保证 `observeChildren(parentId)` 既返回子部门也返回部门成员。

### 2.4 辅助类型

```kotlin
// 部门成员计数结果
data class DeptMemberCount(val deptId: String, val cnt: Int)

// CRUD 操作类型
enum class CrudOperation { CREATE, UPDATE, DELETE }
```

---

## 3. Room 表结构

```sql
CREATE TABLE common_node (
    id             TEXT    NOT NULL PRIMARY KEY,
    nodeType       TEXT    NOT NULL,          -- "GROUP" | "MEMBER"
    resourceType   INTEGER NOT NULL,          -- 1 | 2 | 3 ...
    parentId       TEXT,                      -- 父节点 ID
    deptId         TEXT,                      -- 部门 ID
    name           TEXT    NOT NULL,          -- 显示名称
    userId         TEXT,                      -- 用户 ID
    lastUpdated    INTEGER NOT NULL DEFAULT 0 -- Unix millis
);

-- 单列索引
CREATE INDEX idx_common_node_nodeType     ON common_node(nodeType);
CREATE INDEX idx_common_node_resourceType ON common_node(resourceType);
CREATE INDEX idx_common_node_parentId     ON common_node(parentId);
CREATE INDEX idx_common_node_deptId       ON common_node(deptId);
CREATE INDEX idx_common_node_userId       ON common_node(userId);

-- 复合索引（高频查询路径）
CREATE INDEX idx_common_node_type_nodetype
    ON common_node(resourceType, nodeType);

CREATE INDEX idx_common_node_type_dept_nodetype
    ON common_node(resourceType, deptId, nodeType);
```

数据库版本：**AppDatabase v3**（MIGRATION_2_3 自动创建 common_node 表）

---

## 4. 快速开始

### 4.1 实现远程数据源

这是**唯一步骤**需要你根据实际 API 编写：

```kotlin
class MyRemoteDataSource : CommonNodeRemoteDataSource {

    override suspend fun fetchNodes(resourceType: Int): List<CommonNode> {
        // 1. 调用你的网络 API 获取部门列表和用户列表
        val response = api.getOrgData(resourceType)

        // 2. 转为 CommonNode（GROUP）
        val deptNodes = response.departments.map { dept ->
            CommonNode(
                id = CommonNode.groupId(resourceType, dept.id),
                nodeType = NodeType.GROUP,
                resourceType = resourceType,
                parentId = dept.parentId?.let { CommonNode.groupId(resourceType, it) },
                deptId = dept.id,
                name = dept.name,
                userId = null
            )
        }

        // 3. 转为 CommonNode（MEMBER）
        val memberNodes = response.users.map { user ->
            CommonNode(
                id = CommonNode.memberId(resourceType, user.id),
                nodeType = NodeType.MEMBER,
                resourceType = resourceType,
                parentId = CommonNode.groupId(resourceType, user.deptId),
                deptId = user.deptId,
                name = user.name,
                userId = user.id
            )
        }

        return deptNodes + memberNodes
    }
}
```

### 4.2 Application 初始化

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化数据库
        DatabaseManager.initDatabase(this)

        // 创建仓库（建议单例）
        repo = CommonNodeRepository(
            dao = DatabaseManager.getCommonNodeDao(),
            remote = MyRemoteDataSource()
        )
    }

    companion object {
        lateinit var repo: CommonNodeRepository
    }
}
```

### 4.3 Fragment 中使用

```kotlin
class OrgFragment : Fragment() {

    private val vm: OrgViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 观察数据（Room Flow → StateFlow → RecyclerView）
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { state ->
                    adapter.submitList(state.nodes)           // DiffUtil 局部刷新
                    updateDeptBadges(state.deptCounts)         // 部门角标
                }
            }
        }

        vm.loadType(1)   // 本地优先，无数据自动请求网络
    }

    // 切换类型
    fun onTypeChanged(newType: Int) { vm.loadType(newType) }

    // 下拉刷新
    fun onRefresh() { vm.refresh() }
}
```

---

## 5. API 参考

### 5.1 CommonNodeDao

所有 `observe*()` 方法返回 **Flow**，Room 自动跟踪表变化并重新发射。

#### CRUD

| 方法 | 说明 |
|------|------|
| `suspend insert(node)` | 插入或替换（REPLACE 策略） |
| `suspend insertAll(nodes)` | 批量插入 |
| `suspend update(node)` | 更新 |
| `suspend delete(node)` | 删除 |
| `suspend deleteById(id)` | 按 ID 删除 |
| `suspend deleteByResourceType(type)` | 按资源类型清空 |
| `suspend deleteAll()` | 清空全表 |

#### Flow 查询

| 方法 | SQL | 用途 |
|------|-----|------|
| `observeAll()` | `SELECT * FROM common_node ORDER BY nodeType, name` | 全量观察 |
| `observeByResourceType(Int)` | `WHERE resourceType = ?` | 按类型过滤 |
| `observeByNodeType(String)` | `WHERE nodeType = ?` | 按 GROUP/MEMBER 过滤 |
| `observeByTypeAndNodeType(Int, String)` | `WHERE resourceType = ? AND nodeType = ?` | 联合过滤 |

#### 树形查询

| 方法 | 说明 |
|------|------|
| `observeChildren(parentId): Flow<List<CommonNode>>` | 获取某节点的直属子节点（子部门 + 成员） |
| `observeRootDepts(resourceType): Flow<List<CommonNode>>` | 获取根部门（parentId IS NULL） |
| `observeMembersByDept(deptId): Flow<List<CommonNode>>` | 获取某部门下所有成员 |
| `observeSubDepts(deptId): Flow<List<CommonNode>>` | 获取某部门下所有子部门 |

#### 统计查询

| 方法 | 说明 |
|------|------|
| `countMembersByDept(deptId): Int` | 单部门成员数（suspend 单次） |
| `observeDeptMemberCounts(resourceType): Flow<List<DeptMemberCount>>` | 所有部门成员数（Flow 自动更新） |

#### 工具方法

| 方法 | 说明 |
|------|------|
| `countByResourceType(type): Int` | 检查某类型是否有数据 |
| `getById(id): CommonNode?` | 按 ID 单次查询 |
| `getAllByResourceType(type): List<CommonNode>` | 按类型单次查询 |

### 5.2 CommonNodeRepository

仓库层封装，提供懒加载、并发统计和通知处理。

#### 本地优先观察

```kotlin
// 所有 observe*() 直接返回 Room Flow，零额外开销
repo.observeAll()
repo.observeByResourceType(1)
repo.observeByNodeType(NodeType.GROUP)
repo.observeByTypeAndNodeType(1, NodeType.GROUP)
repo.observeChildren(parentId)
repo.observeRootDepts(1)
repo.observeMembersByDept(deptId)
```

#### 懒加载 & 刷新

```kotlin
// 懒加载 — Room 有数据直接返回，无数据才请求网络
// 返回 true 表示触发了远端刷新
val didFetch = repo.loadIfNeeded(1)

// 强制全量刷新 — 先删后插，原子操作
repo.refresh(1)

// 增量刷新 — 基于 lastUpdated 时间戳
// 远端不支持增量时自动回退到全量刷新
repo.refreshIncremental(1, since = lastSyncTime)
```

**内部实现细节：**

```kotlin
// loadIfNeeded 的内部逻辑
suspend fun loadIfNeeded(resourceType: Int): Boolean {
    val count = dao.countByResourceType(resourceType)
    if (count > 0) return false              // Room 有数据，直接返回
    val nodes = remote.fetchNodes(resourceType)  // 无数据，请求网络
    dao.insertAll(nodes)                      // 写入 Room
    // ↑ insertAll 后，所有 observe*() Flow 自动重新发射
    return true
}
```

#### 检查数据存在性

```kotlin
if (repo.hasData(1)) {
    // Room 中已有类型1的数据
}
```

#### 清空数据

```kotlin
repo.clearByType(1)    // 清空类型1
repo.clearAll()         // 清空全部
```

### 5.3 CommonNodeViewModel

```kotlin
class CommonNodeViewModel(repo: CommonNodeRepository) : ViewModel() {

    // ── 响应式属性 ──

    val currentResourceType: StateFlow<Int>     // 当前选中的类型

    val nodes: StateFlow<List<CommonNode>>      // 当前类型的所有节点
        // 实现: _currentType.flatMapLatest { repo.observeByResourceType(it) }

    val deptCounts: StateFlow<Map<String, Int>> // 部门 → 成员数
        // 实现: _currentType.flatMapLatest { repo.observeDeptMemberCounts(it) }

    val isLoading: StateFlow<Boolean>           // 加载状态

    // ── Actions ──

    fun loadData(resourceType: Int)    // 切换类型（懒加载）
    fun refresh()                      // 强制刷新当前类型
    fun handleNotification(node, op)   // 处理 CRUD 通知
    fun childrenOf(parentId): Flow<...> // 获取子节点（树形展开）
    fun rootDepts(type): Flow<...>      // 根部门列表
}
```

---

## 6. 数据流模式

### 6.1 核心链路

```
ResourceType 变化
    │
    ▼
flatMapLatest {
    repo.observeByResourceType(it)   ← Room Flow
}
    │
    ▼
stateIn(viewModelScope, WhileSubscribed)
    │
    ▼
StateFlow<List<CommonNode>>
    │
    ▼
Fragment.collect { adapter.submitList(it) }
    │
    ▼
DiffUtil → RecyclerView 局部刷新
```

### 6.2 四种触发场景

| 场景 | 触发路径 | Room Flow 是否重发 |
|------|---------|-------------------|
| 首次打开 | `loadType()` → Room 为空 → fetch → insertAll | ✅ 是 |
| 再次打开 | `loadType()` → Room 有数据 → 直接返回 | ❌ 否（Flow 已在 collect） |
| 下拉刷新 | `refresh()` → delete + insertAll | ✅ 是 |
| 推送通知 | `handleNotification()` → insert/update/delete | ✅ 是（单条变更） |

### 6.3 类型切换

```kotlin
// ViewModel 内部实现
val nodes: StateFlow<List<CommonNode>> = _currentResourceType
    .flatMapLatest { resourceType ->            // ← 切换时自动取消旧 Flow
        repo.observeByResourceType(resourceType) // ← 订阅新 Flow
    }
    .stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

// 使用时只需改一个 StateFlow 值
_currentResourceType.value = 2   // nodes 自动切换到类型2的数据
```

### 6.4 并发安全机制

```kotlin
// Repository 内部 — 每个 resourceType 一把独立 Mutex
private val mutexMap = mutableMapOf<Int, Mutex>()

suspend fun refresh(resourceType: Int) = mutexFor(resourceType).withLock {
    dao.deleteByResourceType(resourceType)  // 删
    dao.insertAll(remote.fetchNodes(...))    // 插
    // 同一把锁，handleNotification 和 refresh 不会交错执行
}
```

---

## 7. CRUD 通知处理

### 7.1 单个通知

```kotlin
// 来自推送：{ type: 2, op: "UPDATE", deptId: "group_x", name: "新名称" }

val node = CommonNode(
    id = CommonNode.groupId(2, "group_x"),
    nodeType = NodeType.GROUP,
    resourceType = 2,
    parentId = null,                    // 根据实际情况设置
    deptId = "group_x",
    name = "新名称",
    userId = null
)

viewModel.handleNotification(node, CrudOperation.UPDATE)
// → Room 单条 update → Flow 重新发射 → DiffUtil 更新对应行
```

### 7.2 批量通知

```kotlin
val ops = listOf(
    CommonNode(/* ... */) to CrudOperation.CREATE,
    CommonNode(/* ... */) to CrudOperation.DELETE,
    CommonNode(/* ... */) to CrudOperation.UPDATE,
)

viewModel.handleNotifications(resourceType = 1, ops)
// → 同一 Mutex 内顺序执行 → 一次 Flow 发射 → UI 批量更新
```

### 7.3 部门成员计数自动更新

当通过 `handleNotification` 新增或删除 MEMBER 节点后：

```kotlin
// 不需要手动刷新计数！
// observeDeptMemberCounts 的 SQL 是:
//   SELECT deptId, COUNT(*) FROM common_node
//   WHERE nodeType = 'MEMBER' AND resourceType = ?
//   GROUP BY deptId

// Room 检测到 common_node 表变化 → Flow 重新执行 SQL → deptCounts 自动更新
```

---

## 8. 并发部门统计

### 8.1 Flow 方式（推荐，自动更新）

```kotlin
// ViewModel 中
val deptCounts: StateFlow<Map<String, Int>> = _currentType
    .flatMapLatest { type -> repo.observeDeptMemberCounts(type) }
    .stateIn(viewModelScope, WhileSubscribed(5000), emptyMap())

// UI 中使用
lifecycleScope.launch {
    vm.deptCounts.collect { map ->
        // map = {"dept_a": 15, "dept_b": 8, "dept_c": 3}
        // 成员增删时自动更新
    }
}
```

### 8.2 一次性并发查询

```kotlin
// 需要查询特定几个部门的成员数，用并发加速
viewModelScope.launch {
    val deptIds = listOf("dept_a", "dept_b", "dept_c", ...)

    val results = repo.getDeptMemberCountsConcurrent(deptIds)
    // 内部实现: coroutineScope { deptIds.map { async { dao.countMembersByDept(it) } }.awaitAll() }

    // results = {"dept_a": 15, "dept_b": 8, ...}
    // N 个部门并发查询，总耗时 ≈ 最慢的那个查询
}
```

### 8.3 单部门查询

```kotlin
val count = repo.countMembersByDept("dept_a")  // → 15
```

---

## 9. 树形展开

### 9.1 数据准备

确保 MEMBER 节点的 `parentId` 指向所属部门的 GROUP ID：

```kotlin
// 用户 zhangsan 属于 dept_a
val memberNode = CommonNode(
    id = CommonNode.memberId(1, "zhangsan"),
    nodeType = NodeType.MEMBER,
    resourceType = 1,
    parentId = CommonNode.groupId(1, "dept_a"),  // ← 关键：挂到部门下
    deptId = "dept_a",
    name = "张三",
    userId = "zhangsan"
)
```

### 9.2 获取子节点

```kotlin
// 获取 dept_a 下的所有子节点（子部门 + 成员）
repo.observeChildren(CommonNode.groupId(1, "dept_a"))
    .collect { children ->
        // children = [
        //   CommonNode(nodeType=GROUP, name="研发一组"),    // 子部门
        //   CommonNode(nodeType=GROUP, name="研发二组"),    // 子部门
        //   CommonNode(nodeType=MEMBER, name="张三"),       // 成员
        //   CommonNode(nodeType=MEMBER, name="李四"),       // 成员
        // ]
    }
```

### 9.3 根部门

```kotlin
// 获取所有根部门
repo.observeRootDepts(1).collect { roots ->
    // roots = [CommonNode(nodeType=GROUP, name="总公司")]
}
```

### 9.4 懒加载展开（推荐）

不要在首次加载时一次性构建完整树。而是：

```kotlin
// 1. 初始只加载根部门
repo.observeRootDepts(resourceType).collect { roots ->
    adapter.submitList(roots)    // 只显示根部门
}

// 2. 用户点击展开 dept_a
fun onExpand(deptId: String) {
    repo.observeChildren(deptId).collect { children ->
        // 把 children 插入到 dept_a 下方，展开
    }
}
```

---

## 10. 文件清单

```
board/module/database/src/main/java/com/cn/board/database/
├── CommonNode.kt                    # Entity + 枚举（NodeType/ResourceType/CrudOperation）+ DeptMemberCount
├── CommonNodeTypeConverters.kt      # Room TypeConverter（NodeType ↔ String）
├── CommonNodeDao.kt                 # DAO：CRUD + Flow 查询 + 树形 + 统计
├── CommonNodeRemoteDataSource.kt    # 网络数据源接口（需实现）
├── CommonNodeRepository.kt         # 仓库层：本地优先 + Mutex + 并发统计
├── CommonNodeViewModel.kt          # ViewModel：flatMapLatest + StateFlow
├── CommonNodeUsageExample.kt       # 完整使用示例（7步）
├── AppDatabase.kt                  # [已修改] v2→v3，新增 CommonNode + MIGRATION_2_3
└── DatabaseManager.kt              # [已修改] 新增 getCommonNodeDao()
```

### 依赖

```kotlin
// build.gradle.kts
implementation("androidx.room:room-runtime:2.7.0")
implementation("androidx.room:room-ktx:2.7.0")
kapt("androidx.room:room-compiler:2.7.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
```
