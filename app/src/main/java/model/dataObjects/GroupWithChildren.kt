package model.dataObjects

data class GroupWithChildren (
    val id: Long = 0,
    val parentId: Long?,
    val name: String,
    //val path: String?,
    //val fullpath: String =  "${path ?: ""}/$name",
    val templateName: String,
    val childExpressions: List<Expression>,
    val childGroups: List<Group>
)