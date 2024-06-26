package view.expressionGroupScreen

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import view.resuseable.DismissibleCard
import view.ui.theme.DiceCompanionTheme


@Composable
fun ChildExpressionGroupView(
    modifier: Modifier = Modifier,
    group: Group,
    nameTextFieldState: TextFieldState,
    navigateTo: (Long) -> Unit,
    updateName: () -> Unit,
    delete: () -> Unit,
    copy: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val interactionSource =
        remember(group.id, "expressionGroupInteractionSource") { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val nameFilterInputTransformation = remember("nameRegex"){ RegexInputTransformationFilter(
        RegexPatterns.nameRegex) }
    DismissibleCard(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary
        ),
        onStartToEnd = delete,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TemporaryCopyButton(copy = copy)
            /*Spacer(
                modifier = Modifier.size(1.dp)

            )*/
            BasicTextField(
                modifier = Modifier
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
                onClick = { navigateTo(group.id) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Navigates to Group"
                )
            }
        }
    }
}

@Composable
private fun TemporaryCopyButton(
    copy: () -> Unit
){
    Button(
        modifier = Modifier
            .fillMaxWidth(.15f)
            .aspectRatio(1f)
            .padding(4.dp),
        onClick = copy,
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(2.dp)
    ){
        Icon(imageVector = Icons.Default.Add, contentDescription = "Copies Expression")
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
            ChildExpressionGroupView(
                group = group,
                nameTextFieldState = TextFieldState(initialText = group.name),
                navigateTo = {},
                updateName = {},
                delete = {},
                copy = {}
            )
        }

    }
}
