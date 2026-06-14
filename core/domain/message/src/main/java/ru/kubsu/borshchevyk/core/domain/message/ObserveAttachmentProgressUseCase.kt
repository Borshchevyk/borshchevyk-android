package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAttachmentProgressUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(attachmentId: String): Flow<Float> {
        return repository.observeAttachmentProgress(attachmentId)
    }
}
