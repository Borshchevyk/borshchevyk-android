package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Enumerates the supported types of media attachments.
 */
@Serializable
enum class AttachmentType {
    PHOTO,
    VIDEO,
    CIRCLE,
    VOICE,
    FILE,
    STICKER,
    AVATAR
}

/**
 * Enumerates the possible lifecycle statuses of an attachment upload.
 */
@Serializable
enum class AttachmentStatus {
    INITIALIZED,
    UPLOADING,
    UPLOADED,
    PROCESSING,
    READY,
    FAILED,
    DELETED
}

/**
 * The standard response object containing complete details about a media attachment.
 *
 * @property id The unique identifier of the attachment.
 * @property uploaderId The unique identifier of the user who uploaded the attachment.
 * @property type The specific type of the attachment (e.g., PHOTO, VIDEO).
 * @property s3Key The storage key used to reference the original file in the S3 bucket.
 * @property thumbnailKey The storage key used to reference the thumbnail in the S3 bucket, if available.
 * @property originalFilename The original name of the file before it was uploaded.
 * @property extension The file extension (e.g., "png", "mp4").
 * @property contentType The MIME type of the file (e.g., "image/jpeg").
 * @property sizeBytes The total size of the file in bytes.
 * @property width The width of the media in pixels, if applicable.
 * @property height The height of the media in pixels, if applicable.
 * @property duration The duration of the media in seconds, if applicable.
 * @property status The current processing or upload status of the attachment.
 * @property createdAt The timestamp when the attachment was created.
 * @property updatedAt The timestamp when the attachment was last updated.
 */
@Serializable
data class AttachmentResponse(
    val id: String,
    val uploaderId: String,
    val type: AttachmentType,
    val s3Key: String,
    val thumbnailKey: String? = null,
    val originalFilename: String,
    val extension: String,
    val contentType: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null,
    val status: AttachmentStatus,
    val createdAt: String,
    val updatedAt: String? = null
)

/**
 * Response object containing a signed URL for direct access to an attachment.
 *
 * @property url The signed URL providing temporary access to download the attachment.
 */
@Serializable
data class AttachmentUrlResult(
    val url: String
)

/**
 * Request object to obtain a pre-signed URL for uploading a new attachment.
 *
 * @property type The type of attachment being uploaded.
 * @property contentType The MIME type of the file to be uploaded.
 * @property originalFilename The original name of the file.
 * @property extension The file extension.
 * @property sizeBytes The expected size of the file in bytes.
 * @property width The width of the media, if applicable.
 * @property height The height of the media, if applicable.
 * @property duration The duration of the media, if applicable.
 */
@Serializable
data class RequestUploadUrlRequest(
    val type: AttachmentType,
    val contentType: String,
    val originalFilename: String,
    val extension: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null
)

/**
 * Response object containing a pre-signed URL for client-side upload.
 *
 * @property attachmentId The assigned ID for the new attachment.
 * @property uploadUrl The signed URL where the client should PUT the file.
 * @property s3Key The assigned storage key for the file.
 */
@Serializable
data class UploadUrlResult(
    val attachmentId: String,
    val uploadUrl: String,
    val s3Key: String
)

/**
 * Request object to validate a list of uploaded attachments.
 *
 * @property attachmentIds The list of attachment IDs to validate.
 */
@Serializable
data class ValidateAttachmentsRequest(
    val attachmentIds: List<String>
)

/**
 * Response object indicating the validation result for attachments.
 *
 * @property valid True if all requested attachments are valid and ready.
 * @property attachments Optional list of metadata for the validated attachments.
 */
@Serializable
data class ValidateAttachmentsResponse(
    val valid: Boolean,
    val attachments: List<AttachmentMetadataResponse>? = null
)

/**
 * Metadata response detailing basic properties of an attachment.
 *
 * @property id The unique identifier of the attachment.
 * @property type The type of the attachment.
 * @property originalFilename The original name of the file.
 * @property extension The file extension.
 * @property sizeBytes The size of the file in bytes.
 */
@Serializable
data class AttachmentMetadataResponse(
    val id: String,
    val type: String,
    val originalFilename: String,
    val extension: String,
    val sizeBytes: Long
)
