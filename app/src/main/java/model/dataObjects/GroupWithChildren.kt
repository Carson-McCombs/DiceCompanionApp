package model.dataObjects

data class GroupWithChildren (
    val id: Long = 0,
    val parentId: Long?,
    val name: String,
    val templateName: String,
    val childExpressions: List<Expression>,
    val childGroups: List<Group>
)