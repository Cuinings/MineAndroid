package com.cn.board.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * CommonNode DAO — 本地优先，所有观察方法返回 Flow。
 * 数据变化时 Room 自动重新发射，UI 层直接 collect 即可。
 */
@Dao
interface CommonNodeDao {

    // ═══════════════════════════════════════════
    // CRUD（suspend，线程安全）
    // ═══════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: CommonNode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<CommonNode>)

    @Update
    suspend fun update(node: CommonNode)

    @Delete
    suspend fun delete(node: CommonNode)

    @Query("DELETE FROM common_node WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM common_node WHERE resourceType = :resourceType")
    suspend fun deleteByResourceType(resourceType: Int)

    @Query("DELETE FROM common_node")
    suspend fun deleteAll()

    // ═══════════════════════════════════════════
    // Flow 查询 — Room 自动跟踪变化
    // ═══════════════════════════════════════════

    /** 观察全部节点（按名称排序，GROUP 在前） */
    @Query("SELECT * FROM common_node ORDER BY nodeType ASC, name ASC")
    fun observeAll(): Flow<List<CommonNode>>

    /** 按资源类型观察 */
    @Query("SELECT * FROM common_node WHERE resourceType = :resourceType ORDER BY nodeType ASC, name ASC")
    fun observeByResourceType(resourceType: Int): Flow<List<CommonNode>>

    /** 按节点类型观察（String 参数，TypeConverter 处理） */
    @Query("SELECT * FROM common_node WHERE nodeType = :nodeType ORDER BY name ASC")
    fun observeByNodeType(nodeType: String): Flow<List<CommonNode>>

    /** 按资源类型 + 节点类型联合观察 */
    @Query("SELECT * FROM common_node WHERE resourceType = :resourceType AND nodeType = :nodeType ORDER BY name ASC")
    fun observeByTypeAndNodeType(resourceType: Int, nodeType: String): Flow<List<CommonNode>>

    // ═══════════════════════════════════════════
    // 树形层级查询
    // ═══════════════════════════════════════════

    /** 获取某节点的直接子节点（GROUP 子部门 + MEMBER 用户），先分组后成员 */
    @Query("SELECT * FROM common_node WHERE parentId = :parentId ORDER BY nodeType ASC, name ASC")
    fun observeChildren(parentId: String): Flow<List<CommonNode>>

    /** 获取根部门（parentId IS NULL 且为 GROUP） */
    @Query("SELECT * FROM common_node WHERE parentId IS NULL AND nodeType = 'GROUP' AND resourceType = :resourceType ORDER BY name ASC")
    fun observeRootDepts(resourceType: Int): Flow<List<CommonNode>>

    /** 获取某部门下所有成员 */
    @Query("SELECT * FROM common_node WHERE deptId = :deptId AND nodeType = 'MEMBER' ORDER BY name ASC")
    fun observeMembersByDept(deptId: String): Flow<List<CommonNode>>

    /** 获取某部门下所有子部门 */
    @Query("SELECT * FROM common_node WHERE parentId = :deptId AND nodeType = 'GROUP' ORDER BY name ASC")
    fun observeSubDepts(deptId: String): Flow<List<CommonNode>>

    // ═══════════════════════════════════════════
    // 统计查询
    // ═══════════════════════════════════════════

    /** 某部门成员计数（suspend 单次查询） */
    @Query("SELECT COUNT(*) FROM common_node WHERE deptId = :deptId AND nodeType = 'MEMBER'")
    suspend fun countMembersByDept(deptId: String): Int

    /** 批量部门成员计数 — Flow 自动更新（一次性返回所有部门的统计） */
    @Query("""
        SELECT deptId, COUNT(*) AS cnt 
        FROM common_node 
        WHERE nodeType = 'MEMBER' AND resourceType = :resourceType 
        GROUP BY deptId
    """)
    fun observeDeptMemberCounts(resourceType: Int): Flow<List<DeptMemberCount>>

    // ═══════════════════════════════════════════
    // 数据存在性 & 单次查询
    // ═══════════════════════════════════════════

    @Query("SELECT COUNT(*) FROM common_node WHERE resourceType = :resourceType")
    suspend fun countByResourceType(resourceType: Int): Int

    @Query("SELECT * FROM common_node WHERE id = :id")
    suspend fun getById(id: String): CommonNode?

    @Query("SELECT * FROM common_node WHERE resourceType = :resourceType ORDER BY name ASC")
    suspend fun getAllByResourceType(resourceType: Int): List<CommonNode>
}
