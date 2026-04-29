package ru.kubsu.borshchevyk.core.network.media

import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.network.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.network.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.network.dto.UploadUrlResult
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsResponse

interface MediaNetworkDataSource {
    suspend fun requestUploadUrl(request: RequestUploadUrlRequest): NetworkResult<UploadUrlResult>
    suspend fun uploadToS3(url: String, fileBytes: ByteArray, contentType: String): NetworkResult<Unit>
    suspend fun completeUpload(attachmentId: String): NetworkResult<AttachmentResponse>
    suspend fun uploadAvatar(fileBytes: ByteArray, filename: String, contentType: String): NetworkResult<AttachmentUrlResult>
    suspend fun uploadVoice(fileBytes: ByteArray, duration: Double): NetworkResult<AttachmentResponse>
    suspend fun uploadCircle(fileBytes: ByteArray, duration: Double): NetworkResult<AttachmentResponse>

    suspend fun getAttachmentUrl(attachmentId: String): NetworkResult<AttachmentUrlResult>
    suspend fun deleteAttachment(attachmentId: String): NetworkResult<Unit>
    suspend fun validateAttachments(request: ValidateAttachmentsRequest): NetworkResult<ValidateAttachmentsResponse>
}
