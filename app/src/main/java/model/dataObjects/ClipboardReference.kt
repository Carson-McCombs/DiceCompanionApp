package model.dataObjects

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import model.database.AppRepository

class ClipboardReference(
    private val scope: CoroutineScope,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val repository: AppRepository
) {

    private val expressionSelectionStateMap: MutableMap<Long, MutableState<Boolean>> = mutableMapOf()
    private val groupSelectionStateMap: MutableMap<Long, MutableState<Boolean>> = mutableMapOf()

    private var rootExpressionIds: MutableList<Long> = mutableListOf()
    private var rootGroupIds: MutableList<Long> = mutableListOf()

    private var deepExpressionsMap: Map<Long,Expression> = emptyMap()
    private var deepGroupsMap: Map<Long,Group> = emptyMap()
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
    fun clear(resetState: Boolean = false) {
        if (resetState){
            expressionSelectionStateMap.values.forEach { state -> state.value = false }
            groupSelectionStateMap.values.forEach { state -> state.value = false }
        }

        rootExpressionIds.clear()
        rootGroupIds.clear()
        deepExpressionsMap = emptyMap()
        deepGroupsMap = emptyMap()
        deepExpressionIds = emptyList()
        deepGroupIds = emptyList()
    }
    fun delete(){
        clear()
        setRootReferences()
        rootExpressionIds.fastForEach { expressionId -> expressionSelectionStateMap.remove(expressionId) }
        rootGroupIds.fastForEach { groupId -> groupSelectionStateMap.remove(groupId) }
        runBlocking (defaultDispatcher) {
            repository.deleteExpressions(rootExpressionIds)
            repository.deleteGroups(rootGroupIds)
        }

        clear(true)

    }
    fun copy(){
        populateReferences()
    }

    private fun setRootReferences(){
        rootExpressionIds = expressionSelectionStateMap.filterValues { state -> state.value }.map { entry -> entry.key }.toMutableList()
        rootGroupIds = groupSelectionStateMap.filterValues { state -> state.value }.map { entry -> entry.key }.toMutableList()
    }
    private fun populateReferences(){
        clear()
        setRootReferences()
        getDeepReferenceIds()
        getDeepReferences()
    }
    private fun getDeepReferenceIds(){
        val groupDescendantsMapState = repository.groupDescendantsMap.value
        val groupExpressionDescendantsMapState = repository.groupExpressionDescendantsMap.value
        deepGroupIds = (rootGroupIds + rootGroupIds.fastFlatMap { groupId -> groupDescendantsMapState[groupId]?: emptyList() }).distinct()
        deepExpressionIds = (rootExpressionIds + rootGroupIds.fastFlatMap { groupId -> groupExpressionDescendantsMapState[groupId]?: emptyList() }).distinct()
    }
    private fun getDeepReferences(){
        val groupMapState = repository.groupMap.value
        val expressionMapState = repository.expressionMap.value
        //deepGroups = deepGroupIds.fastMap { groupId -> groupMapState[groupId]!! }
        //deepExpressions = deepExpressionIds.fastMap { expressionId -> expressionMapState[expressionId]!! }
        deepGroupsMap = deepGroupIds.associateWith { groupId -> groupMapState[groupId]!! }
        deepExpressionsMap = deepExpressionIds.associateWith { expressionId -> expressionMapState[expressionId]!! }
    }
    fun pasteTo(parentId: Long) {
        scope.launch (defaultDispatcher){
            if (deepGroupIds.isEmpty() && deepExpressionIds.isEmpty()) return@launch
            val (parentGroups, parentExpressions) = getDirectDescendants(parentId)

            val layeredGroupIds = getLayeredGroupsList(parentId)
            val layeredExpressionIds = getLayeredExpressionsList(parentId, layeredGroupIds)



            //update overlapping names
            val rootGroups = updateOverlappingGroupNames(parentId, parentGroups, rootGroupIds.fastMap { groupId -> deepGroupsMap[groupId]!! })
            val rootExpressions = updateOverlappingExpressionNames(parentId, parentExpressions, rootExpressionIds.fastMap { groupId -> deepExpressionsMap[groupId]!! })

            val oldToNewGroupIdMap = mutableListOf(upsertCopiedLayer(rootGroups, rootExpressions).fastMapIndexed { i, newGroupId -> rootGroupIds[i] to newGroupId }.toMap())
            for (layerIndex in 1..layeredExpressionIds.size) {
                val copiedGroups = layeredGroupIds.getOrElse(index = layerIndex, defaultValue = { emptyList() }).fastMap { groupIdSet ->
                    deepGroupsMap[groupIdSet.id]!!.copy(
                        id = 0,
                        parentId = oldToNewGroupIdMap[layerIndex-1][groupIdSet.parentId]
                    )
                }
                val copiedExpressions = layeredExpressionIds.getOrElse(index = layerIndex, defaultValue = { emptyList() }).fastMap { expressionIdSet ->
                    deepExpressionsMap[expressionIdSet.id]!!.copy(
                        id = 0,
                        parentId =  oldToNewGroupIdMap[layerIndex-1][expressionIdSet.parentId]!!
                    )
                }
                val newGroupIds = upsertCopiedLayer(copiedGroups, copiedExpressions)
                val newGroupIdMap = layeredGroupIds.getOrElse(index = layerIndex, defaultValue = { emptyList() }).fastMapIndexed{ i, oldGroupIdSet -> newGroupIds[i] to oldGroupIdSet.id}.toMap()
                oldToNewGroupIdMap.add(newGroupIdMap)
            }
        }

    }

  /*  fun pasteTo(parentId: Long) {
        scope.launch (defaultDispatcher){
            if (deepGroupIds.isEmpty() && deepExpressionIds.isEmpty()) return@launch
            val (parentGroups, parentExpressions) = getDirectDescendants(parentId)

            val layeredGroupsList = getLayeredGroupsList()
            val layeredExpressionList = getLayeredExpressionsList(layeredGroupsList)

            val oldToNewGroupIdMap = emptyMap<Long,Long>().toMutableMap()

            //update overlapping names
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

    }*/

    private suspend fun upsertCopiedLayer(copiedGroups: List<Group>, copiedExpression: List<Expression>): List<Long> {
         return withContext(defaultDispatcher){
            if (copiedExpression.isNotEmpty()) repository.upsertExpressions(copiedExpression)
            if (copiedGroups.isNotEmpty()) return@withContext repository.upsertGroups(copiedGroups)
            return@withContext emptyList()
        }

    }


    private suspend fun updateOverlappingGroupNames(parentId: Long, parentGroups: List<Group>, rootGroups: List<Group>): List<Group> {
        return withContext(defaultDispatcher){
            val copiedRootGroups = rootGroups.fastMap { group -> group.copy(id = 0, parentId = parentId) }
            if (parentGroups.isEmpty() || rootGroups.isEmpty()) return@withContext copiedRootGroups
            val parentGroupNames = parentGroups.fastMap{group -> group.name}.toMutableList()
            if (rootGroups.fastAll { group -> parentGroupNames.fastAll { groupName -> !groupName.contentEquals(group.name) }}) return@withContext copiedRootGroups
            return@withContext copiedRootGroups.fastMap { group ->
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
    private suspend fun updateOverlappingExpressionNames(parentId: Long, parentExpressions: List<Expression>, rootExpressions: List<Expression>): List<Expression> {
        return withContext(defaultDispatcher){
            val copiedRootExpressions = rootExpressions.fastMap { expression -> expression.copy(id = 0, parentId = parentId) }
            if (parentExpressions.isEmpty() || rootExpressions.isEmpty()) return@withContext copiedRootExpressions
            val parentExpressionNames = parentExpressions.fastMap{ expression -> expression.name}.toMutableList()
            if (rootExpressions.fastAll { expression -> parentExpressionNames.fastAll { expressionName -> !expressionName.contentEquals(expression.name)  } }) return@withContext copiedRootExpressions
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
    /*private suspend fun getLayeredGroupsList(): MutableList<List<Group>> {
        return withContext(defaultDispatcher){
            val rootGroups = deepGroups.fastFilter { group -> rootGroupIds.contains(group.id) }
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

    }*/
    private suspend fun getLayeredGroupsList(parentId: Long): MutableList<List<IdSet>> {
        return withContext(defaultDispatcher){
            val rootGroupIds = rootGroupIds
            val layeredGroupsList = mutableListOf(rootGroupIds.fastMap { id -> IdSet(parentId, id) })
            val groupIdSetMap = deepGroupsMap.values.map { group ->
                IdSet(
                    group.parentId!!,
                    group.id
                )
            }.groupBy { idSet ->
                idSet.parentId
            }

            val queue = ArrayDeque(listOf(layeredGroupsList[0]))
            var layerNumber = 1
            while (queue.isNotEmpty()) {
                val layer = queue.removeFirst()
                val nextLayer = emptyList<IdSet>().toMutableList()
                for (idSet in layer) {
                    nextLayer.addAll( groupIdSetMap[idSet.id] ?: emptyList())
                }
                if (nextLayer.isEmpty()) break
                queue.add(nextLayer)
                layeredGroupsList.add(nextLayer)
                layerNumber ++
                if (layerNumber > deepGroupsMap.size) break
            }
            return@withContext layeredGroupsList
        }

    }
   /* private suspend fun getLayeredExpressionsList(layeredGroupsList: List<List<Group>>): MutableList<List<Expression>> {
        return withContext(defaultDispatcher) {
            val rootExpressions = deepExpressions.fastFilter { expression -> rootExpressionIds.contains(expression.id) }

            val parentIdToLayerMap = layeredGroupsList.fastMapIndexed { layerIndex, layerGroups -> layerGroups.fastMap { group -> group.id to layerIndex + 1 }  }.flatten().toMap()
            val layeredExpressionList = mutableListOf(rootExpressions.toMutableList())

            layeredGroupsList.fastForEach{ layeredExpressionList.add(mutableListOf()) }
            deepExpressions.fastFilter { expression -> !rootExpressions.contains(expression) }.fastForEach { expression ->
                val layerIndex = parentIdToLayerMap[expression.parentId]!!
                layeredExpressionList[layerIndex].add(expression)
            }

            return@withContext layeredExpressionList.fastMap { layer -> layer }.toMutableList()
        }

    }*/
    private suspend fun getLayeredExpressionsList(parentId: Long, layeredGroupIds: List<List<IdSet>>): MutableList<List<IdSet>> {
        return withContext(defaultDispatcher) {
            val layeredExpressionIds = mutableListOf(rootExpressionIds.fastMap{ id -> IdSet(parentId, id)})

            layeredGroupIds.fastForEach { layeredExpressionIds.add(mutableListOf()) }
            val expressionIdSetMap = deepExpressionsMap.values.map { expression ->
                IdSet(
                    expression.parentId,
                    expression.id
                )
            }.groupBy{ idSet ->
                idSet.parentId
            }
            layeredGroupIds.fastForEachIndexed { layerIndex, layerGroupIds ->
                val nextLayerExpressions = layerGroupIds.fastFlatMap { groupIdSet -> expressionIdSetMap[groupIdSet.id] ?: emptyList() }
                layeredExpressionIds[layerIndex + 1] = nextLayerExpressions
            }
            return@withContext layeredExpressionIds
        }

    }
/*    private fun getGroupDescendants(group: Group, unassignedGroups: List<Group>): List<Group> {
        return unassignedGroups.fastFilter { otherGroup -> group.id == otherGroup.parentId }
    }*/


    private data class IdSet (
        val parentId: Long,
        val id: Long
    )
}