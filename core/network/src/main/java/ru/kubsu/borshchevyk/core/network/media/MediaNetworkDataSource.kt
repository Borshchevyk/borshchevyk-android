package ru.kubsu.borshchevyk.core.network.media

import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.core.model.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsResponse

interface MediaNetworkDataSource {
    suspend fun uploadFile(
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        type: AttachmentType,
        width: Int? = null,
        height: Int? = null,
        duration: Double? = null
    ): AttachmentResponse
    
    suspend fun getAttachmentUrl(attachmentId: String): AttachmentUrlResult
    suspend fun deleteAttachment(attachmentId: String)
    suspend fun validateAttachments(request: ValidateAttachmentsRequest): ValidateAttachmentsResponse
}
