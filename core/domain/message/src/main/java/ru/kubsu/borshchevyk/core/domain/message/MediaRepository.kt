package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.model.dto.UploadUrlResult

interface MediaRepository {
    suspend fun requestUploadUrl(request: RequestUploadUrlRequest): UploadUrlResult
    suspend fun completeUpload(attachmentId: String): AttachmentResponse
    suspend fun getAttachmentUrl(attachmentId: String): String
    suspend fun deleteAttachment(attachmentId: String)
    suspend fun validateAttachments(attachmentIds: List<String>): Boolean
    suspend fun uploadFileToS3(url: String, fileBytes: ByteArray, contentType: String)
}
