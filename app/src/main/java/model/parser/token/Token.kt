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
    val getExpressionId: (String) -> Long?,
    val getExpression: (Long) -> Expression? = { null },
    val reevaluateExpression: (Long, String) -> ParseResult?  = { _, _ -> null },
    ) {
    var error: Error? = null
    var expressionId: Long? = null
    var isStatic = true
    init{
        if (tokenType == TokenType.UNDETERMINED) tokenType = TokenType.parseTokenType(text)
        when(tokenType){
            TokenType.REFERENCE -> {
                val fullpath = "/" + RegexPatterns.referenceRegex.matchEntire(text)!!.groupValues[1]
                expressionId = getExpressionId(fullpath)
                if (expressionId != null) {
                    //expressionReference = getExpression(expressionId)

                    updateReference()
                }
                else {
                    error = Error("Could not find dependency: $fullpath")
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
        //expressionReference ?: return
        //val expression = expressionReference!!
        val expression = getExpression(id) ?: return

        if (expression.parseResult.isStatic == true){
            literalType = expression.parseResult.resultType //?: LiteralType.NONE
            value = expression.parseResult.result
            isStatic = true

        } else {
            val reevaluatedResult = reevaluateExpression(id, expression.text)
                ?: throw Error("Expecting Expression ( $id ), but not found")
            //expressionReference = expression.copy(parseResult = parseResult)
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




