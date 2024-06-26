package viewModel

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import model.dataObjects.Expression
import model.dataObjects.Group
import model.dataObjects.GroupWithChildren
import model.database.AppRepository
import model.database.getUpdatedDependentExpressionReferences
import model.database.updateDependentExpressions
import model.database.updateGroupDependents
import model.parser.evaluator.ShuntingYardParser

class GroupScreenViewModel(
    val id: Long,
    val repository: AppRepository
): ViewModel() {

    private val _groupWithChildren: StateFlow<GroupWithChildren?> = repository.getGroupWithChildren(id).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val isRootState: StateFlow<Boolean> = _groupWithChildren.map { groupWithChildren -> groupWithChildren?.parentId == null }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    val fullPathState: StateFlow<String> = repository.groupIdToFullPathMap.map { fullpathMap -> fullpathMap[id] ?: "//null_fullpath" } .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "//default_fullpath"
    )

    val childExpressionsMap: StateFlow<Map<Long, Expression>> = _groupWithChildren.map{ groupWithChildren ->
        groupWithChildren?.childExpressions?.forEach { expression ->
            _expressionTextFieldStateMap[expression.id]?.let { textFieldState ->
                val text = textFieldState.text.toString()
                if (text != expression.text) {
                    println("EXPRESSION TEXT HOT UPDATE: ${expression.text}")
                    textFieldState.edit { replace(0, text.length, expression.text) }

                }
            }
        }
        (groupWithChildren?.childExpressions ?: emptyList())
            .associateBy{ expression ->
                expression.id
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )
    val childGroupsMap: StateFlow<Map<Long, Group>> =_groupWithChildren.map{ groupWithChildren ->
        (groupWithChildren?.childGroups ?: emptyList())
            .associateBy{ expressionGroup ->
                expressionGroup.id
            }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )

    val unhandledErrorState: Pair<String,Error>? = null

    private val _expressionTextFieldStateMap: MutableMap<Long, TextFieldState> = mutableMapOf()

    fun getExpressionTextFieldState(expressionId: Long): TextFieldState {
        if (_expressionTextFieldStateMap[expressionId] == null) _expressionTextFieldStateMap[expressionId] = TextFieldState(initialText = childExpressionsMap.value[expressionId]?.text ?: "0")
        return _expressionTextFieldStateMap[expressionId]!!
    }

    private val _groupNameTextFieldStateMap: MutableMap<Long, TextFieldState> = mutableMapOf()
    fun getGroupNameTextFieldState(expressionGroupId: Long): TextFieldState {
        if (_groupNameTextFieldStateMap[expressionGroupId] == null) _groupNameTextFieldStateMap[expressionGroupId] = TextFieldState(initialText = childGroupsMap.value[expressionGroupId]?.name ?: "default_name")
        return _groupNameTextFieldStateMap[expressionGroupId]!!
    }

    private val _expressionNameTextFieldStateMap: MutableMap<Long, TextFieldState> = mutableMapOf()
    fun getExpressionNameTextFieldState(expressionId: Long): TextFieldState {
        if (_expressionNameTextFieldStateMap[expressionId] == null) _expressionNameTextFieldStateMap[expressionId] = TextFieldState(initialText = childExpressionsMap.value[expressionId]?.name ?: "default_name")
        return _expressionNameTextFieldStateMap[expressionId]!!
    }

    private val _isExpressionCollapsedStateMap: MutableMap<Long, MutableTransitionState<Boolean>> = mutableMapOf()


    fun getIsExpressionCollapsedState(expressionId: Long): MutableTransitionState<Boolean> {
        if (_isExpressionCollapsedStateMap[expressionId] == null) _isExpressionCollapsedStateMap[expressionId] = MutableTransitionState(true)
        return _isExpressionCollapsedStateMap[expressionId]!!
    }




    fun addChildExpression(){
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertExpression(
                Expression(
                    parentId = id,
                    name = "E_${ childExpressionsMap.value.size }",
                )
            )
        }

    }
    fun addChildExpressionGroup(){
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertGroup(
                Group(
                    parentId = id,
                    name = "G_${ childGroupsMap.value.size }",
                    templateName = ""
                )
            )
        }
    }

    fun updateGroupName(groupId: Long) {
        if ( _groupNameTextFieldStateMap[groupId]?.text.isNullOrBlank() ) return
        if ( childGroupsMap.value[groupId]?.name == _groupNameTextFieldStateMap[groupId]?.text.toString()) return
        viewModelScope.launch(Dispatchers.IO) {
            val name = getGroupNameTextFieldState(groupId).text.toString()
            val group = childGroupsMap.value[groupId]!!.copy(name = name)
            repository.upsertGroup(group)
            val oldFullPath = repository.groupIdToFullPathMap.value[group.id]!!
            val newFullPath = oldFullPath.replaceAfterLast('/', group.name)
            repository.updateGroupDependents(
                groupId = group.id,
                oldFullPath = oldFullPath,
                newFullPath = newFullPath
            )

        }

    }



    fun deleteGroup(groupId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val expressionGroup = childGroupsMap.value[groupId] ?: return@launch
            _groupNameTextFieldStateMap.remove(groupId)
            repository.deleteGroup(expressionGroup)
        }
    }

    fun updateExpressionName(expressionId: Long) {
        if ( _expressionTextFieldStateMap[expressionId]?.text.isNullOrBlank() ) return
        if ( childExpressionsMap.value[expressionId]?.name == _expressionNameTextFieldStateMap[expressionId]?.text.toString()) return
        viewModelScope.launch(Dispatchers.IO) {
            val name = getExpressionNameTextFieldState(expressionId).text.toString()
            var expression = childExpressionsMap.value[expressionId]!!
            val oldFullPath = repository.expressionIdToFullPathMap.value[expressionId]!!
            expression = expression.copy(name = name)
            repository.upsertExpression(expression, false)
            repository.getUpdatedDependentExpressionReferences(
                expression = expression,
                oldFullPath = oldFullPath.drop(1),
                newFullPath = oldFullPath.replaceAfterLast('/', name).drop(1),
                updateRepository = true
            )
        }
    }
    fun deleteExpression(expressionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val expression = childExpressionsMap.value[expressionId] ?: return@launch
            _expressionTextFieldStateMap.remove(expressionId)
            _expressionNameTextFieldStateMap.remove(expressionId)
            repository.deleteExpression(expression)
        }
    }
    fun updateExpressionText(expressionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldText = childExpressionsMap.value[expressionId]?.text
            val name = getExpressionNameTextFieldState(expressionId).text.toString()
            val rawText = getExpressionTextFieldState(expressionId).text.toString()
            var expression = childExpressionsMap.value[expressionId]!!
            try {
                val parseResult = ShuntingYardParser.evaluate(
                    id = expressionId,
                    rawText = rawText,
                    getGroupPath = { exprId ->
                        repository.getExpressionGroupPath(exprId)
                    },
                    getExpressionId = { expressionFullPath: String ->
                        repository.expressionFullPathToIdMap.value[expressionFullPath]
                    },
                    getExpression = { exprId: Long ->
                        repository.expressionMap.value[exprId]
                    },
                    getOverlappingDependencies = {exprId, otherIds -> repository.getOverlappingDependencies(exprId, otherIds) }
                )
                expression = expression.copy(name = name, text = rawText, parseResult =  parseResult, updated = true)
                repository.upsertExpression(expression)
                if (oldText != rawText) repository.updateDependentExpressions(expression = expression)
            } catch (error: Error) {
                println(error)
            }

        }
    }






}
@Suppress("UNCHECKED_CAST")
class ExpressionGroupScreenViewModelFactory(
    val id: Long,
    val repository: AppRepository
    //val expressionGroupState: ExpressionGroup
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        println("EGSVM - CREATED ( $id )")
        return GroupScreenViewModel(
            id = id,
            repository = repository
        ) as T
    }
}