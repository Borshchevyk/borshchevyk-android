package ru.kubsu.borshchevyk.core.data.message

import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentResponse
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.model.domain.getOrThrow
import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.core.model.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.network.client.NetworkConstants
import ru.kubsu.borshchevyk.core.network.media.MediaNetworkDataSource
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val networkDataSource: MediaNetworkDataSource
) : MediaRepository {

    override suspend fun requestUploadUrl(
        originalFilename: String,
        contentType: String,
        extension: String,
        type: DomainAttachmentType,
        sizeBytes: Long,
        width: Int?,
        height: Int?,
        duration: Double?
    ): Pair<String, String> {
        val result = networkDataSource.requestUploadUrl(
            RequestUploadUrlRequest(
                type = type.toDto(),
                contentType = contentType,
                originalFilename = originalFilename,
                extension = extension,
                sizeBytes = sizeBytes,
                width = width,
                height = height,
                duration = duration
            )
        ).getOrThrow()
        return Pair(result.attachmentId, result.uploadUrl)
    }

    override suspend fun uploadToS3(url: String, fileBytes: ByteArray, contentType: String) {
        networkDataSource.uploadToS3(url, fileBytes, contentType).getOrThrow()
    }

    override suspend fun completeUpload(attachmentId: String): DomainAttachmentResponse {
        return networkDataSource.completeUpload(attachmentId).getOrThrow().toDomain()
    }

    override suspend fun uploadAvatar(fileBytes: ByteArray, filename: String, contentType: String): String {
        val url = networkDataSource.uploadAvatar(fileBytes, filename, contentType).getOrThrow().url
        return if (url.startsWith("http")) {
            url
        } else {
            "${NetworkConstants.BASE_URL}${if (url.startsWith("/")) "" else "/"}$url"
        }
    }

    override suspend fun uploadVoice(fileBytes: ByteArray, duration: Double): DomainAttachmentResponse {
        return networkDataSource.uploadVoice(fileBytes, duration).getOrThrow().toDomain()
    }

    override suspend fun uploadCircle(fileBytes: ByteArray, duration: Double): DomainAttachmentResponse {
        return networkDataSource.uploadCircle(fileBytes, duration).getOrThrow().toDomain()
    }

    override suspend fun getAttachmentUrl(attachmentId: String): String {
        val url = networkDataSource.getAttachmentUrl(attachmentId).getOrThrow().url
        return if (url.startsWith("http")) {
            url
        } else {
            "${NetworkConstants.BASE_URL}${if (url.startsWith("/")) "" else "/"}$url"
        }
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        networkDataSource.deleteAttachment(attachmentId).getOrThrow()
    }

    override suspend fun validateAttachments(attachmentIds: List<String>): Boolean {
        return networkDataSource.validateAttachments(ValidateAttachmentsRequest(attachmentIds)).getOrThrow().valid
    }

    private fun DomainAttachmentType.toDto(): AttachmentType = when (this) {
        DomainAttachmentType.PHOTO -> AttachmentType.PHOTO
        DomainAttachmentType.VIDEO -> AttachmentType.VIDEO
        DomainAttachmentType.VOICE -> AttachmentType.VOICE
        DomainAttachmentType.CIRCLE -> AttachmentType.CIRCLE
        DomainAttachmentType.FILE -> AttachmentType.FILE
        DomainAttachmentType.STICKER -> AttachmentType.STICKER
        DomainAttachmentType.AVATAR -> AttachmentType.AVATAR
    }

    private fun AttachmentType.toDomain(): DomainAttachmentType = when (this) {
        AttachmentType.PHOTO -> DomainAttachmentType.PHOTO
        AttachmentType.VIDEO -> DomainAttachmentType.VIDEO
        AttachmentType.VOICE -> DomainAttachmentType.VOICE
        AttachmentType.CIRCLE -> DomainAttachmentType.CIRCLE
        AttachmentType.FILE -> DomainAttachmentType.FILE
        AttachmentType.STICKER -> DomainAttachmentType.STICKER
        AttachmentType.AVATAR -> DomainAttachmentType.AVATAR
    }

    private fun AttachmentResponse.toDomain(): DomainAttachmentResponse = DomainAttachmentResponse(
        id = id,
        type = type?.toDomain(),
        originalFilename = originalFilename,
        extension = extension,
        sizeBytes = sizeBytes,
        thumbnailKey = thumbnailKey,
        updatedAt = updatedAt,
        width = width,
        height = height,
        duration = duration
    )
}
