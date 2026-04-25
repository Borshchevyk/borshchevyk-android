package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class UploadAvatarUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(fileBytes: ByteArray, filename: String, contentType: String): String {
        return mediaRepository.uploadAvatar(fileBytes, filename, contentType)
    }
}
