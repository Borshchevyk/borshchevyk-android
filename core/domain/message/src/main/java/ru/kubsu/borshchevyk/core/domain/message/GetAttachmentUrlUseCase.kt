package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for resolving the direct URL of a stored attachment.
 *
 * This use case fetches the fully qualified URL required to download or display
 * a given attachment (e.g., an image, video, or document) stored on the server.
 *
 * @property mediaRepository The repository responsible for media operations.
 */
class GetAttachmentUrlUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    /**
     * Retrieves the URL for the specified attachment.
     *
     * @param attachmentId The unique identifier of the attachment.
     * @return The direct URL string pointing to the attachment resource.
     */
    suspend operator fun invoke(attachmentId: String): String {
        return mediaRepository.getAttachmentUrl(attachmentId)
    }
}
