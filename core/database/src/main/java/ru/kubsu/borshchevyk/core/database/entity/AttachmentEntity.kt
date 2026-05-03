package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType

/**
 * Represents a file attachment linked to a specific message within the Borshchevyk messenger.
 *
 * Supports attachments of any format transferred over both the global server and P2P mesh networks.
 * Linked to [MessageEntity] via `messageId` with a CASCADE delete constraint, ensuring
 * database integrity by automatically removing attachments when the parent message is deleted.
 *
 * @property id The unique identifier of the attachment.
 * @property messageId The ID of the parent [MessageEntity] this attachment belongs to.
 * @property type The type of the attachment (e.g., IMAGE, VIDEO, AUDIO, FILE).
 * @property originalFilename The original name of the file, preserving the sender's naming.
 * @property extension The file extension (e.g., jpg, pdf, mp3), useful for determining how to open it.
 * @property sizeBytes The size of the file in bytes, used for download progress and storage management.
 * @property thumbnailKey A key used to retrieve a lightweight thumbnail from the local cache without loading the full file.
 * @property updatedAt ISO timestamp indicating when the attachment metadata was last updated.
 * @property width The width in pixels (applicable for visual media like images and videos).
 * @property height The height in pixels (applicable for visual media like images and videos).
 * @property duration The duration in seconds (applicable for audio and video files).
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
