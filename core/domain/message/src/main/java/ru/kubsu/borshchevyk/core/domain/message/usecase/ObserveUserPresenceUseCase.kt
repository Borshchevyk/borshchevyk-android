package ru.kubsu.borshchevyk.core.domain.message.usecase

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainPresenceStatus
import javax.inject.Inject

class ObserveUserPresenceUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(userId: String): Flow<DomainPresenceStatus> {
        return messageRepository.observePresence(userId)
    }
}
