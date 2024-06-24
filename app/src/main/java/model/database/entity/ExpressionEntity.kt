package model.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import model.dataObjects.Expression
import model.dataObjects.ParseResult
import model.parser.token.LiteralType

@Entity (
    indices = [Index(value=["id"], unique = true),Index(value=["parentId"])],
    foreignKeys = [
    ForeignKey(
        entity = GroupEntity::class,
        parentColumns = ["id"],
        childColumns = ["parentId"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )]
)
data class ExpressionEntity (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentId: Long,
    val name: String = "",
    val text: String = "",
    val resultText: String = "",
    val resultType: LiteralType = LiteralType.NONE,
    val errorText: String = "",
    val isStatic: Boolean = true,
    val updated: Boolean = true,
){
    fun toExpression(): Expression = Expression(
        id = id,
        parentId = parentId,
        name = name,
        text = text,
        updated = updated,
        parseResult = ParseResult(
            resultType = resultType,
            result = resultType.tryToParse(resultText),
            errorText = errorText,
            isStatic = isStatic
        )
    )

    companion object{
        fun fromExpression(expression: Expression): ExpressionEntity = ExpressionEntity(
            id = expression.id,
            parentId = expression.parentId,
            name = expression.name,
            text = expression.text,
            updated = expression.updated,
            resultText = expression.resultText,
            resultType = expression.parseResult.resultType,
            errorText = expression.parseResult.errorText,
            isStatic = expression.parseResult.isStatic
        )
    }

}