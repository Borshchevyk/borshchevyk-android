package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for resolving the direct thumbnail URL of a stored attachment.
 *
 * @property mediaRepository The repository responsible for media operations.
 */
class GetThumbnailUrlUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    /**
     * Retrieves the thumbnail URL for the specified attachment.
     *
     * @param attachmentId The unique identifier of the attachment.
     * @return The direct URL string pointing to the attachment's thumbnail resource.
     */
    suspend operator fun invoke(attachmentId: String): String {
        return mediaRepository.getAttachmentThumbnailUrl(attachmentId)
    }
}
