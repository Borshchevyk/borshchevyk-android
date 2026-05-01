package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for uploading a user or chat avatar image.
 *
 * This use case handles the business logic for processing and uploading profile
 * pictures, delegating the network operation to the [MediaRepository].
 *
 * @property mediaRepository The repository responsible for media operations.
 */
class UploadAvatarUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    /**
     * Uploads the avatar image.
     *
     * @param fileBytes The raw byte array of the image.
     * @param filename The intended filename for the avatar.
     * @param contentType The MIME type of the image (e.g., "image/jpeg").
     * @return The string URL or unique identifier of the uploaded avatar.
     */
    suspend operator fun invoke(fileBytes: ByteArray, filename: String, contentType: String): String {
        return mediaRepository.uploadAvatar(fileBytes, filename, contentType)
    }
}
