package model.dataObjects

import model.parser.token.LiteralType

data class ParseResult (
    val result: Any? = null,
    val resultType: LiteralType = LiteralType.NONE,
    val isStatic: Boolean = true,
    val globalDependencyIds: List<Long> = emptyList(),
    val localDependencyIds: List<Long> = emptyList(),
    val errorText: String = "",
)