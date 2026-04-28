package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import javax.inject.Inject

class UploadAttachmentUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(
        fileBytes: ByteArray,
        originalFilename: String,
        contentType: String,
        extension: String,
        type: DomainAttachmentType,
        width: Int? = null,
        height: Int? = null,
        duration: Double? = null
    ): String {
        // Step 1: Request pre-signed URL from backend
        val (attachmentId, uploadUrl) = mediaRepository.requestUploadUrl(
            originalFilename = originalFilename,
            contentType = contentType,
            extension = extension,
            type = type,
            sizeBytes = fileBytes.size.toLong(),
            width = width,
            height = height,
            duration = duration
        )

        // Step 2: Upload raw bytes directly to S3 using the pre-signed URL
        mediaRepository.uploadToS3(
            url = uploadUrl,
            fileBytes = fileBytes,
            contentType = contentType
        )

        // Step 3: Notify backend that upload is complete
        val completedAttachment = mediaRepository.completeUpload(attachmentId)

        return completedAttachment.id
    }
}
