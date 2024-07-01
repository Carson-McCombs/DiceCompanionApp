package view.expressionGroupScreen

import android.content.res.Configuration
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import model.RegexPatterns
import model.dataObjects.Group
import view.resuseable.CheckableContainer
import view.resuseable.RegexInputTransformationFilter
import view.ui.theme.DiceCompanionTheme
import view.ui.theme.defaultIconButtonPadding
import view.ui.theme.defaultIconSize
import view.ui.theme.itemTitleHeight
import view.ui.theme.nameTextFieldStatePadding


@Composable
fun ChildGroupView(
    modifier: Modifier = Modifier,
    group: Group,
    nameTextFieldState: TextFieldState,
    selectionMode: MutableTransitionState<Boolean>,
    selectionState: MutableState<Boolean>,
    navigateTo: (Long) -> Unit,
    updateName: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource =
        remember(group.id, "groupInteractionSource") { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val nameFilterInputTransformation = remember("nameRegex"){ RegexInputTransformationFilter(
        RegexPatterns.nameRegex) }
    CheckableContainer(
        modifier = modifier,
        state = selectionState,
        checkboxVisibilityState = selectionMode
    ) {
        Card(
            modifier = Modifier.wrapContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.secondary
            ),
            //onStartToEnd = delete,
            //enableDismissFromEndToStart = false,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemTitleHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(0.dp))
                BasicTextField(
                    modifier = Modifier
                        .padding(start = nameTextFieldStatePadding)
                        .wrapContentHeight(),
                    state = nameTextFieldState,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    ),
                    inputTransformation = nameFilterInputTransformation,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimaryContainer),
                    interactionSource = interactionSource,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    onKeyboardAction = {
                        if (isFocused) {
                            updateName()
                            focusManager.clearFocus()
                        }
                    }
                )
                IconButton(
                    modifier = Modifier
                        .padding(defaultIconButtonPadding)
                        .wrapContentSize(),
                    onClick = { navigateTo(group.id) }
                ) {
                    Icon(
                        modifier = Modifier.size(defaultIconSize),
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Navigates to Group"
                    )
                }
            }
        }
    }

}

@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Light Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun ChildGroupView_Preview() {
    val group = Group(
        name = "preview_group",
        parentId = 0L,
        templateName = ""
    )
    DiceCompanionTheme {
        Surface {
            ChildGroupView(
                group = group,
                nameTextFieldState = TextFieldState(initialText = group.name),
                selectionMode = remember{ MutableTransitionState( false ) },
                selectionState = remember { mutableStateOf( false ) },
                navigateTo = {},
                updateName = {},
            )
        }

    }
}
