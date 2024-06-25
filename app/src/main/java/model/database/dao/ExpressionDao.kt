package model.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import model.database.entity.ExpressionEntity


@Dao
interface ExpressionDao {
    
    @Query("SELECT * FROM expressionEntity WHERE id LIKE (:id)")
    fun getExpression(id: Long): Flow<ExpressionEntity>

    @Query("SELECT * FROM expressionEntity WHERE id in (:ids)")
    fun getExpressions(ids: List<Long>): Flow<List<ExpressionEntity>>
    @Transaction
    @Query("SELECT * FROM expressionEntity")
    fun getExpressionMap(): Flow<Map<@MapColumn(columnName = "id") Long, ExpressionEntity>>

    @Transaction
    @Query("SELECT id, parentId FROM expressionEntity")
    fun getExpressionNameToIdMap(): Flow<Map<@MapColumn(columnName = "parentId")Long, @MapColumn(columnName = "id")Long>>

    @Transaction
    @Query("SELECT * FROM expressionEntity WHERE parentId LIKE (:parentId)")
    fun getExpressionMapAtPath(parentId: Long): Flow<Map<@MapColumn(columnName = "id") Long, ExpressionEntity>>

    @Upsert
    suspend fun upsertExpression(expressionEntity: ExpressionEntity): Long

    @Transaction
    @Upsert
    suspend fun upsertExpressions(expressionEntities: List<ExpressionEntity>): List<Long>

    @Delete
    suspend fun deleteExpression(expressionEntity: ExpressionEntity)

    @Query("DELETE FROM expressionEntity")
    suspend fun deleteAllExpression()
}