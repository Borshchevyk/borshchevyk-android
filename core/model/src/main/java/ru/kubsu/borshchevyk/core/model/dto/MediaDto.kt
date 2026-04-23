package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
enum class AttachmentType {
    PHOTO,
    VIDEO,
    CIRCLE,
    VOICE,
    FILE,
    STICKER
}

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

@Serializable
data class AttachmentUrlResult(
    val url: String
)

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

@Serializable
data class UploadUrlResult(
    val attachmentId: String,
    val uploadUrl: String,
    val s3Key: String
)

@Serializable
data class ValidateAttachmentsRequest(
    val attachmentIds: List<String>
)

@Serializable
data class ValidateAttachmentsResponse(
    val valid: Boolean
)
