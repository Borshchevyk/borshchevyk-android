package ru.kubsu.borshchevyk.core.domain.message.usecase

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import ru.kubsu.borshchevyk.core.model.dto.PresenceStatusResponse
import javax.inject.Inject

class ObserveUserPresenceUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(userId: String): Flow<PresenceStatusResponse> {
        return messageRepository.observePresence(userId)
    }
}
