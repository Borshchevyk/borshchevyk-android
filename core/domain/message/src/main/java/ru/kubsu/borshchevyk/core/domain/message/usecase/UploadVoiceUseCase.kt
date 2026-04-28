package ru.kubsu.borshchevyk.core.domain.message.usecase

import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentResponse
import javax.inject.Inject

class UploadVoiceUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(fileBytes: ByteArray, duration: Double): DomainAttachmentResponse {
        return mediaRepository.uploadVoice(fileBytes, duration)
    }
}
