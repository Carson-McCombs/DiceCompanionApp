package view.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import model.dataObjects.ClipboardReference
import model.database.AppRepository
import view.expressionGroupScreen.GroupScreenView
import view.helpScreen.HelpScreenView
import viewModel.ExpressionGroupScreenViewModelFactory
import viewModel.GroupScreenEvents
import viewModel.GroupScreenViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    repository: AppRepository,
    clipboardReference: ClipboardReference,
    rootId: Long
) {
    val selectionMode = remember(0) { MutableTransitionState(false)}
    val clipboard = remember(0) { clipboardReference }
    NavHost(
        navController = navController,
        startDestination = "expressionGroup?id={id}"
    ) {
        composable(
            route = "expressionGroup?id={id}",
            arguments = listOf(
                navArgument(
                    name = "id"
                ){
                    type = NavType.LongType
                    defaultValue = rootId
                }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { width -> width  }
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { width -> -width  }
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { width -> -width  }
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { width -> width  }
                )
            },
        ){ backStackEntry ->

            val id = backStackEntry.arguments!!.getLong("id")
            val screenViewModel = viewModel<GroupScreenViewModel>(
                key = id.toString(),
                factory = ExpressionGroupScreenViewModelFactory(
                    id = id,
                    repository = repository
                )
            )
            GroupScreenView(
                viewModel = screenViewModel,
                groupScreenEvents = GroupScreenEvents(
                    setSelectionMode = { mode ->
                        selectionMode.targetState = mode
                        if (!mode) {
                            clipboard.clear()
                        }
                    },
                    copySelection = {
                        clipboard.copy()
                    },
                    pasteSelection = { parentGroupId: Long ->
                        clipboard.pasteTo(parentGroupId)
                    },
                    deleteSelection = {
                        clipboard.delete()
                    },
                    navigateTo = { expressionGroupId ->
                        navController.navigate(
                            route = "expressionGroup?id=$expressionGroupId"
                        )
                    },
                    navigateUp = { navController.navigateUp() },
                    navigateToHelpScreen = { navController.navigate("settings") }
                ),
                selectionMode = selectionMode,
                clipboardReference = clipboardReference
            )

        }
        composable(
            route = "settings",
        ){
            HelpScreenView(
                navigateUp = { navController.navigateUp() },
                navigateToGitHub = { navigateToGitHub(navController.context) }
            )
        }
    }
}

fun navigateToGitHub(context: Context){
    val url = "https://github.com/Carson-McCombs"
    val openGitHubIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    ContextCompat.startActivity(context, openGitHubIntent, null)
}
