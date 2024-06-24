package model.parser.function

import model.parser.token.LiteralType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.random.Random

data class FunctionVariant(
    val parameterTypes: Array<LiteralType>,
    val apply: (Array<Any?>) -> Any?,
    val returnType: LiteralType
)

enum class AssociativityDirection { RightToLeft, LeftToRight }

class Function(
    val name: String,
    variants: Array<FunctionVariant>,
    private val allowCast: Boolean = true,
    val associativity: AssociativityDirection,
    val precedence: Int,
    val isStatic: Boolean = true
) {
    private val variantMap = variants.associateBy { variant -> LiteralType.getHashOfArray(variant.parameterTypes) }
    val parameterCount = tryGetParameterCount()


    private fun tryGetParameterCount():Int? {
        val count = variantMap.values.first().parameterTypes.size
        if (variantMap.values.all { variant -> variant.parameterTypes.size == count }) return count
        return null
    }

    fun getVariant(parameterTypes: Array<LiteralType>): FunctionVariant? {
        return variantMap[LiteralType.getHashOfArray(parameterTypes)]
    }
    fun getCastedVariant(parameterTypes: Array<LiteralType>): FunctionVariant?{
        if (containsVariant(parameterTypes)) return variantMap[LiteralType.getHashOfArray(
            parameterTypes
        )]
        if (!canCastTo(parameterTypes)) return null
        var castedVariant: FunctionVariant? = null
        variantMap.values.forEach { variant -> if (LiteralType.canCastArrayTo(
                parameterTypes,
                variant.parameterTypes
            )
        ) { castedVariant = variant}  }
        return castedVariant
    }
    fun containsVariant(parameterTypes: Array<LiteralType>): Boolean = variantMap.containsKey(
        LiteralType.getHashOfArray(parameterTypes)
    )
    fun canCastTo(parameterTypes: Array<LiteralType>): Boolean {
        return when {
            containsVariant(parameterTypes) -> true
            !allowCast -> false
            else -> variantMap.values.any { variant ->
                LiteralType.canCastArrayTo(
                    parameterTypes,
                    variant.parameterTypes
                )
            }
        }
    }
    fun getReturnType(parameterTypes: Array<LiteralType>): Pair<LiteralType, Error?> {
        if (!canCastTo(parameterTypes)) return Pair(LiteralType.NONE, Error("Function \"$name\" does not contain function variant with parameter types:  [ ${parameterTypes.joinToString(", ")} ]"))
        val variant = getCastedVariant(parameterTypes)
            ?: return Pair(LiteralType.NONE, Error("Function \"$name\" contains null variant with parameter types: [ ${parameterTypes.joinToString(", ")} ]"))
        return Pair(variant.returnType, null)
    }
    fun apply(parameterValues: Array<Any?>): Pair<Any?, Error?> {
        val parameterTypes = LiteralType.parseLiteralTypeArrayFromValueArray(parameterValues)

        if (!canCastTo(parameterTypes)) return Pair(null, Error("Function \"$name\" does not contain function variant with parameter types: [ ${parameterTypes.joinToString(", ")} ]"))
        val variant = getCastedVariant(parameterTypes) ?: return Pair(null, Error("Function \"$name\" contains a null variant with parameter types: [ ${parameterTypes.joinToString(", ")} ]"))
        val castedParameterValues =
            LiteralType.tryCastValueArrayTo(parameterValues, parameterTypes, variant.parameterTypes)
        return try {
            Pair( variant.apply(castedParameterValues), null)
        } catch (e: Exception) {
            Pair( null,  Error("Function \"$name\" with parameter types [ ${parameterTypes.joinToString(", ")} ], given [ ${parameterValues.joinToString(", ")} ], throws ${e.message}"))
        }
    }

    override fun toString(): String = "Function Name ( $name ): { Allow Casts ( $allowCast ), Associativity ( $associativity ), Precedence ( $precedence ), Variants ( ${variantMap.values.joinToString(", ")} )"

    companion object{
        private const val debug = true
        private val functions = arrayOf(
            Function(
                name= "+",
                precedence = 4,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.INTEGER, LiteralType.INTEGER),
                        apply= { parameterValues ->
                            if (debug) println("+ (Integer, Integer) [ ${parameterValues.joinToString(", ")} ]")
                            (parameterValues[0] as Int) + (parameterValues[1] as Int)
                        },
                        returnType = LiteralType.INTEGER
                    ),
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE, LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("+ (Double, Double) [ ${parameterValues.joinToString(", ")} ]")
                            (parameterValues[0] as Double) + (parameterValues[1] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),
            Function(
                name= "-",
                precedence = 4,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.INTEGER, LiteralType.INTEGER),
                        apply= { parameterValues ->
                            if (debug) println("- (Integer, Integer) [ ${parameterValues.joinToString(", ")} ]")
                            (parameterValues[0] as Int) - (parameterValues[1] as Int)
                        },
                        returnType = LiteralType.INTEGER
                    ),
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE, LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("- (Double, Double) [ ${parameterValues.joinToString(", ")} ]")
                            (parameterValues[0] as Double) - (parameterValues[1] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),
            Function(
                name="u",
                precedence = 2,
                associativity = AssociativityDirection.RightToLeft,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.INTEGER),
                        apply= { parameterValues ->
                            if (debug) println("-u (Integer) [ ${parameterValues[0]} ]")
                            -(parameterValues[0] as Int)
                        },
                        returnType = LiteralType.INTEGER
                    ),
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("-u (Double) [ ${parameterValues[0]} ]")
                            -(parameterValues[0] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),
            Function(
                name= "*",
                precedence = 3,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.INTEGER, LiteralType.INTEGER),
                        apply= { parameterValues ->
                            if (debug) println("* (Integer, Integer) [ ${parameterValues.joinToString(", ")} ]")
                            (parameterValues[0] as Int) * (parameterValues[1] as Int)
                        },
                        returnType = LiteralType.INTEGER
                    ),
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE, LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("* (Double, Double) [ ${parameterValues.joinToString(", ")} ]")
                            (parameterValues[0] as Double) * (parameterValues[1] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),
            Function(
                name= "/",
                precedence = 2,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE, LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("/ (Double, Double) [ ${parameterValues.joinToString(", ")} ]")
                            (parameterValues[0] as Double) / (parameterValues[1] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),
            Function(
                name= "^",
                precedence = 2,
                associativity = AssociativityDirection.RightToLeft,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.INTEGER, LiteralType.INTEGER),
                        apply= { parameterValues ->
                            if (debug) println("^ (Integer, Integer) [ ${parameterValues.joinToString(", ")} ]")
                            ((parameterValues[0] as Int).toDouble().pow((parameterValues[1] as Int).toDouble())).toInt()
                        },
                        returnType = LiteralType.INTEGER
                    ),
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE, LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("^ (Double, Double) [ ${parameterValues.joinToString(", ")} ]")
                            (parameterValues[0] as Double).pow(parameterValues[1] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),
            Function(
                name= "sqrt",
                precedence = 2,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("sqrt (Double) [ ${parameterValues.joinToString(", ")} ]")
                            sqrt(parameterValues[0] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),

            Function(
                name= "%",
                precedence = 2,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.INTEGER, LiteralType.INTEGER),
                        apply= { parameterValues ->
                            if (debug) println("% (Int, Int) [ ${parameterValues.joinToString(", ")} ]")
                            (parameterValues[0] as Int) % (parameterValues[1] as Int)
                        },
                        returnType = LiteralType.INTEGER
                    ),
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE, LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("% (Double, Double) [ ${parameterValues.joinToString(", ")} ]")
                            (parameterValues[0] as Double) % (parameterValues[1] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),
            Function(
                name= "max",
                precedence = 1,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.INTEGER, LiteralType.INTEGER),
                        apply= { parameterValues ->
                            if (debug) println("max (Int, Int) [ ${parameterValues.joinToString(", ")} ]")
                            max(parameterValues[0] as Int, parameterValues[1] as Int)
                        },
                        returnType = LiteralType.INTEGER
                    ),
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE, LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("max (Double, Double) [ ${parameterValues.joinToString(", ")} ]")
                            max(parameterValues[0] as Double, parameterValues[1] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),
            Function(
                name= "min",
                precedence = 1,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.INTEGER, LiteralType.INTEGER),
                        apply= { parameterValues ->
                            if (debug) println("min (Int, Int) [ ${parameterValues.joinToString(", ")} ]")
                            min(parameterValues[0] as Int, parameterValues[1] as Int)
                        },
                        returnType = LiteralType.INTEGER
                    ),
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE, LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("min (Double, Double) [ ${parameterValues.joinToString(", ")} ]")
                            min(parameterValues[0] as Double, parameterValues[1] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),
            Function(
                name= "floor",
                precedence = 1,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("floor (Double) [ ${parameterValues.joinToString(", ")} ]")
                            floor(parameterValues[0] as Double).toInt()
                        },
                        returnType = LiteralType.INTEGER
                    )
                )
            ),
            Function(
                name= "round",
                precedence = 1,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("round (Double) [ ${parameterValues.joinToString(", ")} ]")
                            val value = parameterValues[0] as Double
                            val decimal = value % 1
                            when{
                                decimal < 0.5 -> (value - value.sign * decimal).toInt()
                                else -> (value + value.sign * (1 - decimal)).toInt()
                            }
                        },
                        returnType = LiteralType.INTEGER
                    )
                )
            ),
            Function(
                name= "ceil",
                precedence = 1,
                associativity = AssociativityDirection.LeftToRight,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("ceil (Double) [ ${parameterValues.joinToString(", ")} ]")
                            ceil(parameterValues[0] as Double).toInt()
                        },
                        returnType = LiteralType.INTEGER
                    )
                )
            ),
            Function(
                name= "random",
                precedence = 1,
                associativity = AssociativityDirection.LeftToRight,
                isStatic = false,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.INTEGER, LiteralType.INTEGER),
                        apply= { parameterValues ->
                            if (debug) println("random (Integer, Integer) [ ${parameterValues.joinToString(", ")} ]")
                            Random.nextInt(parameterValues[0] as Int, parameterValues[1] as Int)
                        },
                        returnType = LiteralType.INTEGER
                    ),
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.DOUBLE, LiteralType.DOUBLE),
                        apply= { parameterValues ->
                            if (debug) println("random (Double, Double) [ ${parameterValues.joinToString(", ")} ]")
                            Random.nextDouble(parameterValues[0] as Double, parameterValues[1] as Double)
                        },
                        returnType = LiteralType.DOUBLE
                    )
                )
            ),
            Function(
                name= "roll",
                precedence = 1,
                associativity = AssociativityDirection.LeftToRight,
                isStatic = false,
                variants = arrayOf(
                    FunctionVariant(
                        parameterTypes = arrayOf(LiteralType.INTEGER, LiteralType.INTEGER),
                        apply= { parameterValues ->
                            if (debug) println("roll (Integer, Integer) [ ${parameterValues.joinToString(", ")} ]")
                            var sum = 0
                            for (i in 0..<(parameterValues[0] as Int)) {
                                sum += Random.nextInt(0,parameterValues[1] as Int) + 1
                            }
                            sum
                        },
                        returnType = LiteralType.INTEGER
                    )
                )
            ),
        )
        val functionMap = functions.associateBy { function -> function.name }
    }

}