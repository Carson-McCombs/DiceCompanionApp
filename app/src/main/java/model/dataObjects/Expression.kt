package model.dataObjects

import model.RegexPatterns
import viewModel.EvaluationState

data class Expression (
    val id: Long = 0,
    val parentId: Long,
    val name: String,
    val text: String = "",
    val updated: Boolean = true,
    val parseResult: ParseResult = ParseResult()
) {
    val resultText: String = parseResult.result?.toString() ?: ""
    val evaluationState = EvaluationState.fromExpression(this)

    companion object{
        fun renameReference(expression: Expression, oldFullPath: String, newFullPath: String): Expression {
            val newText = RegexPatterns.referenceRegex.replace(input = expression.text) { matchResult ->
                matchResult.groupValues[0].replaceFirst(oldValue = oldFullPath, newValue = newFullPath)
            }
            return expression.copy(text = newText)
        }
    }
}