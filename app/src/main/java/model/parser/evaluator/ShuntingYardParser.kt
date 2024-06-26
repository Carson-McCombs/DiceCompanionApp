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
    val globalDependencyIds: List<Long>,
    val localDependencyIds: List<Long>,
)


class ShuntingYardParser {
    companion object {
        private val cachedExpressionTokens: MutableMap<Long, ParserSnapshot> = mutableMapOf() //unsorted

        /*
        Checks for a cached tokens to reuse ( exists if the expression has been parsed at least once since the app has began running, even if not directly
        */
        fun reevaluate(id: Long,  getGroupPath: (Long) -> String, rawText: String, getExpressionId: (String) -> Long?, getExpression: (Long) -> Expression?, getOverlappingDependencies: (Long, List<Long>) -> List<Long>): ParseResult {
            if (!cachedExpressionTokens.containsKey(id)) return evaluate(
                id = id,
                rawText = rawText,
                getGroupPath = getGroupPath,
                getExpressionId = getExpressionId,
                getExpression = getExpression,
                getOverlappingDependencies = getOverlappingDependencies
            )
            val parserSnapShot = cachedExpressionTokens[id]!!
            parserSnapShot.tokens.fastFilter {token -> token.tokenType == TokenType.REFERENCE }.fastForEach { token -> token.updateReference() }
            return evaluate(
                tokens = parserSnapShot.tokens,
                isStatic = parserSnapShot.isStatic,
                globalDependencyIds = parserSnapShot.globalDependencyIds,
                localDependencyIds = parserSnapShot.localDependencyIds
            )
        }

        /*
        -Transforms raw text into tokens
        -Checks for errors such as Cyclic dependencies, self references, etc.
         */
        fun evaluate(id: Long, rawText: String, getGroupPath: (Long) -> String, getExpressionId: (String) -> (Long?), getExpression: (Long) -> Expression?, getOverlappingDependencies: (Long, List<Long>) -> List<Long>): ParseResult {
            if (rawText.isBlank()) return ParseResult()
            val (tokens, parseTokensErr) = Tokenizer.tokenize(
                rawText = rawText,
                groupPath = getGroupPath(id),
                getExpression = getExpression,
                getExpressionId = getExpressionId,
                reevaluateExpression = { expressionId, expressionRawText ->
                    reevaluate(
                        id = expressionId,
                        rawText = expressionRawText,
                        getGroupPath = getGroupPath,
                        getExpressionId = getExpressionId,
                        getExpression = getExpression,
                        getOverlappingDependencies = getOverlappingDependencies,

                    )
                },
            )
            if (parseTokensErr != null) return ParseResult(errorText = parseTokensErr.message ?: "")
            if (tokens == null) return ParseResult( errorText = "Tokenizer returned null list of tokens")
            val isStatic = tokens.fastFilter{ token -> token.tokenType == TokenType.FUNCTION || token.tokenType == TokenType.REFERENCE }.fastAll{ token -> token.isStatic}
            val dependencies: List<Token> = tokens.fastFilter { token -> token.tokenType == TokenType.REFERENCE }
            val globalDependencyIds: List<Long> = dependencies.fastFilter { token -> token.isLocalExpressionReference == false }.fastMap { token -> token.expressionId!! }
            val localDependencyIds: List<Long> = dependencies.fastFilter { token -> token.isLocalExpressionReference == true }.fastMap { token -> token.expressionId!! }
            if (globalDependencyIds.contains(id) || localDependencyIds.contains(id)) return ParseResult( errorText = "Expressions cannot reference themselves.")
            val invalidDependencies: List<Long> = getOverlappingDependencies(id, globalDependencyIds + localDependencyIds)
            if (invalidDependencies.isNotEmpty()) return ParseResult( errorText = "Cyclic Dependencies Found: $globalDependencyIds")
            if (!isStatic || globalDependencyIds.isNotEmpty()) cachedExpressionTokens[id] = ParserSnapshot(tokens = tokens, isStatic = isStatic, globalDependencyIds = globalDependencyIds, localDependencyIds = localDependencyIds)
            return evaluate(
                tokens = tokens,
                isStatic = isStatic,
                globalDependencyIds = globalDependencyIds,
                localDependencyIds = localDependencyIds
            )
        }

        /*
        -Actually takes the tokens and evaluates them, returning the parse result
         */
        private fun evaluate(tokens: List<Token>, isStatic: Boolean, globalDependencyIds: List<Long>, localDependencyIds: List<Long>): ParseResult {
            val baseParseResult = ParseResult(isStatic = isStatic, globalDependencyIds = globalDependencyIds, localDependencyIds = localDependencyIds)
            val outputQueue = ArrayDeque<Token>()
            val operatorStack = ArrayDeque<Token>()
            //println(tokens.joinToString ("\n "))
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
                                    if (operatorStack.isEmpty()) return baseParseResult.copy( errorText = "No opening parenthesis found")
                                    //println("-------------")
                                    //println("Token: ${operatorStack.first().text}")
                                    //println("Output Queue: ${outputQueue.map { t -> t.text }}")
                                    //println("Operator Stack: ${operatorStack.map { t -> t.text }}")
                                    addToOutputQueue(outputQueue, operatorStack.removeFirst())
                                }
                                if (operatorStack.first().text != "(") return baseParseResult.copy(  errorText = "No opening parenthesis found")
                                operatorStack.removeFirst()
                                if (operatorStack.isNotEmpty() && operatorStack.first().tokenType == TokenType.FUNCTION) {
                                    val err = syPushThenEvaluate(outputQueue, operatorStack.removeFirst().value as Function)
                                    if (err != null) return baseParseResult.copy( errorText = err.message ?: "")
                                }
                            }
                        }
                    }
                    TokenType.FUNCTION -> {
                        if (operatorStack.isEmpty()) {
                            operatorStack.addFirst(token)
                            continue
                        }
                        if (operatorStack.first().tokenType == TokenType.PUNCTUATION){
                            operatorStack.addFirst(token)
                            continue
                        }
                        val tokenFunction = token.value as Function
                        var otherFunction = operatorStack.first().value as Function
                        while ((otherFunction.name != "(") && (tokenFunction.precedence > otherFunction.precedence) || (tokenFunction.precedence == otherFunction.precedence && tokenFunction.associativity == AssociativityDirection.LeftToRight)) {
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
                    else -> return baseParseResult.copy( errorText = "Bad token: $token")
                }
            }
            while (operatorStack.isNotEmpty()) {
                //println("-------------")
                //println("Token: ${operatorStack.first().text}")
                //println("Output Queue: ${outputQueue.map { t -> t.text }}")
                //println("Operator Stack: ${operatorStack.map { t -> t.text }}")
                if (operatorStack.last().text == "(") return baseParseResult.copy( errorText = "Cannot end statement with an open parenthesis")
                val err = addToOutputQueue(outputQueue, operatorStack.removeFirst())
                if (err != null) return baseParseResult.copy( errorText = err.message ?: "")
            }
            //println("---------------------")
            //println("Final Output Queue: ${outputQueue.map { t -> t.text }}")
            //println("Final Operator Stack: ${operatorStack.map { t -> t.text }}")
            if (outputQueue.isEmpty()) return baseParseResult.copy( errorText = "Internal error, no output")
            val finalValue = (outputQueue.first())
            finalValue.value ?: return baseParseResult.copy( errorText = "Could not parse value")
            println("\n\n\nFinalValue: $finalValue")
            return baseParseResult.copy(
                result = finalValue.value,
                resultType = finalValue.literalType,
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
                    groupPath = "",
                    tokenType = TokenType.LITERAL,
                    literalType = functionCall.returnType,
                    value = functionCall.result,
                    getExpressionId = { throw Error("The result of an expression should always be a value, never a reference to another Expression.") }
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

