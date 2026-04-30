package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType

/**
 * Represents a file attachment linked to a specific message.
 *
 * Linked to [MessageEntity] via `messageId`. Deleting the parent message results in the automatic
 * deletion of this attachment.
 *
 * @property id The unique identifier of the attachment.
 * @property messageId The ID of the message this attachment belongs to.
 * @property type The type of the attachment (e.g., IMAGE, VIDEO, FILE).
 * @property originalFilename The original name of the file.
 * @property extension The file extension (e.g., jpg, pdf).
 * @property sizeBytes The size of the file in bytes.
 * @property thumbnailKey A key used to retrieve the thumbnail from local cache.
 * @property updatedAt Timestamp of the last update.
 * @property width The width (for visual media).
 * @property height The height (for visual media).
 * @property duration The duration in seconds (for audio/video).
 */
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
