package model.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import model.dataObjects.Group

@Entity(
    indices = [Index(value = ["id"], unique = true), Index(value = ["parentId"])],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroupEntity (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentId: Long? = null,
    val name: String,
    val templateName: String = "",
) {
    fun toGroup(): Group = Group(
        id = id,
        parentId = parentId,
        name = name,
        //path = path,
        templateName = templateName
    )

    companion object{
        fun fromExpressionGroup(group: Group): GroupEntity =
            GroupEntity(
                id = group.id,
                parentId = group.parentId,
                name = group.name,
                templateName = group.templateName
            )

    }
}