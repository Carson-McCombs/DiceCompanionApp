package model.parser.token

import model.RegexPatterns

enum class TokenType { UNDETERMINED, NONE, LITERAL, OPERATOR, FUNCTION, PUNCTUATION, COMMENT, REFERENCE;
    companion object{
        fun parseTokenType(text: String): TokenType {
            return when{
                RegexPatterns.punctuationRegex.matches(text) -> PUNCTUATION
                RegexPatterns.commentRegex.matches(text) -> COMMENT
                RegexPatterns.operatorRegex.matches(text) -> OPERATOR
                RegexPatterns.functionRegex.matches(text) -> FUNCTION
                RegexPatterns.literalRegex.matches(text) -> LITERAL
                RegexPatterns.referenceRegex.matches(text) -> REFERENCE
                else -> NONE
            }

        }
    }

}