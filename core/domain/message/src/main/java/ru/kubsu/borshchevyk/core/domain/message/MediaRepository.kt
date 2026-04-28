package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentResponse
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType

interface MediaRepository {
    suspend fun requestUploadUrl(
        originalFilename: String,
        contentType: String,
        extension: String,
        type: DomainAttachmentType,
        sizeBytes: Long,
        width: Int? = null,
        height: Int? = null,
        duration: Double? = null
    ): Pair<String, String> // returns Pair(attachmentId, uploadUrl)

    suspend fun uploadToS3(url: String, fileBytes: ByteArray, contentType: String)

    suspend fun completeUpload(attachmentId: String): DomainAttachmentResponse

    suspend fun uploadAvatar(fileBytes: ByteArray, filename: String, contentType: String): String

    suspend fun uploadVoice(fileBytes: ByteArray, duration: Double): DomainAttachmentResponse

    suspend fun uploadCircle(fileBytes: ByteArray, duration: Double): DomainAttachmentResponse

    suspend fun getAttachmentUrl(attachmentId: String): String

    suspend fun deleteAttachment(attachmentId: String)

    suspend fun validateAttachments(attachmentIds: List<String>): Boolean
}
