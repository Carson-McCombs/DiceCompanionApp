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
    fun getExpressionDeepDependencyList(): Flow<Map<@MapColumn(columnName = "dependencyId") Long, List<@MapColumn(columnName = "id") Long>>>

    @Query("SELECT * FROM expressionDirectDependenciesEntity")
    fun getExpressionAllDirectDependentsMap(): Flow<Map<@MapColumn(columnName = "dependencyId") Long, List<@MapColumn(columnName = "id") Long>>>

    @Query("SELECT * FROM expressionDirectDependenciesEntity WHERE isLocal = (:isLocal)")
    fun getExpressionDirectDependenciesMap(isLocal: Boolean): Flow<Map<@MapColumn(columnName = "id") Long, List<@MapColumn(columnName = "dependencyId") Long>>>

    @Insert
    fun insertDependencies(expressionDirectDependenciesEntity: List<ExpressionDirectDependenciesEntity>)

    @Transaction
    @Query("DELETE FROM expressionDirectDependenciesEntity WHERE id = (:id)")
    fun deleteDependencies(id: Long)
}
