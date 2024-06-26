package model.dataObjects

import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import model.database.AppRepository

class ClipboardReference(private val scope: CoroutineScope, private val repository: AppRepository) {
    private var expressionIds: List<Long> = emptyList()
    private var groupIds: List<Long> = emptyList()

    private var deepExpressions: List<Expression> = emptyList()
    private var deepGroups: List<Group> = emptyList()
    private var deepExpressionIds: List<Long> = emptyList()
    private var deepGroupIds: List<Long> = emptyList()
    fun copy(expressionIds: List<Long>, groupIds: List<Long>) {
        this.expressionIds = expressionIds
        this.groupIds = groupIds
        getDeepReferences()


    }
    private fun getDeepReferences(){
        scope.launch(Dispatchers.IO) {
            val groupDescendantsMapState = repository.groupDescendantsMap.value
            val groupExpressionDescendantsMapState = repository.groupExpressionDescendantsMap.value
            deepGroupIds = (groupIds + groupIds.fastFlatMap { groupId -> groupDescendantsMapState[groupId]?: emptyList() }).distinct()
            deepExpressionIds = (expressionIds + groupIds.fastFlatMap { groupId -> groupExpressionDescendantsMapState[groupId]!! })

            val groupMapState = repository.groupMap.value
            val expressionMapState = repository.expressionMap.value
            deepGroups = deepGroupIds.fastMap { groupId -> groupMapState[groupId]!! }
            deepExpressions = deepExpressionIds.fastMap { expressionId -> expressionMapState[expressionId]!! }
        }
    }
    fun paste(parentId: Long) {
        if (deepGroupIds.isEmpty() && deepExpressionIds.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val (parentGroups, parentExpressions) = getDirectDescendants(parentId)
            val layeredGroupsList = getLayeredGroupsList()
            val layeredExpressionList = getLayeredExpressionsList(layeredGroupsList)
            val oldToNewGroupIdMap = emptyMap<Long,Long>().toMutableMap()
            layeredGroupsList[0] = updateOverlappingGroupNames(parentGroups, layeredGroupsList.getOrElse(index = 0, defaultValue = {emptyList()}))
            layeredExpressionList[0] = updateOverlappingExpressionNames(parentExpressions, layeredExpressionList.getOrElse(index = 0, defaultValue = { emptyList() }))
            for (layerIndex in 0..layeredExpressionList.size) {
                var newGroupIds: List<Long> = emptyList()
                val updatedGroups = layeredGroupsList.getOrElse(index = layerIndex, defaultValue = { emptyList() }).fastMap { group -> group.copy(id = 0, parentId = if (layerIndex == 0) parentId else oldToNewGroupIdMap[group.parentId]) }
                val updatedExpressions = layeredExpressionList.getOrElse(index = layerIndex, defaultValue = { emptyList() }).fastMap { expression -> expression.copy(id = 0, parentId = if (layerIndex == 0) parentId else oldToNewGroupIdMap[expression.parentId]!!) }
                runBlocking(Dispatchers.IO){
                    if (updatedGroups.isNotEmpty()) newGroupIds = repository.upsertGroups(updatedGroups)
                    if (updatedExpressions.isNotEmpty())repository.upsertExpressions(updatedExpressions)
                }
                if (newGroupIds.isNotEmpty()) oldToNewGroupIdMap.putAll(newGroupIds.fastMapIndexed{ i, newGroupId -> layeredGroupsList[layerIndex][i].id to newGroupId})
            }
        }


    }
    private fun updateOverlappingGroupNames(parentGroups: List<Group>, rootGroups: List<Group>): List<Group> {
        if (parentGroups.isEmpty() || rootGroups.isEmpty()) return rootGroups
        val parentGroupNames = parentGroups.fastMap{group -> group.name}
        if (parentGroupNames.fastAll { groupName -> rootGroups.fastAll { group -> groupName.contentEquals(group.name)  } }) return rootGroups
        return rootGroups.fastMap { group ->
            var count = 0
            var name = group.name
            while (parentGroupNames.contains(name)){
                count ++
                name = "${group.name}_$count"
            }
            group.copy(name = name)
        }
    }
    private fun updateOverlappingExpressionNames(parentExpressions: List<Expression>, rootExpressions: List<Expression>): List<Expression> {
        if (parentExpressions.isEmpty() || rootExpressions.isEmpty()) return rootExpressions
        val parentExpressionNames = parentExpressions.fastMap{ expression -> expression.name}
        if (parentExpressionNames.fastAll { expressionName -> rootExpressions.fastAll { expression -> expressionName.contentEquals(expression.name)  } }) return rootExpressions
        return rootExpressions.fastMap { expression ->
            var count = 0
            var name = expression.name
            while (parentExpressionNames.contains(name)){
                count ++
                name = "${expression.name}_$count"
            }
            expression.copy(name = name)
        }
    }

    private fun getDirectDescendants(parentId: Long): Pair<List<Group>, List<Expression>> {
        val parentToGroupMapState = repository.groupMap.value.map{ entry -> entry.value.parentId to entry.value }.groupBy(keySelector = {pair -> pair.first}, valueTransform = {pair -> pair.second})
        val parentToExpressionMapState = repository.expressionMap.value.map{ entry -> entry.value.parentId to entry.value }.groupBy(keySelector = {pair -> pair.first}, valueTransform = {pair -> pair.second})
        return Pair(parentToGroupMapState[parentId] ?: emptyList(), parentToExpressionMapState[parentId] ?: emptyList())
    }

    private fun getLayeredGroupsList(): MutableList<List<Group>> {
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
        return layeredGroupsList
    }
    private fun getLayeredExpressionsList(layeredGroupsList: List<List<Group>>): MutableList<List<Expression>> {
        val rootExpressions = deepExpressions.fastFilter { expression -> !deepGroups.fastAny { group -> expression.parentId == group.id } }
        val parentIdToLayerMap = layeredGroupsList.fastMapIndexed { layerIndex, layer -> layer.fastMap { group -> group.id to layerIndex + 1 }  }.flatten().toMap()
        val layeredExpressionList = mutableListOf(rootExpressions.toMutableList())
        layeredGroupsList.fastForEach{layeredExpressionList.add(mutableListOf())}
        deepExpressions.fastFilter { expression -> !rootExpressions.contains(expression) }.fastForEach { expression ->
            val layerIndex = parentIdToLayerMap[expression.parentId]!!
            layeredExpressionList[layerIndex].add(expression)
        }
        return layeredExpressionList.fastMap { layer -> layer }.toMutableList()
    }
    private fun getGroupDescendants(group: Group, unassignedGroups: List<Group>): List<Group> {
        return unassignedGroups.fastFilter { otherGroup -> group.id == otherGroup.parentId }
    }
}