package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.core.model.dto.RequestUploadUrlRequest
import javax.inject.Inject

class UploadAttachmentUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(
        fileBytes: ByteArray,
        originalFilename: String,
        contentType: String,
        extension: String,
        type: AttachmentType,
        width: Int? = null,
        height: Int? = null,
        duration: Double? = null
    ): String {
        val request = RequestUploadUrlRequest(
            type = type,
            contentType = contentType,
            originalFilename = originalFilename,
            extension = extension,
            sizeBytes = fileBytes.size.toLong(),
            width = width,
            height = height,
            duration = duration
        )
        
        val urlResult = mediaRepository.requestUploadUrl(request)
        mediaRepository.uploadFileToS3(urlResult.uploadUrl, fileBytes, contentType)
        mediaRepository.completeUpload(urlResult.attachmentId)
        
        return urlResult.attachmentId
    }
}
