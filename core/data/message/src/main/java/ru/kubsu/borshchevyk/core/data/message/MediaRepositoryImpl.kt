package ru.kubsu.borshchevyk.core.data.message

import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.network.MediaNetworkDataSource
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val networkDataSource: MediaNetworkDataSource
) : MediaRepository {

    override suspend fun uploadFile(
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        type: AttachmentType,
        width: Int?,
        height: Int?,
        duration: Double?
    ): AttachmentResponse {
        return networkDataSource.uploadFile(
            fileBytes = fileBytes,
            fileName = fileName,
            contentType = contentType,
            type = type,
            width = width,
            height = height,
            duration = duration
        )
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
}
