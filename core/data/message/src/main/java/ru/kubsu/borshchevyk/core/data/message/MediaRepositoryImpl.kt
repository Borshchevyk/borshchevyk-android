package ru.kubsu.borshchevyk.core.data.message

import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
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
        type: AttachmentType,
        sizeBytes: Long,
        width: Int?,
        height: Int?,
        duration: Double?
    ): Pair<String, String> {
        val result = networkDataSource.requestUploadUrl(
            RequestUploadUrlRequest(
                type = type,
                contentType = contentType,
                originalFilename = originalFilename,
                extension = extension,
                sizeBytes = sizeBytes,
                width = width,
                height = height,
                duration = duration
            )
        )
        return Pair(result.attachmentId, result.uploadUrl)
    }

    override suspend fun uploadToS3(url: String, fileBytes: ByteArray, contentType: String) {
        networkDataSource.uploadToS3(url, fileBytes, contentType)
    }

    override suspend fun completeUpload(attachmentId: String): AttachmentResponse {
        return networkDataSource.completeUpload(attachmentId)
    }

    override suspend fun getAttachmentUrl(attachmentId: String): String {
        val url = networkDataSource.getAttachmentUrl(attachmentId).url
        return if (url.startsWith("http")) {
            url
        } else {
            "${NetworkConstants.BASE_URL}${if (url.startsWith("/")) "" else "/"}$url"
        }
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        networkDataSource.deleteAttachment(attachmentId)
    }

    override suspend fun validateAttachments(attachmentIds: List<String>): Boolean {
        return networkDataSource.validateAttachments(ValidateAttachmentsRequest(attachmentIds)).valid
    }
}
