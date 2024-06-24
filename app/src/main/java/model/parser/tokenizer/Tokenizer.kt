package model.parser.tokenizer

import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import model.RegexPatterns
import model.RegexPatterns.Companion.scannerRegex
import model.dataObjects.Expression
import model.dataObjects.ParseResult
import model.parser.token.Token

class Tokenizer {
    companion object {
        fun addSpaces(text: String):String {
            val spacedText = RegexPatterns.spacingRegex.replace(text, "$1 ")
            return RegexPatterns.moreThanOneSpaces.replace(spacedText, " ")
        }

        fun tokenize(rawText: String, getExpressionId: (String) -> Long?, getExpression: (Long) -> Expression?, reevaluateExpression: (Long, String) -> ParseResult?): Pair<List<Token>?,Error?> {
            //val transformedInputText = unaryOperatorRegex.replace(text, "u")
            //println("Original Text:    $text")
            //println("Transformed Text: $transformedInputText")
            //val spacedText = addSpaces(text)
            val badInputStrings = scannerRegex.split(rawText).filter { string -> string.isNotBlank() }
            if (badInputStrings.isNotEmpty()) return Pair(null, Error("Bad Input Found: ${badInputStrings.joinToString(", ")}"))
            val tokenStrings =
                scannerRegex.findAll(rawText).map { matchResult -> matchResult.value }.toList().toMutableList()
            for (i in 0..tokenStrings.count()) {
                if (isUnaryMinus(tokenStrings, i)) tokenStrings[i] = "u"
            }

            val tokens = tokenStrings.fastMap { string ->
                Token(
                    text = string,
                    getExpressionId = getExpressionId,
                    getExpression = getExpression,
                    reevaluateExpression = reevaluateExpression,
                )
            }.toList()
            val errorTokens = tokens.fastFilter { token -> token.error != null }
            if (errorTokens.isNotEmpty()) return Pair(null, Error(errorTokens.joinToString(separator = "; ") { token -> token.error.toString() }))
            return Pair(tokens,null)
        }
        private fun isUnaryMinus(list: List<String>, index: Int): Boolean {
            if (index < 0 || index > list.size - 1 || list[index] != "-") return false
            if ( index == 0 ) return true
            return when (list[index-1]){
                "(", ",", "+", "-", "*", "/", "%", "^" -> true
                else -> false
            }
        }

    }
}
