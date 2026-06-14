package ru.kubsu.borshchevyk.core.domain.message.usecase

import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentResponse
import javax.inject.Inject

/**
 * Use case for uploading a circular video message.
 *
 * This use case handles the specific logic for processing and uploading
 * short, circular video recordings (often referred to as "circles" or "video notes").
 *
 * @property mediaRepository The repository responsible for media operations.
 */
class UploadCircleUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    /**
     * Uploads the recorded circular video.
     *
     * @param fileBytes The raw byte array of the encoded video file.
     * @param duration The duration of the video in seconds.
     * @return A [DomainAttachmentResponse] representing the finalized video attachment.
     */
    suspend operator fun invoke(inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, duration: Double): DomainAttachmentResponse {
        return mediaRepository.uploadCircle(inputStreamProvider, sizeBytes, duration)
    }
}
