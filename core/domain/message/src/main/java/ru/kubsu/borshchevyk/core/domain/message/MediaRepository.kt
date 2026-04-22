package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType

interface MediaRepository {
    suspend fun uploadFile(
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        type: AttachmentType,
        width: Int? = null,
        height: Int? = null,
        duration: Double? = null
    ): AttachmentResponse
    
    suspend fun getAttachmentUrl(attachmentId: String): String
    suspend fun deleteAttachment(attachmentId: String)
    suspend fun validateAttachments(attachmentIds: List<String>): Boolean
}
