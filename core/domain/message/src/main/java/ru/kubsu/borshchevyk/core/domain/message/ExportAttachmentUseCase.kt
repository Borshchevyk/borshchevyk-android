package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class ExportAttachmentUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(attachmentId: String): Result<String> {
        return mediaRepository.exportAttachment(attachmentId)
    }
}