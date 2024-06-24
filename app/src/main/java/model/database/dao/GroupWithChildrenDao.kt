package model.database.dao

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import model.database.entity.relationship.GroupWithChildrenEntity

@Dao
interface GroupWithChildrenDao {
    @Transaction
    @Query("SELECT * FROM groupEntity WHERE id LIKE (:id)")
    fun getExpressionGroupWithChildren(id: Long): Flow<GroupWithChildrenEntity>

    @Transaction
    @Query("SELECT * FROM groupEntity")
    fun getExpressionGroupWithChildrenMap(): Flow<Map<@MapColumn(columnName = "id")Long, GroupWithChildrenEntity>>

}