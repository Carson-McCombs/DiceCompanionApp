package viewModel

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import model.dataObjects.ClipboardReference

data class GroupEvents (
    val getNameTextFieldState: (Long) -> TextFieldState,
    val updateName: (Long) ->  Unit,
    val delete: (Long) -> Unit,
    val getSelectionState: (Long) -> MutableState<Boolean>
){
    companion object{
        fun fromViewModel(viewModel: GroupScreenViewModel, clipboardReference: ClipboardReference): GroupEvents =
            GroupEvents(
                getNameTextFieldState = { groupId -> viewModel.getGroupNameTextFieldState(groupId) },
                updateName = { groupId -> viewModel.updateGroupName(groupId) },
                delete = { groupId -> viewModel.deleteGroup(groupId) },
                getSelectionState = { groupId -> clipboardReference.getGroupSelectionState(groupId)}
            )
    }
}
