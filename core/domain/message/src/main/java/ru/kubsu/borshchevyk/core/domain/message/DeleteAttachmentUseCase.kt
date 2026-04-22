package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class DeleteAttachmentUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(attachmentId: String) {
        mediaRepository.deleteAttachment(attachmentId)
    }
}
