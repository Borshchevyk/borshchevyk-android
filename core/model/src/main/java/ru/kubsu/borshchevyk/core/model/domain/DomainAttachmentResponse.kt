package ru.kubsu.borshchevyk.core.model.domain

/**
 * Domain model representing the response from the server after an attachment is uploaded or fetched.
 *
 * It contains the metadata required to reconstruct or display the file on the client side.
 *
 * @property id Unique identifier for the attachment.
 * @property type The type of the attachment (photo, video, etc.).
 * @property originalFilename The original filename provided during upload.
 * @property extension The file extension of the attachment.
 * @property sizeBytes The size of the file in bytes.
 * @property thumbnailKey A key or URL to access the generated thumbnail, if available.
 * @property updatedAt ISO 8601 formatted timestamp of the last update.
 * @property width The width in pixels, if the attachment is an image or video.
 * @property height The height in pixels, if the attachment is an image or video.
 * @property duration The playback duration in seconds, if the attachment is audio or video.
 */
data class DomainAttachmentResponse(
    val id: String,
    val type: DomainAttachmentType?,
    val originalFilename: String?,
    val extension: String?,
    val sizeBytes: Long?,
    val thumbnailKey: String?,
    val updatedAt: String?,
    val width: Int?,
    val height: Int?,
    val duration: Double?
)
