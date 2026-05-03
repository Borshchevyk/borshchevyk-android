package ru.kubsu.borshchevyk.core.domain.message.usecase

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainPresenceStatus
import javax.inject.Inject

/**
 * Use case for observing the online presence status of a specific user.
 *
 * This use case provides a reactive stream that emits updates when the target user
 * comes online, goes offline, or changes their active status. This is crucial for
 * rendering "Online" indicators in the UI.
 *
 * @property messageRepository The repository providing the presence event stream.
 */
class ObserveUserPresenceUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a reactive stream of presence status updates for the specified user.
     *
     * @param userId The unique identifier of the user to observe.
     * @return A [Flow] emitting [DomainPresenceStatus] objects as the user's status changes.
     */
    operator fun invoke(userId: String): Flow<DomainPresenceStatus> {
        return messageRepository.observePresence(userId)
    }
}
