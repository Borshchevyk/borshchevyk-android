package ru.kubsu.borshchevyk.core.network.media

import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.model.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.model.dto.UploadUrlResult
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsResponse

interface MediaNetworkDataSource {
    suspend fun requestUploadUrl(request: RequestUploadUrlRequest): UploadUrlResult
    suspend fun uploadToS3(url: String, fileBytes: ByteArray, contentType: String)
    suspend fun completeUpload(attachmentId: String): AttachmentResponse
    suspend fun uploadAvatar(fileBytes: ByteArray, filename: String, contentType: String): AttachmentUrlResult
    
    suspend fun getAttachmentUrl(attachmentId: String): AttachmentUrlResult
    suspend fun deleteAttachment(attachmentId: String)
    suspend fun validateAttachments(request: ValidateAttachmentsRequest): ValidateAttachmentsResponse
}
