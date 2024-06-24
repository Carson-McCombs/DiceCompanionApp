package view.resuseable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape

@Composable
fun DismissibleCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    border: BorderStroke? = null,
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    onStartToEnd: () -> Unit = {},
    onEndToStart: () -> Unit = {},
    onSettle: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            return@rememberSwipeToDismissBoxState when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onStartToEnd()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onEndToStart()
                    true
                }
                SwipeToDismissBoxValue.Settled -> {
                    onSettle()
                    false
                }
            }
        },
        positionalThreshold = {totalDistance -> totalDistance / 2 }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = enableDismissFromStartToEnd,
        enableDismissFromEndToStart = enableDismissFromEndToStart,
        backgroundContent = {
            SwipeToDismissBackground(
                modifier = modifier
                    .fillMaxSize()
                    .clip(shape = shape),
                dismissState = dismissState
            )
        }
    ) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border
        ) {
            content()
        }
    }
}