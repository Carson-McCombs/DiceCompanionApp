package viewModel

import androidx.compose.foundation.text.input.TextFieldState

data class GroupEvents (
    val getNameTextFieldState: (Long) -> TextFieldState,
    val updateName: (Long) ->  Unit,
    val delete: (Long) -> Unit
){
    companion object{
        fun fromViewModel(viewModel: GroupScreenViewModel): GroupEvents =
            GroupEvents(
                getNameTextFieldState = { expressionGroupId -> viewModel.getGroupNameTextFieldState(expressionGroupId) },
                updateName = { expressionGroupId -> viewModel.updateGroupName(expressionGroupId) },
                delete = { expressionGroupId -> viewModel.deleteGroup(expressionGroupId) }
            )
    }
}
