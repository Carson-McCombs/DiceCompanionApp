package model.database

import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFilterNotNull
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastMap
import model.dataObjects.Expression
import model.parser.evaluator.ShuntingYardParser


suspend fun AppRepository.updateGroupDependents(groupId: Long, oldFullPath: String, newFullPath: String){
    val childExpressions = groupDescendantsMap.value[groupId] ?: emptyList()
    val directDependentsMapState = expressionAllDirectDependentsMap.value
    val directDependentIds = childExpressions + childExpressions.fastFlatMap { expressionId -> directDependentsMapState[expressionId] ?: emptyList() }.distinct()
    val directDependents = directDependentIds.fastMap { expressionId -> expressionMap.value[expressionId] }.fastFilterNotNull()
    val affectedExpressions = directDependents.fastFlatMap { expression ->
        getUpdatedDependentExpressionReferences(
            expression = expression,
            oldFullPath = oldFullPath.drop(1),
            newFullPath = newFullPath.drop(1),
            updateRepository = false,
        )
    }.distinctBy { expression -> expression.id }
    upsertExpressions(affectedExpressions)
}

suspend fun AppRepository.getUpdatedDependentExpressionReferences(expression: Expression, oldFullPath: String, newFullPath: String, updateRepository: Boolean): List<Expression> {
    val dependentIds = expressionAllDirectDependentsMap.value[expression.id] ?: return emptyList()
    val expressionMapState = expressionMap.value
    val dependents = dependentIds.map { dependentId -> expressionMapState[dependentId] }.fastFilterNotNull()
    val updatedDependents = dependents.fastMap { dependent -> Expression.renameReference(expression = dependent, oldFullPath = oldFullPath, newFullPath = newFullPath) }
    if (updateRepository) upsertExpressions(updatedDependents)
    return updatedDependents
}

suspend fun AppRepository.updateDependentExpressions(expression: Expression) {
    val directDependentsFullPath = expressionAllDirectDependentsMap.value[expression.id] ?: return
    val expressionMapState = expressionMap.value
    val directDependents: List<Expression> =
        directDependentsFullPath.map { fullpath -> expressionMapState[fullpath]!! }
    if (!expression.parseResult.isStatic){
        upsertExpressions( directDependents.fastMap { directDependent -> directDependent.copy(updated = false) })
        return
    }
    val dynamicDirectDependents: List<Expression> = directDependents.fastFilter { dependentExpression -> !dependentExpression.parseResult.isStatic }.fastMap { dependentExpression -> dependentExpression.copy(updated = false) }
    upsertExpressions(dynamicDirectDependents)
    val staticDirectDependents: List<Expression> = directDependents.fastFilter { dependentExpression -> dependentExpression.parseResult.isStatic }
    val reevaluatedDirectDependents: List<Expression> = staticDirectDependents.map { dependentExpression ->
        dependentExpression.copy(
            parseResult = ShuntingYardParser.reevaluate(
                id = dependentExpression.id,
                rawText = dependentExpression.text,
                getGroupPath = { exprId ->
                    getExpressionGroupPath(exprId)
                },
                getExpressionId = { expressionFullPath: String ->
                    expressionFullPathToIdMap.value[expressionFullPath]
                },
                getExpression = { expressionId: Long ->
                    expressionMap.value[expressionId]
                },
                getOverlappingDependencies = {exprId, otherIds -> getOverlappingDependencies(exprId, otherIds) }
            )
        )
    }
    upsertExpressions(reevaluatedDirectDependents)
    reevaluatedDirectDependents.forEach{ dependentExpression ->
        updateDependentExpressions(expression = dependentExpression)
    }
}