package view.expressionGroupScreen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.carsonmccombs.dicecompanion.R
import model.dataObjects.ClipboardReference
import model.dataObjects.Expression
import model.dataObjects.Group
import model.dataObjects.ParseResult
import model.parser.evaluator.ShuntingYardParser
import model.parser.token.LiteralType
import view.ui.theme.DiceCompanionTheme
import view.ui.theme.defaultIconButtonPadding
import view.ui.theme.defaultIconSize
import viewModel.ExpressionEvents
import viewModel.GroupEvents
import viewModel.GroupScreenEvents
import viewModel.GroupScreenViewModel

@Composable
fun GroupScreenView(
    modifier: Modifier = Modifier,
    viewModel: GroupScreenViewModel,
    groupScreenEvents: GroupScreenEvents,
    selectionMode: MutableTransitionState<Boolean>,
    clipboardReference: ClipboardReference
) {
    val isRootState = viewModel.isRootState.collectAsState()
    val fullpathState = viewModel.fullPathState.collectAsState()
    val childExpressionMapState = viewModel.childExpressionsMap.collectAsState(emptyMap())
    val childGroupMapState = viewModel.childGroupsMap.collectAsState(emptyMap())
    GroupScreenView(
        modifier = modifier,
        id = remember { mutableLongStateOf(viewModel.id) },
        fullpathState = fullpathState,
        isRoot = isRootState,
        childExpressionMapState = childExpressionMapState,
        childGroupMapState = childGroupMapState,
        addChildExpression = { viewModel.addChildExpression() },
        addChildGroup = { viewModel.addChildExpressionGroup() },
        expressionEvents = remember(viewModel.id, "expressionEvents") { ExpressionEvents.fromViewModel(viewModel, clipboardReference)},
        groupEvents = remember(viewModel.id, "expressionEvents") { GroupEvents.fromViewModel(viewModel, clipboardReference) },
        groupScreenEvents = groupScreenEvents,
        selectionMode = selectionMode
    )
}

