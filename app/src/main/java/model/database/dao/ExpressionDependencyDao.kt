package model.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import model.database.entity.ExpressionDirectDependenciesEntity

@Dao
interface ExpressionDependencyDao {

    @Transaction
    @Query("WITH recursive_table (id, dependencyId, depth) AS ( " +
            "SELECT id, dependencyId, 0 as depth " +
            "FROM expressionDirectDependenciesEntity " +
            "UNION ALL " +
            "SELECT relationship.id, relationship.dependencyId, recursion.depth + 1 " +
            "FROM expressionDirectDependenciesEntity AS relationship " +
            "INNER JOIN recursive_table AS recursion " +
            "ON recursion.id = relationship.dependencyId " +
            ") " +
            "SELECT id, dependencyId FROM recursive_table")
    fun getExpressionDeepDependencyList(): Flow<List<ExpressionDirectDependenciesEntity>>


    @Query("SELECT * FROM expressionDirectDependenciesEntity")
    fun getExpressionDirectDependentMap(): Flow<Map<@MapColumn(columnName = "dependencyId") Long, List<ExpressionDirectDependenciesEntity>>>

    @Insert
    fun insertDependencies(expressionDirectDependenciesEntity: List<ExpressionDirectDependenciesEntity>)

    @Transaction
    @Query("DELETE FROM expressionDirectDependenciesEntity WHERE id = (:id)")
    fun deleteDependencies(id: Long)
}
