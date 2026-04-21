package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class GetAttachmentUrlUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(attachmentId: String): String {
        return mediaRepository.getAttachmentUrl(attachmentId)
    }
}
