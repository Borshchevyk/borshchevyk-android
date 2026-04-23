package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType

interface MediaRepository {
    suspend fun requestUploadUrl(
        originalFilename: String,
        contentType: String,
        extension: String,
        type: AttachmentType,
        sizeBytes: Long,
        width: Int? = null,
        height: Int? = null,
        duration: Double? = null
    ): Pair<String, String> // returns Pair(attachmentId, uploadUrl)
    
    suspend fun uploadToS3(url: String, fileBytes: ByteArray, contentType: String)
    
    suspend fun completeUpload(attachmentId: String): AttachmentResponse
    
    suspend fun getAttachmentUrl(attachmentId: String): String
    
    suspend fun deleteAttachment(attachmentId: String)
    
    suspend fun validateAttachments(attachmentIds: List<String>): Boolean
}
