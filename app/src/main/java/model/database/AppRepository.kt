package model.database

import androidx.annotation.WorkerThread
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import model.dataObjects.Expression
import model.dataObjects.Group
import model.dataObjects.GroupWithChildren
import model.database.entity.ExpressionDirectDependenciesEntity
import model.database.entity.ExpressionEntity
import model.database.entity.GroupEntity

class AppRepository(
    scope: CoroutineScope,
    private val database: AppDatabase
) {

    /*
    For populating Expression references in tokens
     */
    val groupIdToFullPathMap: StateFlow<Map<Long,String>> = database.groupDao().getFullPathMap().stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )
    val expressionMap: StateFlow<Map<Long, Expression>> = database.expressionDao().getExpressionMap().map { expressionEntityMap ->
        expressionEntityMap.mapValues { entry -> entry.value.toExpression() }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )

    val expressionFullPathToIdMap: StateFlow<Map<String, Long>> = groupIdToFullPathMap.combine(expressionMap){ idToFullPathMap, expMap ->
        expMap.values.associate { expression -> "${idToFullPathMap[expression.parentId]}/${expression.name}" to expression.id }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )
    val expressionIdToFullPathMap: StateFlow<Map<Long, String>> = expressionFullPathToIdMap.map { fullpathToIdMap ->
        fullpathToIdMap.map { entry -> entry.value to entry.key }.toMap()
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )



    /*
        For Updating full paths and Expression references
    */

    val groupMap: StateFlow<Map<Long, Group>> = database.groupDao().getGroupMap().map { groupEntityMapState ->
        groupEntityMapState.mapValues { entry -> entry.value.toGroup() }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )

    val groupDescendantsMap: StateFlow<Map<Long, List<Long>>> = database.groupDao().getGroupDescendantsMap().stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )
    val groupExpressionDescendantsMap: StateFlow<Map<Long, List<Long>>> = database.groupDao().getExpressionDescendantsMap().stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )

    /*
    For ensuring that there are no Cyclic dependencies
     */
    val expressionAllDirectDependentsMap: StateFlow<Map<Long, List<Long>>> = database.expressionDependencyDao().getExpressionAllDirectDependentsMap().stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )

    val expressionDeepDependentsMap: StateFlow<Map<Long, List<Long>>> = database.expressionDependencyDao().getExpressionDeepDependencyList().stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )

    /*
    Checking if there are possible Cyclic Dependencies
     */
    @WorkerThread
    fun getOverlappingDependencies(id: Long, otherIds: List<Long>): List<Long> = expressionDeepDependentsMap.value[id]?.fastFilter { dependentId -> otherIds.contains(dependentId) } ?: emptyList()


    @WorkerThread
    suspend fun upsertExpression(expression: Expression, updateDependents: Boolean = true): Long {
        val id = database.expressionDao().upsertExpression(ExpressionEntity.fromExpression(expression))
        if (updateDependents){
            database.expressionDependencyDao().deleteDependencies(expression.id)
            database.expressionDependencyDao().insertDependencies(ExpressionDirectDependenciesEntity.fromExpression(expression))
        }
        return id
    }

    @WorkerThread
    fun getExpressionGroupPath(expressionId: Long): String {
        val groupId = expressionMap.value[expressionId]?.parentId ?: return ""
        return groupIdToFullPathMap.value[groupId] ?: ""
    }

    @WorkerThread
    suspend fun upsertExpressions(expressions: List<Expression>): List<Long> = database.expressionDao().upsertExpressions(expressions.fastMap { expression -> ExpressionEntity.fromExpression(expression) })

    @WorkerThread
    suspend fun deleteExpression(expression: Expression) = database.expressionDao().deleteExpression(ExpressionEntity.fromExpression(expression))

    @WorkerThread
    fun getGroupWithChildren(id: Long): Flow<GroupWithChildren> = database.groupWithChildrenDao().getExpressionGroupWithChildren(id).map{ expressionGroupWithChildrenEntity -> expressionGroupWithChildrenEntity.toExpressionGroupWithChildren()}

    @WorkerThread
    suspend fun upsertGroup(group: Group): Long = database.groupDao().upsertGroup(GroupEntity.fromExpressionGroup(group))

    @WorkerThread
    suspend fun upsertGroups(groups: List<Group>): List<Long> = database.groupDao().upsertGroups(groups.fastMap { group -> GroupEntity.fromExpressionGroup(group) })

    @WorkerThread
    suspend fun deleteGroup(group: Group) = database.groupDao().deleteGroup(GroupEntity.fromExpressionGroup(group))

}