@Composable
private fun GroupScreenView(
    modifier: Modifier = Modifier,
    id: State<Long>,
    fullpathState: State<String>,
    isRoot: State<Boolean>,
    childExpressionMapState: State<Map<Long, Expression>>,
    childGroupMapState: State<Map<Long, Group>>,
    isExpandedState: MutableState<Boolean> = remember { mutableStateOf(false) },
    addChildExpression: () -> Unit,
    addChildGroup: () -> Unit,
    expressionEvents: ExpressionEvents,
    groupEvents: GroupEvents,
    groupScreenEvents: GroupScreenEvents,
    selectionMode: MutableTransitionState<Boolean>,
){

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            ExpressionGroupScreenTopBar(
                fullpathState = fullpathState,
                isRoot = isRoot,
                navigateUp = groupScreenEvents.navigateUp,
                navigateToHelpScreen = groupScreenEvents.navigateToHelpScreen,
                selectionMode = selectionMode,
                deleteSelection = groupScreenEvents.deleteSelection,
                copySelection = groupScreenEvents.copySelection,
                pasteSelection = { groupScreenEvents.pasteSelection(id.value) }
            )

        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    isExpandedState.value = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More Options",
                )
                AddChildDropDownMenu(
                    isExpandedState = isExpandedState,
                    addExpression = addChildExpression,
                    addGroup = addChildGroup,
                    paste = { groupScreenEvents.pasteSelection(id.value) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),

            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ){
            LazyColumn(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                item{
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(childGroupMapState.value.values.toList()){ expressionGroup ->
                    ChildGroupView(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        group = expressionGroup,
                        navigateTo = groupScreenEvents.navigateTo,
                        nameTextFieldState = groupEvents.getNameTextFieldState(expressionGroup.id),
                        selectionState = groupEvents.getSelectionState(expressionGroup.id),
                        selectionMode = selectionMode,
                        updateName = {
                            groupEvents.updateName(expressionGroup.id)
                        },
                        /*delete = {
                            groupEvents.delete(expressionGroup.id)
                        },*/

                    )
                }
                items(childExpressionMapState.value.values.toList()){ expression ->
                    ChildExpressionView(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        expression = expression,
                        nameTextFieldState = expressionEvents.getNameTextFieldState(expression.id),
                        expressionTextFieldState = expressionEvents.getTextFieldState(expression.id),
                        visibleState = expressionEvents.getIsCollapsedState(expression.id),
                        selectionState = expressionEvents.getSelectionState(expression.id),
                        selectionMode = selectionMode,
                        updateName = {
                            expressionEvents.updateName(expression.id)
                        },
                        updateExpressionText = {
                            expressionEvents.updateText(expression.id)
                        },
                        /*delete = {
                            expressionEvents.delete(expression.id)
                        }*/
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressionGroupScreenTopBar(
    isRoot: State<Boolean>,
    fullpathState: State<String>,
    navigateUp: () -> Unit,
    navigateToHelpScreen: () -> Unit,
    selectionMode: MutableTransitionState<Boolean>,
    copySelection: () -> Unit,
    pasteSelection: () -> Unit,
    deleteSelection: () -> Unit,
) {
    val isExpandedState = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .wrapContentSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(color = MaterialTheme.colorScheme.primaryContainer)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(defaultIconButtonPadding),
                onClick = navigateUp,
                enabled = !isRoot.value
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigates to the Parent Group",
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = fullpathState.value.drop(1),
                style = MaterialTheme.typography.headlineMedium
            )

            IconButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(defaultIconButtonPadding),
                onClick = {isExpandedState.value = true}
            ){
                Column {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Show more options",
                    )
                    MoreOptionsDropDownMenu(
                        isExpandedState = isExpandedState,
                        navigateToHelpScreen = navigateToHelpScreen,
                        startSelectionMode = { selectionMode.targetState = true }
                    )
                }

            }
        }
        AnimatedVisibility(
            modifier = Modifier.zIndex(0f),
            visibleState = selectionMode,
            enter = slideInVertically(initialOffsetY = { height -> -height }),
            exit = slideOutVertically(targetOffsetY = { height -> -height })
        ) {
            SelectionOptions(
                delete = deleteSelection,
                copy = copySelection,
                paste = pasteSelection,
                exitSelectionMode = { selectionMode.targetState = false }
            )
        }
    }


}
@Composable
private fun SelectionOptions(
    delete: () -> Unit,
    copy: () -> Unit,
    paste: () -> Unit,
    exitSelectionMode: () -> Unit
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MaterialTheme.colorScheme.inversePrimary)
            .padding(8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ){
        IconButton(
            modifier = Modifier
                .wrapContentSize()
                .padding(defaultIconButtonPadding),
            onClick = copy
        ) {
            Icon(
                modifier = Modifier.size(defaultIconSize),
                painter = painterResource(id = R.drawable.baseline_content_copy_24),
                contentDescription = "Copies to Clipboard"
            )
        }
        IconButton(
            modifier = Modifier
                .wrapContentSize()
                .padding(defaultIconButtonPadding),
            onClick = paste
        ) {
            Icon(
                modifier = Modifier.size(defaultIconSize),
                painter = painterResource(id = R.drawable.baseline_content_paste_24),
                contentDescription = "Pastes from Clipboard"
            )
        }
        IconButton(
            modifier = Modifier
                .wrapContentSize()
                .padding(defaultIconButtonPadding),
            onClick = delete
        ) {
            Icon(
                modifier = Modifier.size(defaultIconSize),
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Deletes Selection"
            )
        }
        IconButton(
            modifier = Modifier
                .wrapContentSize()
                .padding(defaultIconButtonPadding),
            onClick = exitSelectionMode
        ) {
            Icon(
                modifier = Modifier.size(defaultIconSize),
                imageVector = Icons.Default.Close,
                contentDescription = "Closes Selection"
            )
        }
    }
}

@Composable
private fun MoreOptionsDropDownMenu(
    isExpandedState: MutableState<Boolean>,
    navigateToHelpScreen: () -> Unit,
    startSelectionMode: () -> Unit
){
    if (isExpandedState.value) {
        DropdownMenu(
            expanded = isExpandedState.value,
            onDismissRequest = { isExpandedState.value = false },
            shape = MaterialTheme.shapes.medium,
            offset = DpOffset(x = 0.dp, y = 16.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Open Help Screen",
                    )
                },
                onClick = {
                    isExpandedState.value = false
                    navigateToHelpScreen()
                }
            )
            Spacer(modifier = Modifier.size(8.dp))
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Toggle Select Items",
                    )
                },
                onClick = {
                    isExpandedState.value = false
                    startSelectionMode()
                }

            )
        }
    }

}

