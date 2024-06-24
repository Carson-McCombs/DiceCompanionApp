package view.resuseable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SwipeToDismissBackground(modifier: Modifier = Modifier, dismissState: SwipeToDismissBoxState) {
    val (backgroundColor, iconColor) = when( dismissState.dismissDirection ) {
        SwipeToDismissBoxValue.StartToEnd -> Pair(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
        else -> Pair(Color.Transparent, Color.Transparent)
    }
    Column(
        modifier = modifier
            .background(backgroundColor)
            .padding(8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ){
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete this Expression",
            tint = iconColor
        )
    }
}