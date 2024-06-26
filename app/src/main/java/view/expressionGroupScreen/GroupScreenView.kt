package view.expressionGroupScreen

import android.content.res.Configuration
import androidx.compose.animation.core.MutableTransitionState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import model.dataObjects.Expression
import model.dataObjects.Group
import model.dataObjects.ParseResult
import model.parser.evaluator.ShuntingYardParser
import model.parser.token.LiteralType
import view.ui.theme.DiceCompanionTheme
import viewModel.ExpressionEvents
import viewModel.GroupEvents
import viewModel.GroupScreenEvents
import viewModel.GroupScreenViewModel

@Composable
fun GroupScreenView(
    modifier: Modifier = Modifier,
    viewModel: GroupScreenViewModel,
    groupScreenEvents: GroupScreenEvents
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
        expressionEvents = remember(viewModel.id, "expressionEvents") { ExpressionEvents.fromViewModel(viewModel)},
        groupEvents = remember(viewModel.id, "expressionEvents") { GroupEvents.fromViewModel(viewModel) },
        groupScreenEvents = groupScreenEvents
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
    groupScreenEvents: GroupScreenEvents
){

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            ExpressionGroupScreenTopBar(
                fullpathState = fullpathState,
                isRoot = isRoot,
                navigateUp = groupScreenEvents.navigateUp,
                navigateToHelpScreen = groupScreenEvents.navigateToHelpScreen
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
                    paste = { groupScreenEvents.paste(id.value) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ){
            LazyColumn(
                modifier = Modifier.wrapContentHeight(),
            ) {
                item{
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(childGroupMapState.value.values.toList()){ expressionGroup ->
                    ChildExpressionGroupView(
                        modifier = Modifier.padding(4.dp),
                        group = expressionGroup,
                        navigateTo = groupScreenEvents.navigateTo,
                        nameTextFieldState = groupEvents.getNameTextFieldState(expressionGroup.id),
                        updateName = {
                            groupEvents.updateName(expressionGroup.id)
                        },
                        delete = {
                            groupEvents.delete(expressionGroup.id)
                        },
                        copy = {
                            groupScreenEvents.copy(expressionGroup.id, true)
                        }
                    )
                }
                items(childExpressionMapState.value.values.toList()){ expression ->
                    ChildExpressionView(
                        modifier = Modifier.padding(4.dp),
                        expression = expression,
                        nameTextFieldState = expressionEvents.getNameTextFieldState(expression.id),
                        expressionTextFieldState = expressionEvents.getTextFieldState(expression.id),
                        visibleState = expressionEvents.getIsCollapsedState(expression.id),
                        updateName = {
                            expressionEvents.updateName(expression.id)
                        },
                        updateExpressionText = {
                            expressionEvents.updateText(expression.id)
                        },
                        delete = {
                            expressionEvents.delete(expression.id)
                        },
                        copy = {
                            groupScreenEvents.copy(expression.id, false)
                        }
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primaryContainer),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = navigateUp,
            enabled = !isRoot.value
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Navigates to the Parent Group",
            )
        }
        Text(
            text = fullpathState.value.drop(1),
            style = MaterialTheme.typography.headlineMedium
        )
        IconButton(
            onClick = navigateToHelpScreen
        ){
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Open Settings Window",
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
                    //path = "//testfullpath/testA",
                    text = "1+2",
                    parseResult = ParseResult(
                        result = 3,
                        resultType = LiteralType.INTEGER
                    )
                ),
                1L to Expression(
                    id = 1,
                    name = "testB",
                    //path = "//testfullpath/testB",
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
                    //path = "//testfullpath/testC",
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
                    //path = "//testfullpath",
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
            delete = {}
        )
    }
    val groupEvents = remember {
        GroupEvents(
            getNameTextFieldState = { expressionGroupId -> childGroupNameTextStateMap[expressionGroupId]!! },
            updateName = {},
            delete = {}
        )
    }
    DiceCompanionTheme {
        GroupScreenView(
            fullpathState = remember{ mutableStateOf("//testfullpath") },
            id = remember { mutableLongStateOf(0L) },
            isRoot = remember{ mutableStateOf(false) },
            childExpressionMapState = childExpressionMapState,
            childGroupMapState = childGroupMapState,
            addChildExpression = {},
            addChildGroup = {},
            expressionEvents = expressionEvents,
            groupEvents = groupEvents,
            groupScreenEvents = GroupScreenEvents()
        )
    }

}