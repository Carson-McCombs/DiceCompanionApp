package model.parser.function

import model.parser.token.LiteralType

data class FunctionCall(
    val name: String,
    val parameterValues: Array<Any?>
) {
    val function: Function? = Function.functionMap[name]
    var result: Any?
    var returnType: LiteralType
    var error: Error?
    init {
        if (function == null) {
            result = null
            returnType = LiteralType.NONE
            error = Error("Function $name does not exist")
        } else {
            try {
                val (literalType, returnTypeErr) = function.getReturnType(
                    LiteralType.parseLiteralTypeArrayFromValueArray(
                        parameterValues
                    )
                )
                returnType = literalType

                if (returnTypeErr == null) {
                    //println("$name ( ${parameterValues.joinToString(", ") } )")
                    val (functionResult, functionError) = function.apply(parameterValues)
                    result = functionResult
                    error = functionError
                } else {
                    result = null
                    error = returnTypeErr
                }
            } catch (ex: Exception) {
                error = Error(ex.message)
                result = null
                returnType = LiteralType.NONE
            }


        }
    }

    override fun toString(): String {
        if (error == null ) return "Sucess - Function $name ( ${parameterValues.joinToString(", ")} ): Return Type ( $returnType ), Result ( $result )"
        return "Failure - Function $name ( ${parameterValues.joinToString(", ")} ): Return Type ( $returnType ), Result ( $result ), Error ( $error )"
    }
}