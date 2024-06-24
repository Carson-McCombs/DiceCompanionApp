package view.expressionGroupScreen

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import view.ui.theme.DiceCompanionTheme
import view.ui.theme.onWarningContainerDark
import view.ui.theme.onWarningContainerLight
import view.ui.theme.onWarningDark
import view.ui.theme.onWarningLight
import view.ui.theme.warningContainerDark
import view.ui.theme.warningContainerLight
import view.ui.theme.warningDark
import view.ui.theme.warningLight
import viewModel.EvaluationState

@Composable
fun evaluationButtonColors(evaluationState: EvaluationState): ButtonColors {
    val isDarkTheme = isSystemInDarkTheme()
    return when(evaluationState)  {
        EvaluationState.READY -> ButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        EvaluationState.OUT_OF_DATE -> {
            if (isDarkTheme) {
                ButtonColors(
                    containerColor = warningContainerDark,
                    contentColor = onWarningContainerDark,
                    disabledContainerColor = warningDark,
                    disabledContentColor = onWarningDark
                )
            } else {
                ButtonColors(
                    containerColor = warningContainerLight,
                    contentColor = onWarningContainerLight,
                    disabledContainerColor = warningLight,
                    disabledContentColor = onWarningLight
                )
            }
        }
        EvaluationState.ERROR -> ButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
            disabledContentColor = MaterialTheme.colorScheme.onErrorContainer
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
private fun EvaluationButtonColors_Preview(){
    DiceCompanionTheme {
        LazyColumn(
            modifier = Modifier
                .height(320.dp)
                .width(128.dp),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            items(listOf(EvaluationState.READY, EvaluationState.ERROR, EvaluationState.OUT_OF_DATE)){ state ->
                Button(
                    modifier = Modifier.size(96.dp),
                    onClick = { },
                    colors = evaluationButtonColors(evaluationState = state)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        text = "123",
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

    }
}
