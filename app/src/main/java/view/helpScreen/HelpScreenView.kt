package view.helpScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import view.ui.theme.DiceCompanionTheme

@Composable
fun HelpScreenView(navigateUp: () -> Unit, navigateToGitHub: () -> Unit){
    val scrollState = rememberScrollState()
    val boldBodyLarge =  MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { HelpScreenViewTopBar(navigateUp, navigateToGitHub) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(8.dp)
        ){
            Text(
                text = "Getting Started:",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text =  "    First thing to know is that there are two object types: " +
                        "Expressions and Groups. \n\n" +
                        "    An Expression contains a text field " +
                        "where a mathematical expression can be entered that can be " +
                        "evaluated for a result. An Expression can either have a static " +
                        "or a dynamic value. If an Expression is dynamic, that means that " +
                        "everytime you evaluate the expression it will result in a new " +
                        "value, and all other Expressions that reference it will also be " +
                        "dynamic. An Expression can reference any other Expressions " +
                        "that doesn't reference itself ( either directly or indirectly ). " +
                        "When an Expression has its text changed, it will update all " +
                        "of the static Expressions that reference it and will " +
                        "tag all dynamic Expressions that one of its dependants has changed.\n\n" +
                        "A Group is a collection of Expressions and other Groups. " +
                        "this can be used to hide and organize your information. Although " +
                        "more features are planned for future releases. \n",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Expression Syntax: ",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Names of Expressions and Groups are case-sensitive and can only be " +
                        "made up of alphanumeric characters, -hyphens, and  _underscores.\n",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "\"@(Group_FullPath/Expression_Name)\": ",
                textAlign = TextAlign.Start,
                style = boldBodyLarge
            )
            Text(
                text =  "This will reference the value of the Expression at that path. " +
                        "The fullpath of an Expression is determined by the Group fullpath, " +
                        "found at the top of the screen, and the name of the Expression.\n " +
                        "For example, the fullpath \"@(Character/Stats/Strength)\" will look for an Expression " +
                        "named \"Strength\" located in a Group named \"Stats\", which is " +
                        "located in a Group named \"Character\".",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "\"@(../Expression_Name)\": ",
                textAlign = TextAlign.Start,
                style = boldBodyLarge
            )
            Text(
                text =  "This is the syntax for a Local or Relative Expression Reference. " +
                        "Where the path of the Group containing the Expression replaces " +
                        "the \"..\". For example, if I have a group named \"GroupA\" which " +
                        "contains two Expressions: \"ExpressionA\" and \"ExpressionB\", " +
                        "ExpressionB can use \"@(../ExpressionA)\", which is the same as " +
                        "saying \"@(GroupA/ExpressionA), to reference ExpressionA. This is " +
                        "mainly useful simplifying references and for " +
                        "Copying / Pasting / Templating Expressions and Groups.",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "\"floor( x )\": ",
                textAlign = TextAlign.Start,
                style = boldBodyLarge
            )
            Text(
                text =  "This will round the \"x\" value down to the nearest Integer.\n For example, \n" +
                        "\"ceil( 2.8 )\" = 2.\n",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "\"ceil( x )\": ",
                textAlign = TextAlign.Start,
                style = boldBodyLarge
            )
            Text(
                text =  "This will round the \"x\" value up to the nearest Integer. \n" +
                        "For example, \"ceil( 2.1 )\" = 3.\n",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "\"round( x )\": ",
                textAlign = TextAlign.Start,
                style = boldBodyLarge
            )
            Text(
                text =  "This will round the \"x\" value to the nearest Integer, rounding on " +
                        "ties, where the decimal value is 0.5. \n" +
                        "For example, \"round( 2.2 )\" = 2, " +
                        "\"round( 2.5 )\" = 3, \"round( 2.6 )\" = 3.\n",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "\"min( x, y )\": ",
                textAlign = TextAlign.Start,
                style = boldBodyLarge
            )
            Text(
                text =  "This will return the lower of the two values. \n" +
                        "For example, \"max( 12, 8 )\" = 8.\n",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "\"max( x, y )\": ",
                textAlign = TextAlign.Start,
                style = boldBodyLarge
            )
            Text(
                text =  "This will return higher of the two values. \n" +
                        "For example, \"max( 12, 8 )\" = 12.\n",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "\"random( x, y )\": ",
                textAlign = TextAlign.Start,
                style = boldBodyLarge
            )
            Text(
                text =  "This will return a pseudo-random number between x (inclusive) and y (exclusive). " +
                        "This will also cause the Expression to be dynamic as this function will return " +
                        "a new value each time it is called. \n" +
                        "For example, random ( 0, 8 ) can return any of the " +
                        "following values: [0, 1, 2, 3 ,4 , 5, 6, 7].\n",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "\"roll( x, y )\": ",
                textAlign = TextAlign.Start,
                style = boldBodyLarge
            )
            Text(
                text =  "This will return a roll \"y\" sided dice ( a random number between 1 (inclusive) and " +
                        "\"y\" (inclusive) ) \"x\" number of times. This will also cause the Expression to be " +
                        "dynamic as this function will return a new value each time it is called. \n" +
                        "For example, roll ( 3, 8 ) can return will return ( random ( 1, 9 ) + random ( 1, 9 ) + random ( 1, 9 ).\n\n",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Future Plans ( not ordered ):",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text =  "-Better Selection for Copy & Paste Expressions / Groups\n" +
                        "-Expression and Group Templates: Save a copy of an Expression or Group. If it is an Expression, " +
                        "then lock its text. If it is a Group, lock all of the Expressions and Groups that it contains, but allow " +
                        "the addition of new Expressions and Groups.\n" +
                        "-Path Navigation: Ability to click on hotlinks in the path shown for your current group to jump to the " +
                        "selected parent group.\n" +
                        "-Advanced Expression Debugging: The selection of text in an Expression that is causing an error or warning is highlighted " +
                        "a corresponding color.\n" +
                        "-Sharing / Community Templates\n" +
                        "-Custom Tags: Ability to reference all Expressions that contain a specified tag.\n" +
                        "-Arrays / Lists\n" +
                        "-Loops / Iterators ( Maybe )\n" +
                        "-Conditional Statements \n",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )
        }

    }

}

@Composable
private fun HelpScreenViewTopBar(navigateUp: () -> Unit, navigateToGitHub: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primaryContainer),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = navigateUp,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Navigates to the Parent Group",
            )
        }
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )
        IconButton(
            onClick = navigateToGitHub,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Opens the GitHub page",
            )
        }
    }
}

@Preview
@Composable
private fun HelpScreenView_Preview(){
    DiceCompanionTheme {
        HelpScreenView (navigateUp = {}, navigateToGitHub = {})
    }
}