package model.parser.token

import model.RegexPatterns

enum class LiteralType { UNDETERMINED, NONE, STRING, INTEGER, DOUBLE;
    fun canCastTo(other: LiteralType): Boolean = canCastTo(this, other)
    fun tryToParse(text: String): Any? {
        return when(this){
            NONE -> null
            STRING -> text
            INTEGER -> text.toInt()
            DOUBLE -> text.toDouble()
            else -> null
        }
    }
    fun tryToCast(value: Any?,  newLiteralType: LiteralType): Any? {
        return when{
            this == newLiteralType -> value
            newLiteralType == STRING -> value.toString()
            this == INTEGER && newLiteralType == DOUBLE -> (value as Int).toDouble()
            else -> null
        }
    }

    companion object{

        fun getByOrdinal(ordinal: Int): LiteralType? {
            if (ordinal > entries.size) return null
            return entries[ordinal]
        }

        fun parseLiteralTypeFromValue(value: Any?): LiteralType {
            return when(value){
                is Double -> DOUBLE
                is Int -> INTEGER
                is String -> STRING
                else -> NONE
            }
        }

        fun parseLiteralTypeArrayFromValueArray(valueArray: Array<Any?>): Array<LiteralType> {

            return valueArray.map { value -> parseLiteralTypeFromValue(value) }.toTypedArray()
        }

        fun parseLiteralTypeFromText(text: String): LiteralType {
            return when{
                RegexPatterns.stringRegex.matches(text) -> STRING
                RegexPatterns.doubleRegex.matches(text) -> DOUBLE
                RegexPatterns.integerRegex.matches(text) -> INTEGER
                /*RegexPatterns.referenceRegex.matches(text) -> {
                    val match = RegexPatterns.referenceRegex.matchEntire(text) ?: return NONE
                    ExpressionReferences.getGroupAttribute(
                        match.groupValues[1],
                        match.groupValues[2]
                    )?.literalType ?: NONE
                }*/
                else -> NONE
            }
        }



        fun getHashOfArray(literalTypeArray: Array<LiteralType>): Int {
            if (literalTypeArray.isEmpty()) return -1
            var hash = 0
            literalTypeArray.forEach { literalType -> hash = hash * 4 + literalType.ordinal }
            return hash
        }

        fun parseHash(hash: Int): Array<LiteralType>? {
            if (hash < 0) return null
            val literalTypeList = mutableListOf<LiteralType>()
            var currentHash = hash
            while (currentHash > 0) {
                val ordinal = currentHash % 4
                currentHash = (hash - ordinal) / 4
                literalTypeList.add(getByOrdinal(ordinal)!!)
            }
            return literalTypeList.toTypedArray().reversedArray()
        }

        fun canCastTo(from: LiteralType, to: LiteralType): Boolean {
            return when {
                from == to -> true
                to == STRING -> true
                to == DOUBLE && from == INTEGER -> true
                else -> false
            }
        }

        fun canCastArrayTo(from: Array<LiteralType>, to: Array<LiteralType>): Boolean {
            if (from.size != to.size) return false
            from.forEachIndexed { i, literalType -> if (!canCastTo(literalType, to[i])) return false }
            return true
        }

        fun canCastArrayToHash(from: Array<LiteralType>, to: Int): Boolean {
            val parsedArray = parseHash(to) ?: return false
            return canCastArrayTo(from, parsedArray)
        }

        fun tryCastValueArrayTo(fromValues: Array<Any?>, fromTypes: Array<LiteralType>, toTypes: Array<LiteralType>): Array<Any?> {
            if (fromValues.size != fromTypes.size || fromTypes.size != toTypes.size) return emptyArray()
            return fromTypes.mapIndexed { i, literalType -> tryCastValueTo(fromValues[i], literalType, toTypes[i]) }.toTypedArray()
        }

        fun tryCastValueTo(value: Any?, oldLiteralType: LiteralType, newLiteralType: LiteralType): Any? {
            return when{
                oldLiteralType == newLiteralType -> value
                newLiteralType == STRING -> value.toString()
                oldLiteralType == INTEGER && newLiteralType == DOUBLE -> (value as Int).toDouble()
                else -> null
            }
        }
    }

}