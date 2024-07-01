package viewModel

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import model.dataObjects.ClipboardReference

data class ExpressionEvents (
    val getNameTextFieldState: (Long) -> TextFieldState,
    val getTextFieldState: (Long) -> TextFieldState,
    val getIsCollapsedState: (Long) -> MutableTransitionState<Boolean>,
    val updateName: (Long) -> Unit,
    val updateText: (Long) -> Unit,
    val delete: (Long) -> Unit,
    val getSelectionState: (Long) -> MutableState<Boolean>
) {
    companion object{
        fun fromViewModel(viewModel: GroupScreenViewModel, clipboardReference: ClipboardReference): ExpressionEvents =
            ExpressionEvents(
                getNameTextFieldState = { expressionId ->  viewModel.getExpressionNameTextFieldState(expressionId) },
                getTextFieldState = { expressionId -> viewModel.getExpressionTextFieldState(expressionId) },
                getIsCollapsedState = { expressionId -> viewModel.getIsExpressionCollapsedState(expressionId) },
                updateName = { expressionId -> viewModel.updateExpressionName(expressionId) },
                updateText = { expressionId -> viewModel.updateExpressionText(expressionId) },
                delete = { expressionId -> viewModel.deleteExpression(expressionId) },
                getSelectionState = { expressionId -> clipboardReference.getExpressionSelectionState(expressionId)}
            )

    }
}