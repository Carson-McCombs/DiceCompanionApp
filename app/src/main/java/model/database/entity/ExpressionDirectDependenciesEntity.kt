package model.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import model.dataObjects.Expression

@Entity(
    indices = [Index("id"), Index("dependencyId")],
    primaryKeys = ["id", "dependencyId"],
    foreignKeys = [
        ForeignKey(
            entity = ExpressionEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExpressionEntity::class,
            parentColumns = ["id"],
            childColumns = ["dependencyId"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class ExpressionDirectDependenciesEntity (
    val id: Long,
    val dependencyId: Long,
) {
    companion object{
        fun fromExpression(expression: Expression): List<ExpressionDirectDependenciesEntity> =
            expression.parseResult.directDependencies.map { expressionDependencyId -> ExpressionDirectDependenciesEntity(id = expression.id, dependencyId = expressionDependencyId) }
    }
}