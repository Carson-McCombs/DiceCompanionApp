package view.expressionGroupScreen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import model.RegexPatterns
import model.dataObjects.Expression
import model.dataObjects.ParseResult
import model.parser.token.LiteralType
import view.resuseable.DismissibleCard
import view.ui.theme.DiceCompanionTheme
import viewModel.EvaluationState


@Composable
fun ChildExpressionView(
    modifier: Modifier = Modifier,
    expression: Expression,
    nameTextFieldState: TextFieldState,
    expressionTextFieldState: TextFieldState,
    visibleState: MutableTransitionState<Boolean>,
    updateName: () -> Unit,
    updateExpressionText: () -> Unit,
    delete: () -> Unit,
    copy: () -> Unit,
) {
    Row(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Column{
            if (visibleState.currentState){
                TemporaryCopyButton (copy = copy)

            }
            ResultButton(
                enabled = !expression.parseResult.isStatic,
                evaluationState = expression.evaluationState,
                resultText = expression.resultText,
                updateExpressionText = updateExpressionText
            )
        }

        DismissibleCard(
            modifier = modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.secondary,
            ),
            elevation = CardDefaults.elevatedCardElevation(),
            onStartToEnd = delete,
            enableDismissFromEndToStart = false,
        ) {
            ChildExpressionView_TitleBar(
                expression = expression,
                nameTextFieldState = nameTextFieldState,
                visibleState = visibleState,
                updateName = updateName
            )
            AnimatedVisibility(
                visibleState = visibleState,
                enter = expandVertically(),
                exit = shrinkVertically()

            ) {
                ChildExpressionView_Body(
                    expression = expression,
                    expressionTextFieldState = expressionTextFieldState,
                    visibleState = visibleState,
                    updateExpressionText = updateExpressionText
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

@Composable
private fun ResultButton(
    resultText: String,
    enabled: Boolean,
    evaluationState: EvaluationState,
    updateExpressionText: () -> Unit,
) {
    val displayText = remember(evaluationState, resultText) { if (evaluationState != EvaluationState.ERROR) resultText else "!" }
    val buttonColors =  evaluationButtonColors(evaluationState)

    Button(
        modifier = Modifier
            .fillMaxWidth(.15f)
            .aspectRatio(1f)
            .padding(4.dp),
        enabled = enabled,
        onClick = updateExpressionText,
        shape = MaterialTheme.shapes.large,
        colors = buttonColors,
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            text = displayText,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )


    }
}


@Composable
private fun ChildExpressionView_TitleBar(expression: Expression, nameTextFieldState: TextFieldState, visibleState: MutableTransitionState<Boolean>, updateName: () -> Unit){
    val focusManager = LocalFocusManager.current
    val interactionSource = remember(expression.id, "expressionNameInteractionSource") { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val nameFilterInputTransformation = remember("nameRegex"){ RegexInputTransformationFilter(RegexPatterns.nameRegex) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .padding(4.dp)
            .zIndex(1f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.size(4.dp))
        BasicTextField(
            modifier = Modifier.wrapContentHeight(),
            state = nameTextFieldState,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.None
            ),
            inputTransformation =  nameFilterInputTransformation,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimaryContainer),
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = {
                if (isFocused) {
                    updateName()
                    focusManager.clearFocus()
                }
            },
        )
        IconButton(
            onClick = {
                visibleState.targetState = !visibleState.currentState
            }
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Opens expression editor",
            )
        }
    }
}

@Composable
private fun ChildExpressionView_Body(expression: Expression, expressionTextFieldState: TextFieldState, visibleState: MutableTransitionState<Boolean>, updateExpressionText: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val interactionSource = remember(expression.id, "expressionTextInteractionSource") { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = if (visibleState.currentState) 256.dp else 0.dp)
            .padding(8.dp)
            .zIndex(-1f)
    ) {
        if (expression.parseResult.errorText.isNotBlank()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .horizontalScroll(scrollState),
                text = expression.parseResult.errorText,
                color = MaterialTheme.colorScheme.error,
                maxLines = 4,
                softWrap = true,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(4.dp),
            decorator = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(8.dp),

                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    innerTextField()
                }
            },
            state = expressionTextFieldState,
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = {
                if (isFocused) {
                    updateExpressionText()
                    focusManager.clearFocus()
                }
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimaryContainer),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        )
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
private fun ChildExpressionView_Preview(){
    Column() {
        ChildExpressionView_Preview_Internal(isVisible = true)
        ChildExpressionView_Preview_Internal(isVisible = false)
    }

}

@Composable
private fun ChildExpressionView_Preview_Internal(isVisible: Boolean = true){
    DiceCompanionTheme {
        Surface{
            val expression = Expression(
                id = -1,
                parentId = 0L,
                name = "Text_Expression",
            )
            val expressions = listOf(
                expression.copy(
                    text = "123",
                    parseResult = ParseResult(
                        resultType = LiteralType.INTEGER,
                        result = 123,
                    )
                ),
                expression.copy(
                    text = "roll(1,8)",
                    parseResult = ParseResult(
                        resultType = LiteralType.INTEGER,
                        result = 7,
                        isStatic = false
                    )
                ),
                expression.copy(
                    text = "roll(1,8) + @(other)",
                    parseResult = ParseResult(
                        result = 6,
                        resultType = LiteralType.INTEGER,
                        isStatic = false
                    ),
                    updated = false
                ),
                expression.copy(
                    text = ")(*(2",
                    parseResult = ParseResult(
                        resultType = LiteralType.NONE,
                        errorText = "Example Error Text"
                    )
                )
            )
            val visibleState = MutableTransitionState(isVisible)
            LazyColumn(
                modifier = Modifier.wrapContentHeight()
            ) {
                items(expressions) { expr ->
                    val nameTextFieldState = TextFieldState(initialText = expression.name)
                    val expressionTextFieldState = TextFieldState(initialText = expression.text)
                    ChildExpressionView(
                        modifier = Modifier
                            .padding(4.dp),
                        expression = expr,
                        nameTextFieldState = nameTextFieldState,
                        expressionTextFieldState = expressionTextFieldState,
                        visibleState = visibleState,
                        updateName = {},
                        updateExpressionText = {},
                        delete = {},
                        copy = {}
                    )
                }
            }

        }
    }
}