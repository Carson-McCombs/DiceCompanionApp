package model.dataObjects

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.database.AppRepository

class ClipboardReference(
    private val scope: CoroutineScope,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val repository: AppRepository
) {

    private val expressionSelectionStateMap: MutableMap<Long, MutableState<Boolean>> = mutableMapOf()
    private val groupSelectionStateMap: MutableMap<Long, MutableState<Boolean>> = mutableMapOf()

    private var expressionIds: MutableList<Long> = mutableListOf()
    private var groupIds: MutableList<Long> = mutableListOf()

    private var deepExpressions: List<Expression> = emptyList()
    private var deepGroups: List<Group> = emptyList()
    private var deepExpressionIds: List<Long> = emptyList()
    private var deepGroupIds: List<Long> = emptyList()


    fun getExpressionSelectionState(expressionId: Long): MutableState<Boolean> {
        if (!expressionSelectionStateMap.containsKey(expressionId)) expressionSelectionStateMap[expressionId] = mutableStateOf(false)
        return expressionSelectionStateMap[expressionId] ?: mutableStateOf(false)
    }
    fun getGroupSelectionState(groupId: Long): MutableState<Boolean> {
        if (!groupSelectionStateMap.containsKey(groupId)) groupSelectionStateMap[groupId] = mutableStateOf(false)
        return groupSelectionStateMap[groupId] ?: mutableStateOf(false)
    }
    fun clear() {
        expressionIds.clear()
        groupIds.clear()
        deepExpressions = emptyList()
        deepGroups = emptyList()
        deepExpressionIds = emptyList()
        deepGroupIds = emptyList()
    }
    private fun populateReferences(){
        expressionIds = expressionSelectionStateMap.filterValues { state -> state.value }.map { entry -> entry.key }.toMutableList()
        groupIds = groupSelectionStateMap.filterValues { state -> state.value }.map { entry -> entry.key }.toMutableList()
        getDeepReferenceIds()
        getDeepReferences()
    }
    private fun getDeepReferenceIds(){
        val groupDescendantsMapState = repository.groupDescendantsMap.value
        val groupExpressionDescendantsMapState = repository.groupExpressionDescendantsMap.value
        deepGroupIds = (groupIds + groupIds.fastFlatMap { groupId -> groupDescendantsMapState[groupId]?: emptyList() }).distinct()
        deepExpressionIds = (expressionIds + groupIds.fastFlatMap { groupId -> groupExpressionDescendantsMapState[groupId]?: emptyList() }).distinct()
    }
    private fun getDeepReferences(){
        val groupMapState = repository.groupMap.value
        val expressionMapState = repository.expressionMap.value
        deepGroups = deepGroupIds.fastMap { groupId -> groupMapState[groupId]!! }
        deepExpressions = deepExpressionIds.fastMap { expressionId -> expressionMapState[expressionId]!! }
    }
    fun copyTo(parentId: Long) {
        populateReferences()
        scope.launch (defaultDispatcher){
            if (deepGroupIds.isEmpty() && deepExpressionIds.isEmpty()) return@launch
            val (parentGroups, parentExpressions) = getDirectDescendants(parentId)
            val layeredGroupsList = getLayeredGroupsList()
            val layeredExpressionList = getLayeredExpressionsList(layeredGroupsList)
            val oldToNewGroupIdMap = emptyMap<Long,Long>().toMutableMap()
            layeredGroupsList[0] = updateOverlappingGroupNames(parentGroups, layeredGroupsList.getOrElse(index = 0, defaultValue = {emptyList()}))
            layeredExpressionList[0] = updateOverlappingExpressionNames(parentExpressions, layeredExpressionList.getOrElse(index = 0, defaultValue = { emptyList() }))
            for (layerIndex in 0..layeredExpressionList.size) {
                val copiedGroups = layeredGroupsList.getOrElse(index = layerIndex, defaultValue = { emptyList() }).fastMap { group ->
                    group.copy(
                        id = 0,
                        parentId = if (layerIndex == 0) parentId else oldToNewGroupIdMap[group.parentId]
                    )
                }
                val copiedExpressions = layeredExpressionList.getOrElse(index = layerIndex, defaultValue = { emptyList() }).fastMap { expression ->
                    expression.copy(
                        id = 0,
                        parentId = if (layerIndex == 0) parentId else oldToNewGroupIdMap[expression.parentId]!!
                    )
                }
                val newGroupIds = upsertCopiedLayer(copiedGroups, copiedExpressions)
                if (newGroupIds.isNotEmpty()) oldToNewGroupIdMap.putAll(newGroupIds.fastMapIndexed{ i, newGroupId -> layeredGroupsList[layerIndex][i].id to newGroupId})
            }
        }

    }

    private suspend fun upsertCopiedLayer(copiedGroups: List<Group>, copiedExpression: List<Expression>): List<Long> {
        return withContext(defaultDispatcher){
            if (copiedExpression.isNotEmpty()) repository.upsertExpressions(copiedExpression)
            if (copiedGroups.isNotEmpty()) return@withContext repository.upsertGroups(copiedGroups)
            return@withContext emptyList()
        }
    }


    private suspend fun updateOverlappingGroupNames(parentGroups: List<Group>, rootGroups: List<Group>): List<Group> {
        return withContext(defaultDispatcher){
            if (parentGroups.isEmpty() || rootGroups.isEmpty()) return@withContext rootGroups
            val parentGroupNames = parentGroups.fastMap{group -> group.name}.toMutableList()
            if (rootGroups.fastAll { group -> parentGroupNames.fastAll { groupName -> !groupName.contentEquals(group.name) }}) return@withContext rootGroups
            return@withContext rootGroups.fastMap { group ->
                var count = 0
                var name = group.name
                while (parentGroupNames.contains(name)){
                    count ++
                    name = "${group.name}_$count"
                }
                parentGroupNames.add(name)
                group.copy(name = name)
            }
        }

    }
    private suspend fun updateOverlappingExpressionNames(parentExpressions: List<Expression>, rootExpressions: List<Expression>): List<Expression> {
        return withContext(defaultDispatcher){
            if (parentExpressions.isEmpty() || rootExpressions.isEmpty()) return@withContext rootExpressions
            val parentExpressionNames = parentExpressions.fastMap{ expression -> expression.name}.toMutableList()
            if (rootExpressions.fastAll { expression -> parentExpressionNames.fastAll { expressionName -> !expressionName.contentEquals(expression.name)  } }) return@withContext rootExpressions
            return@withContext rootExpressions.fastMap { expression ->
                var count = 0
                var name = expression.name
                while (parentExpressionNames.contains(name)){
                    count ++
                    name = "${expression.name}_$count"
                }
                parentExpressionNames.add(name)
                expression.copy(name = name)
            }
        }

    }

    private suspend fun getDirectDescendants(parentId: Long): Pair<List<Group>, List<Expression>> {
        return withContext(defaultDispatcher){
            val parentToGroupMapState = repository.groupMap.value.map{ entry -> entry.value.parentId to entry.value }.groupBy(keySelector = {pair -> pair.first}, valueTransform = {pair -> pair.second})
            val parentToExpressionMapState = repository.expressionMap.value.map{ entry -> entry.value.parentId to entry.value }.groupBy(keySelector = {pair -> pair.first}, valueTransform = {pair -> pair.second})
            return@withContext Pair(parentToGroupMapState[parentId] ?: emptyList(), parentToExpressionMapState[parentId] ?: emptyList())
        }
    }

    private suspend fun getLayeredGroupsList(): MutableList<List<Group>> {
        return withContext(defaultDispatcher){
            val rootGroups = deepGroups.fastFilter { group -> !deepGroupIds.contains(group.parentId) }
            val layeredGroupsList = mutableListOf(rootGroups)
            val unassignedGroups = deepGroups.toMutableList()
            unassignedGroups.removeAll(rootGroups)
            val queue = ArrayDeque(listOf(rootGroups))
            var layerNumber = 1
            while (queue.isNotEmpty()) {
                val layer = queue.removeFirst()
                val nextLayer = emptyList<Group>().toMutableList()
                for (group in layer) {
                    val descendants = getGroupDescendants(group = group, unassignedGroups = unassignedGroups.toList())
                    nextLayer.addAll(descendants)
                    unassignedGroups.removeAll(descendants)
                }
                if (nextLayer.isEmpty()) break
                queue.add(nextLayer)
                layeredGroupsList.add(nextLayer)
                layerNumber ++
                if (layerNumber > deepGroups.size) break
            }
            return@withContext layeredGroupsList
        }

    }
    private suspend fun getLayeredExpressionsList(layeredGroupsList: List<List<Group>>): MutableList<List<Expression>> {
        return withContext(defaultDispatcher) {
            val rootExpressions = deepExpressions.fastFilter { expression -> !deepGroups.fastAny { group -> expression.parentId == group.id } }
            val parentIdToLayerMap = layeredGroupsList.fastMapIndexed { layerIndex, layer -> layer.fastMap { group -> group.id to layerIndex + 1 }  }.flatten().toMap()
            val layeredExpressionList = mutableListOf(rootExpressions.toMutableList())
            layeredGroupsList.fastForEach{layeredExpressionList.add(mutableListOf())}
            deepExpressions.fastFilter { expression -> !rootExpressions.contains(expression) }.fastForEach { expression ->
                val layerIndex = parentIdToLayerMap[expression.parentId]!!
                layeredExpressionList[layerIndex].add(expression)
            }
            return@withContext layeredExpressionList.fastMap { layer -> layer }.toMutableList()
        }

    }
    private fun getGroupDescendants(group: Group, unassignedGroups: List<Group>): List<Group> {
        return unassignedGroups.fastFilter { otherGroup -> group.id == otherGroup.parentId }
    }

}