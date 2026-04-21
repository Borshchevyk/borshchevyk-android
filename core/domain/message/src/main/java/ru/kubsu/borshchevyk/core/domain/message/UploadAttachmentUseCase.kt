package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
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
        val response = mediaRepository.uploadFile(
            fileBytes = fileBytes,
            fileName = originalFilename,
            contentType = contentType,
            type = type,
            width = width,
            height = height,
            duration = duration
        )
        return response.id
    }
}
