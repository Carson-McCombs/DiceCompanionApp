package model.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import model.database.entity.GroupEntity

@Dao
interface GroupDao {

    @Query("SELECT * FROM groupEntity WHERE parentId IS NULL LIMIT 1")
    fun getRootGroup(): GroupEntity?

    @Query("SELECT * FROM groupEntity")
    fun getGroupMap(): Flow<Map<@MapColumn(columnName = "id") Long, GroupEntity>>

    @Transaction
    @Query("SELECT parents.id AS parentsId, children.id AS id " +
            "FROM groupEntity AS parents INNER JOIN groupEntity as children " +
            "WHERE parents.id = children.parentId ")
    fun getGroupChildrenMap(): Flow<Map<@MapColumn(columnName = "parentsId") Long?,  List<@MapColumn(columnName = "id")Long>>>

    @Transaction
    @Query("WITH descendant_group_table (id, groupId) AS ( " +
            "SELECT groupEntity.parentId, groupEntity.id FROM groupEntity WHERE groupEntity.parentId IS NOT NULL " +
            "UNION ALL " +
            "SELECT group_table.id AS parentId, child.id AS groupId " +
            "FROM groupEntity AS child " +
            "INNER JOIN descendant_group_table AS group_table " +
            "ON group_table.groupId = child.parentId " +
            ") " +
            "SELECT DISTINCT groups.id AS parentId, groups.groupId AS groupIds FROM descendant_group_table AS groups " +
            "UNION ALL " +
            "SELECT groupEntity.parentId AS parentId, groupEntity.id AS groupIds FROM groupEntity")
    fun getGroupDescendantsMap(): Flow<Map<@MapColumn(columnName = "parentId") Long, List<@MapColumn(columnName = "groupIds")Long>>>

    @Transaction
    @Query("WITH descendant_group_table (id, groupId) AS ( " +
            "SELECT groupEntity.parentId, groupEntity.id FROM groupEntity WHERE groupEntity.parentId IS NOT NULL " +
            "UNION ALL " +
            "SELECT group_table.id AS parentId, child.id AS groupId " +
            "FROM groupEntity AS child " +
            "INNER JOIN descendant_group_table AS group_table " +
            "ON group_table.groupId = child.parentId " +
            "), " +
            "descendant_expression_table (id, expressionId) AS ( " +
            "SELECT groups.id AS id, expressions.id AS expressionId FROM expressionEntity AS expressions " +
            "INNER JOIN descendant_group_table AS groups " +
            "ON groups.groupId = expressions.parentId OR groups.id = expressions.parentId" +
            ") " +
            "SELECT DISTINCT expressions.id AS parentId, expressions.expressionId AS expressionIds FROM descendant_expression_table AS expressions " +
            "UNION ALL " +
            "SELECT expressionEntity.parentId AS parentId, expressionEntity.id AS expressionIds FROM expressionEntity")
    fun getExpressionDescendantsMap(): Flow<Map<@MapColumn(columnName = "parentId") Long, List<@MapColumn(columnName = "expressionIds")Long>>>

    @Upsert
    suspend fun upsertGroup(groupEntity: GroupEntity): Long

    @Upsert
    suspend fun upsertGroups(groupEntities: List<GroupEntity>): List<Long>

    @Delete
    suspend fun deleteGroup(groupEntity: GroupEntity)

    @Transaction
    @Query("WITH fullpath_table (id, parentId, fullpath) AS ( " +
            "SELECT id, parentId, name  FROM groupEntity " +
            "UNION ALL " +
            "SELECT child.id , parent.parentId , parent.name || '/' || child.fullpath  " +
            "FROM groupEntity as parent " +
            "INNER JOIN fullpath_table AS child " +
            "ON parent.id = child.parentId " +
            ") " +
            "SELECT * FROM fullpath_table WHERE parentId IS NULL")
    fun getFullPathMap(): Flow<Map<@MapColumn(columnName = "id") Long, @MapColumn(columnName = "fullpath") String>>


}
