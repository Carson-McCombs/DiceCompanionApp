package model.database.entity.relationship

import androidx.room.Embedded
import androidx.room.Relation
import model.dataObjects.GroupWithChildren
import model.database.entity.ExpressionEntity
import model.database.entity.GroupEntity

data class GroupWithChildrenEntity (
    @Embedded
    val groupEntity: GroupEntity,
    @Relation(
        entity = GroupEntity::class,
        parentColumn = "id",
        entityColumn = "parentId"
    )
    val childExpressionGroupEntities: List<GroupEntity>,
    @Relation(
        entity = ExpressionEntity::class,
        parentColumn = "id",
        entityColumn = "parentId"
    )
    val childExpressionEntities: List<ExpressionEntity>
) {
    fun toExpressionGroupWithChildren(): GroupWithChildren =
        GroupWithChildren(
            id = groupEntity.id,
            parentId = groupEntity.parentId,
            name = groupEntity.name,
            templateName = groupEntity.templateName,
            childExpressions = childExpressionEntities.map { expressionEntity -> expressionEntity.toExpression() },
            childGroups =  childExpressionGroupEntities.map { expressionGroupEntity -> expressionGroupEntity.toGroup() }
        )
}