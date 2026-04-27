package ru.kubsu.borshchevyk.core.domain.message.usecase

import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import javax.inject.Inject

class UploadCircleUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(fileBytes: ByteArray, duration: Double): AttachmentResponse {
        return mediaRepository.uploadCircle(fileBytes, duration)
    }
}
