package viewModel

import model.dataObjects.Expression

enum class EvaluationState { READY, ERROR, OUT_OF_DATE;
    companion object{
        fun fromExpression(expression: Expression): EvaluationState =
            when{
                expression.parseResult.errorText.isNotBlank() -> ERROR
                !expression.updated -> OUT_OF_DATE
                else -> READY
            }

    }
}