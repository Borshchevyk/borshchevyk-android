package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for deleting an attachment from the media repository.
 *
 * This use case handles the business logic of permanently removing an attachment,
 * delegating the network or local database operation to the [MediaRepository].
 *
 * @property mediaRepository The repository responsible for media operations.
 */
class DeleteAttachmentUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    /**
     * Deletes the specified attachment.
     *
     * @param attachmentId The unique identifier of the attachment to be deleted.
     */
    suspend operator fun invoke(attachmentId: String) {
        mediaRepository.deleteAttachment(attachmentId)
    }
}
