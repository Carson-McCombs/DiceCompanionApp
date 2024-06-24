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

    /*@Transaction
    @Query("SELECT parents.id AS parentsId, children.id AS id " +
            "FROM groupEntity AS parents INNER JOIN groupEntity as children " +
            "WHERE parents.fullpath = children.path ")
    fun getGroupChildrenMap(): Flow<Map<@MapColumn(columnName = "parentsId") Long,  List<@MapColumn(columnName = "id")Long>>>*/

    @Transaction
    @Query("SELECT parents.id AS parentsId, children.id AS id " +
            "FROM groupEntity AS parents INNER JOIN groupEntity as children " +
            "WHERE parents.id = children.parentId ")
    fun getGroupChildrenMap(): Flow<Map<@MapColumn(columnName = "parentsId") Long?,  List<@MapColumn(columnName = "id")Long>>>

    /*@Transaction
    @Query("SELECT DISTINCT parentGroups.id AS parentsId, childExpressions.id AS id " +
            "FROM groupEntity AS parentGroups " +
            "INNER JOIN groupEntity AS childGroups ON parentGroups.fullpath = childGroups.path " +
            "INNER JOIN expressionEntity AS childExpressions ON childGroups.fullpath = childExpressions.path")
    fun getExpressionDescendantsMap(): Flow<Map<@MapColumn(columnName = "fullpath") Long, List<@MapColumn(columnName = "id") Long>>>*/

    @Transaction
    @Query("WITH descendant_table (id, parentId, expressionId) AS ( " +
            "SELECT parent.id, parent.parentId AS parentId, expression.id AS expressionId FROM expressionEntity AS expression INNER JOIN groupEntity AS parent WHERE parent.id = expression.parentId " +
            "UNION ALL " +
            "SELECT parent.id AS id, parent.parentId AS parentId, child.expressionId AS expressionId " +
            "FROM groupEntity as parent " +
            "INNER JOIN descendant_table AS child " +
            "ON parent.id = child.parentId " +
            ") " +
            "SELECT * FROM descendant_table")
    fun getGroupDescendantsMap(): Flow<Map<@MapColumn(columnName = "id") Long?, List<@MapColumn(columnName = "expressionId") Long>>>

    //@Query("SELECT * FROM groupEntity WHERE fullpath LIKE (:fullpath)")
    //fun getGroup(fullpath: String): GroupEntity?

    @Upsert
    suspend fun upsertGroup(groupEntity: GroupEntity): Long

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


    /*@Query("WITH fullpath_table (id, parentId, fullpath) AS ( " +
            "SELECT id, parentId, name  FROM groupEntity " +
            "UNION ALL " +
            "SELECT child.id , parent.parentId , parent.name || '/' || child.fullpath  " +
            "FROM groupEntity as parent " +
            "INNER JOIN fullpath_table AS child " +
            "ON parent.id = child.parentId " +
            ") " +
            "SELECT id, fullpath FROM fullpath_table")*/
}