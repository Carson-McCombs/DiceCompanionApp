package model.parser.token

import model.RegexPatterns
import model.dataObjects.Expression
import model.dataObjects.ParseResult
import model.parser.function.Function


class Token(
    val text: String,
    var tokenType: TokenType = TokenType.UNDETERMINED,
    var literalType: LiteralType = LiteralType.UNDETERMINED,
    var value: Any? = null,
    groupPath: String,
    val getExpressionId: (String) -> Long?,
    val getExpression: (Long) -> Expression? = { null },
    val reevaluateExpression: (Long, String) -> ParseResult?  = { _, _ -> null },
    ) {
    var error: Error? = null
    var expressionId: Long? = null
    var isLocalExpressionReference: Boolean? = null
    var isStatic = true
    init{
        if (tokenType == TokenType.UNDETERMINED) tokenType = TokenType.parseTokenType(text)
        when(tokenType){
            TokenType.REFERENCE -> {
                val match = RegexPatterns.referenceRegex.matchEntire(text)!!
                println("GROUP VALUES : ${match.groupValues.joinToString(", ")}")
                isLocalExpressionReference = match.groupValues[1].isNotEmpty()
                val fullpath = "${if (isLocalExpressionReference == true) groupPath else "/"}${match.groupValues[2]}"

                expressionId = getExpressionId(fullpath)
                if (expressionId != null) {
                    updateReference()
                }
                else {
                    error = Error("Could not find dependency: ${if (isLocalExpressionReference == true) ".." else ""}$fullpath")
                }
            }
            TokenType.FUNCTION -> {
                if (literalType == LiteralType.UNDETERMINED) literalType = LiteralType.NONE
                if (value == null) value = parseValue()
                isStatic = (value as? Function)?.isStatic ?: true
            }
            else -> {
                if (literalType == LiteralType.UNDETERMINED) literalType =
                    LiteralType.parseLiteralTypeFromText(text)
                if (value == null) value = parseValue()
            }
        }

    }

    fun updateReference() {
        if (tokenType != TokenType.REFERENCE) return
        val id = expressionId ?: return
        val expression = getExpression(id) ?: return

        if (expression.parseResult.isStatic){
            literalType = expression.parseResult.resultType
            value = expression.parseResult.result
            isStatic = true

        } else {
            val reevaluatedResult = reevaluateExpression(id, expression.text)
                ?: throw Error("Expecting Expression ( $id ), but not found")
            literalType = reevaluatedResult.resultType
            value = reevaluatedResult.result
            isStatic = reevaluatedResult.isStatic

        }

    }

    fun parseValue(): Any? {
        return when (tokenType) {
            TokenType.NONE -> null
            TokenType.COMMENT -> text
            TokenType.PUNCTUATION -> text
            TokenType.REFERENCE -> {
                value
                //val (groupName, attributeName) = RegexPatterns.getReferenceGroupNameAndAttribute(text)
                //ExpressionReferences.getGroupAttribute(groupName, attributeName)?.value

            }
            TokenType.OPERATOR, TokenType.FUNCTION -> Function.functionMap[text]
            TokenType.LITERAL -> literalType.tryToParse(text)

            else -> null
        }
    }

    override fun toString(): String {
        var string = "Token Text ( \"$text\" ) -> TokenType ( $tokenType )"
        if (tokenType == TokenType.LITERAL || tokenType == TokenType.REFERENCE) string += " - LiteralType ( $literalType )"
        string += " Value ( $value )"
        if (error != null) string += " -> Error ( ${error!!.message} )"
        return string
    }


}




