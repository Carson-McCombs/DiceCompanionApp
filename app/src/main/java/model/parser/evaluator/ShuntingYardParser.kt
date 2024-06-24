package model.parser.evaluator

import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import model.dataObjects.Expression
import model.dataObjects.ParseResult
import model.parser.function.AssociativityDirection
import model.parser.function.Function
import model.parser.function.FunctionCall
import model.parser.token.Token
import model.parser.token.TokenType
import model.parser.tokenizer.Tokenizer


private data class ParserSnapshot(
    val tokens: List<Token>,
    val isStatic: Boolean,
    val dependencies: List<Long>
)


class ShuntingYardParser {
    companion object {
        private val cachedExpressionTokens: MutableMap<Long, ParserSnapshot> = mutableMapOf() //unsorted

        /*
        Checks for a cached tokens to reuse ( exists if the expression has been parsed at least once since the app has began running, even if not directly
        */
        fun reevaluate(id: Long, rawText: String, getExpressionId: (String) -> Long?, getExpression: (Long) -> Expression?, isDependentOn: (Long, Long) -> Boolean): ParseResult {
            if (!cachedExpressionTokens.containsKey(id)) return evaluate(
                id = id,
                rawText = rawText,
                getExpressionId = getExpressionId,
                getExpression = getExpression,
                isDependentOn = isDependentOn
            )
            val (tokens, isStatic, dependencies) = cachedExpressionTokens[id]!!
            tokens.fastFilter {token -> token.tokenType == TokenType.REFERENCE }.fastForEach { token -> token.updateReference() }
            return evaluate(
                tokens = tokens,
                isStatic = isStatic,
                dependencies = dependencies
            )
        }

        /*
        -Transforms raw text into tokens
        -Checks for errors such as Cyclic dependencies, self references, etc.
         */
        fun evaluate(id: Long, rawText: String, getExpressionId: (String) -> (Long?), getExpression: (Long) -> Expression?, isDependentOn: (Long, Long) -> Boolean): ParseResult {
            if (rawText.isBlank()) return ParseResult()
            val (tokens, parseTokensErr) = Tokenizer.tokenize(
                rawText = rawText,
                getExpression = getExpression,
                getExpressionId = getExpressionId,
                reevaluateExpression = { expressionId, expressionRawText ->
                    reevaluate(
                        id = expressionId,
                        rawText = expressionRawText,
                        getExpressionId = getExpressionId,
                        getExpression = getExpression,
                        isDependentOn = isDependentOn,

                    )
                },
            )
            if (parseTokensErr != null) return ParseResult(errorText = parseTokensErr.message ?: "")
            if (tokens == null) return ParseResult( errorText = "Tokenizer returned null list of tokens")
            val isStatic = tokens.fastFilter{ token -> token.tokenType == TokenType.FUNCTION || token.tokenType == TokenType.REFERENCE }.fastAll{ token -> token.isStatic}
            val dependencyIds: List<Long> = tokens.fastFilter { token -> token.tokenType == TokenType.REFERENCE }.fastMap { token -> token.expressionId!! }
            if (dependencyIds.contains(id)) return ParseResult( errorText = "Expressions cannot reference themselves.")
            val invalidDependencies: List<Long> = dependencyIds.fastFilter{ dependencyId -> isDependentOn(dependencyId, id)}
            if (invalidDependencies.isNotEmpty()) return ParseResult( errorText = "Cyclic Dependencies Found: $dependencyIds")
            if (!isStatic || dependencyIds.isNotEmpty()) cachedExpressionTokens[id] = ParserSnapshot(tokens = tokens, isStatic = isStatic, dependencies = dependencyIds)
            return evaluate(
                tokens = tokens,
                isStatic = isStatic,
                dependencies = dependencyIds,
            )
        }

        /*
        -Actually takes the tokens and evaluates them, returning the parse result
         */
        private fun evaluate(tokens: List<Token>, isStatic: Boolean, dependencies: List<Long>): ParseResult {
            val outputQueue = ArrayDeque<Token>()
            val operatorStack = ArrayDeque<Token>()
            for (token in tokens) {
                //println("-------------")
                //println("Token: ${token.text}")
                //println("Output Queue: ${outputQueue.map { t -> t.text }}")
                //println("Operator Stack: ${operatorStack.map { t -> t.text }}")
                when(token.tokenType) {
                    TokenType.LITERAL, TokenType.REFERENCE -> outputQueue.addLast(token)
                    TokenType.OPERATOR -> operatorStack.addFirst(token)
                    TokenType.PUNCTUATION -> {
                        when (token.text) {
                            "(" -> operatorStack.addFirst(token)
                            "," -> {
                                while (operatorStack.first().text != "(") {
                                    addToOutputQueue(outputQueue, operatorStack.removeFirst())
                                }
                            }

                            ")" -> {

                                while (operatorStack.first().text != "(") {
                                    if (operatorStack.isEmpty()) return ParseResult( errorText = "No opening parenthesis found", directDependencies = dependencies)
                                    //outputQueue.addLast(operatorStack.removeFirst())
                                    //println("-------------")
                                    //println("Token: ${operatorStack.first().text}")
                                    //println("Output Queue: ${outputQueue.map { t -> t.text }}")
                                    //println("Operator Stack: ${operatorStack.map { t -> t.text }}")
                                    addToOutputQueue(outputQueue, operatorStack.removeFirst())
                                }
                                if (operatorStack.first().text != "(") return ParseResult(  errorText = "No opening parenthesis found", directDependencies = dependencies )
                                operatorStack.removeFirst()
                                if (operatorStack.isNotEmpty() && operatorStack.first().tokenType == TokenType.FUNCTION) {
                                    val err = syPushThenEvaluate(outputQueue, operatorStack.removeFirst().value as Function)
                                    if (err != null) return ParseResult( errorText = err.message ?: "", directDependencies = dependencies)
                                }
                            }
                        }
                    }
                    TokenType.FUNCTION -> {
                        if (operatorStack.isEmpty()) {
                            operatorStack.addFirst(token)
                            continue
                        }
                        //var functionA = Function.functionMap[token.text] ?: return Pair(null, Error("Unknown Function \"${operatorStack.first()}\""))
                        //var functionB = Function.functionMap[operatorStack.first().text] ?: return Pair(null, Error("Unknown Function \"${operatorStack.first()}\""))
                        val tokenFunction = token.value as Function
                        var otherFunction = operatorStack.first().value as Function
                        while ((otherFunction.name != "(") && (tokenFunction.precedence > otherFunction.precedence) || (tokenFunction.precedence == otherFunction.precedence && tokenFunction.associativity == AssociativityDirection.LeftToRight)) {
                            //outputQueue.addLast(operatorStack.removeFirst())
                            //println("-------------")
                            //println("Token: ${operatorStack.first().text}")
                            //println("Output Queue: ${outputQueue.map { t -> t.text }}")
                            //println("Operator Stack: ${operatorStack.map { t -> t.text }}")
                            addToOutputQueue(outputQueue, operatorStack.removeFirst())
                            if (operatorStack.isEmpty()) break;
                            otherFunction = operatorStack.first().value as Function
                        }
                        operatorStack.addFirst(token)

                    }
                    else -> return ParseResult( errorText = "Bad token: $token", directDependencies = dependencies)
                }
            }
            while (operatorStack.isNotEmpty()) {
                //println("-------------")
                //println("Token: ${operatorStack.first().text}")
                //println("Output Queue: ${outputQueue.map { t -> t.text }}")
                //println("Operator Stack: ${operatorStack.map { t -> t.text }}")
                if (operatorStack.last().text == "(") return ParseResult( errorText = "Cannot end statement with an open parenthesis", directDependencies = dependencies)
                //outputQueue.addLast(operatorStack.removeFirst())
                val err = addToOutputQueue(outputQueue, operatorStack.removeFirst())
                if (err != null) return ParseResult( errorText = err.message ?: "")
            }
            //println("---------------------")
            //println("Final Output Queue: ${outputQueue.map { t -> t.text }}")
            //println("Final Operator Stack: ${operatorStack.map { t -> t.text }}")
            if (outputQueue.isEmpty()) return ParseResult( errorText = "Internal error, no output", directDependencies = dependencies)
            val finalValue = (outputQueue.first())
            finalValue.value ?: return ParseResult( errorText = "Could not parse value", directDependencies = dependencies)
            println("\n\n\nFinalValue: $finalValue")
            return ParseResult(
                result = finalValue.value,
                resultType = finalValue.literalType,
                isStatic = isStatic,
                directDependencies = dependencies
            )
        }

        private fun addToOutputQueue(outputQueue: ArrayDeque<Token>, token: Token): Error? {
            when(token.tokenType) {
                TokenType.FUNCTION, TokenType.OPERATOR -> return syPushThenEvaluate(outputQueue, token.value as Function)
                else -> outputQueue.addLast(token)
            }
            return null
        }

        private fun syPushThenEvaluate(outputQueue: ArrayDeque<Token>, function: Function): Error? {
                //if (Function.containsFunctionVariant())
                //else -> return Error("Unknown Operator or Function: $token")
            //}
            if (function.parameterCount == null) return Error("${function.name} has an unknown number of parameters")
            val (parameterTokens, err) = removeLast(outputQueue, function.parameterCount)
            if (err != null) return err
            //println("Push Then Evaluate: ")
            //println("Output Queue: ${outputQueue.map { t -> t.text }}")
            //println("Parameters: { ${parameterTokens.joinToString(", ") } }")
            //println("Function: $function")
            val parameterTypes = parameterTokens.map{token -> token.literalType }.toTypedArray()

            if (!function.canCastTo(parameterTypes))
                return Error("$function cannot have parameters of type { ${parameterTypes.joinToString(", ")} }")
            val parameterValues = parameterTokens.map{token -> token.value }.toTypedArray()
            val functionCall = FunctionCall(function.name, parameterValues)

            if (functionCall.error != null) return functionCall.error
            outputQueue.addLast(
                Token(
                    text = functionCall.result.toString(),
                    tokenType = TokenType.LITERAL,
                    literalType = functionCall.returnType,
                    value = functionCall.result,
                    getExpressionId = { null!! }
                )
            )
            //println("Function Result: ${functionCall.result.toString()}")
            return null
        }

        private fun <T> removeLast(queue: ArrayDeque<T>, count: Int): Pair<List<T>, Error?> {
            if (queue.size < count) return Pair(emptyList<T>(), Error("Try to remove too many items from queue"))
            val sub = queue.subList(queue.size - count, queue.size)
            val output = sub.toList()
            sub.clear()
            return Pair(output, null)
        }

    }


}

