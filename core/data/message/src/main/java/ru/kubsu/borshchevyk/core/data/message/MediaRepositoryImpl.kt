package ru.kubsu.borshchevyk.core.data.message

import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.model.dto.UploadUrlResult
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.network.MediaNetworkDataSource
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val networkDataSource: MediaNetworkDataSource
) : MediaRepository {

    override suspend fun requestUploadUrl(request: RequestUploadUrlRequest): UploadUrlResult {
        return networkDataSource.requestUploadUrl(request)
    }

    override suspend fun completeUpload(attachmentId: String): AttachmentResponse {
        return networkDataSource.completeUpload(attachmentId)
    }

    override suspend fun getAttachmentUrl(attachmentId: String): String {
        return networkDataSource.getAttachmentUrl(attachmentId).url
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        networkDataSource.deleteAttachment(attachmentId)
    }

    override suspend fun validateAttachments(attachmentIds: List<String>): Boolean {
        return networkDataSource.validateAttachments(ValidateAttachmentsRequest(attachmentIds)).valid
    }

    override suspend fun uploadFileToS3(url: String, fileBytes: ByteArray, contentType: String) {
        networkDataSource.uploadFileToS3(url, fileBytes, contentType)
    }
}
