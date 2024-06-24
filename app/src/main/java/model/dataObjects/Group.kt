package model.dataObjects

data class Group (
    val id: Long = 0,
    val parentId: Long?,
    val name: String,
    //val path: String?,
    //val fullpath: String =  "${path ?: ""}/$name",
    val templateName: String,
)