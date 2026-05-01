package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Domain model representing a generic file attachment in a chat or message.
 *
 * This class encapsulates both metadata and file characteristics for various
 * media types such as images, videos, audio, or standard files.
 *
 * @property id Unique identifier for the attachment.
 * @property type The type of attachment (e.g., IMAGE, VIDEO, FILE).
 * @property originalFilename The original name of the file before upload.
 * @property extension The file extension (e.g., "jpg", "pdf").
 * @property sizeBytes The size of the file in bytes.
 * @property url The direct URL to download or access the attachment.
 * @property thumbnailKey A key or URL to retrieve the generated thumbnail (if any).
 * @property updatedAt ISO 8601 formatted timestamp of the last update.
 * @property width The width of the image or video in pixels, if applicable.
 * @property height The height of the image or video in pixels, if applicable.
 * @property duration The duration of the video or audio in seconds, if applicable.
 */
@Serializable
data class Attachment(
    val id: String,
    val type: DomainAttachmentType,
    val originalFilename: String = "file",
    val extension: String = "",
    val sizeBytes: Long = 0,
    val url: String? = null,
    val thumbnailKey: String? = null,
    val updatedAt: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null
)
