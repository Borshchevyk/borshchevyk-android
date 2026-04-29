package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["messageId"])]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val type: DomainAttachmentType,
    val originalFilename: String,
    val extension: String,
    val sizeBytes: Long,
    val thumbnailKey: String?,
    val updatedAt: String?,
    val width: Int?,
    val height: Int?,
    val duration: Int?
)
