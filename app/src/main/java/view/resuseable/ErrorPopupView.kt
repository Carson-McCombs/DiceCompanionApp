package view.resuseable

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Popup

@Composable
fun ErrorPopupView(title: String, error: Error){
    Popup {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
            )
        Text(text = error.message ?: "empty error message")
    }
}

@Preview
@Composable
fun ErrorPopupView_Preview(){
    ErrorPopupView(title = "Error Preview", error = Error("Example Error Text"))
}