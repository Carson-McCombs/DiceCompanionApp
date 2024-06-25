package view.expressionGroupScreen

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

class RegexInputTransformationFilter(private val regex: Regex): InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (!regex.matches(asCharSequence())) revertAllChanges()
    }
}