@Composable
private fun AddChildDropDownMenu(
    isExpandedState: MutableState<Boolean>,
    addExpression: () -> Unit,
    addGroup: () -> Unit,
    paste: () -> Unit,
) {
    DropdownMenu(
        expanded = isExpandedState.value,
        onDismissRequest = { isExpandedState.value = false },
        shape = MaterialTheme.shapes.medium,
        offset = DpOffset(x = 0.dp, y = (-8).dp)
        ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = "Add Expression",
                )
            },
            onClick = addExpression
        )
        Spacer(modifier = Modifier.size(8.dp))
        DropdownMenuItem(
            text = {
                Text(
                    text = "Add Group",
                )
            },
            onClick = addGroup
        )
        Spacer(modifier = Modifier.size(8.dp))
        DropdownMenuItem(
            text = {
                Text(
                    text = "Paste",
                )
            },
            onClick = paste
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
private fun GroupScreenView_Preview() {
    val childExpressionMapState = remember{
        mutableStateOf(
            mapOf(
                0L to Expression(
                    id = 1+2,
                    name = "testA",
                    parentId = 1L,
                    text = "1+2",
                    parseResult = ParseResult(
                        result = 3,
                        resultType = LiteralType.INTEGER
                    )
                ),
                1L to Expression(
                    id = 1,
                    name = "testB",
                    parentId = 1L,
                    text = "@(testfullpath/testA)+1",
                    parseResult = ParseResult(
                        result = 4,
                        resultType = LiteralType.INTEGER
                    )
                ),
                2L to Expression(
                    id = 2,
                    name = "testC",
                    parentId = 1L,
                    text = "@(asdn)",
                    parseResult = ShuntingYardParser.evaluate(
                        id = 2,
                        getGroupPath = { "" },
                        rawText = "@(asdn)",
                        getExpressionId = { _ -> null },
                        getExpression = { null },
                        getOverlappingDependencies = { _, _ -> emptyList() }
                    )
                ),
            )
        )
    }
    val childGroupMapState = remember {
        mutableStateOf(
            mapOf(
                1L to Group(
                    id = 1,
                    name = "testD",
                    parentId = 0L,
                    templateName = ""
                )
            )
        )
    }
    val childExpressionNameTextStateMap = remember {
        childExpressionMapState.value.values.associate { expression ->
            expression.id to TextFieldState(
                initialText = expression.name
            )
        }
    }
    val childExpressionTextStateMap = remember {
        childExpressionMapState.value.values.associate { expression ->
            expression.id to TextFieldState(
                initialText = expression.text
            )
        }
    }
    val childGroupNameTextStateMap = remember {
        childGroupMapState.value.values.associate { group ->
            group.id to TextFieldState(
                initialText = group.name
            )
        }
    }
    val isCollapsedStateMap =
        mapOf(
            0L to MutableTransitionState(true),
            1L to MutableTransitionState(false)
        )

    val expressionEvents = remember {
        ExpressionEvents(
            getNameTextFieldState = { expressionId -> childExpressionNameTextStateMap[expressionId]!! },
            getTextFieldState = { expressionId -> childExpressionTextStateMap[expressionId]!! },
            getIsCollapsedState = { expressionId -> isCollapsedStateMap[expressionId]?: MutableTransitionState(true) },
            updateName = {},
            updateText = {},
            delete = {},
            getSelectionState = { mutableStateOf(false) }
        )
    }
    val groupEvents = remember {
        GroupEvents(
            getNameTextFieldState = { expressionGroupId -> childGroupNameTextStateMap[expressionGroupId]!! },
            updateName = {},
            delete = {},
            getSelectionState = { mutableStateOf(false) }
        )
    }
    DiceCompanionTheme {
        GroupScreenView(
            fullpathState = remember{ mutableStateOf("//testfullpath/test/tes/tes/test/estsetsetsetsetsssssssssssssssssssssssssssssssssss") },
            id = remember { mutableLongStateOf(0L) },
            isRoot = remember{ mutableStateOf(false) },
            childExpressionMapState = childExpressionMapState,
            childGroupMapState = childGroupMapState,
            addChildExpression = {},
            addChildGroup = {},
            expressionEvents = expressionEvents,
            groupEvents = groupEvents,
            groupScreenEvents = GroupScreenEvents(),
            selectionMode = remember { MutableTransitionState( true ) },
        )
    }

}