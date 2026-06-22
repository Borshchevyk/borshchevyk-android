package ru.kubsu.borshchevyk.core.domain.message.usecase

import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentResponse
import javax.inject.Inject

/**
 * Use case for uploading a voice message.
 *
 * This use case handles the logic for taking a recorded audio file (voice note)
 * and uploading it securely to the storage infrastructure.
 *
 * @property mediaRepository The repository responsible for media operations.
 */
class UploadVoiceUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    /**
     * Uploads the recorded voice message.
     *
     * @param fileBytes The raw byte array of the encoded audio file.
     * @param duration The duration of the audio recording in seconds.
     * @return A [DomainAttachmentResponse] representing the finalized voice attachment.
     */
    suspend operator fun invoke(inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, duration: Double): DomainAttachmentResponse {
        return mediaRepository.uploadVoice(inputStreamProvider, sizeBytes, duration)
    }
}
