package view.resuseable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CheckableContainer(
    modifier: Modifier = Modifier,
    checkboxModifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    state: MutableState<Boolean> = mutableStateOf(false),
    onStateChanged: (Boolean) -> Unit = {},
    checkboxVisibilityState: MutableTransitionState<Boolean>,
    checkboxEnterTransition: EnterTransition = slideInHorizontally(initialOffsetX = { width -> -width }),
    checkboxExitTransition: ExitTransition = slideOutHorizontally(targetOffsetX = { width -> -width }),
    content: @Composable() (RowScope.() -> Unit),
) {
    Row(
        modifier = modifier.animateContentSize(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment
    ){
        AnimatedVisibility(
            visibleState = checkboxVisibilityState,
            enter = checkboxEnterTransition,
            exit = checkboxExitTransition
        ) {
            Checkbox(
                modifier = checkboxModifier,
                checked = state.value,
                onCheckedChange = { newState ->
                    state.value = newState
                    onStateChanged(state.value)
                }
            )
        }
        content()
    }
}