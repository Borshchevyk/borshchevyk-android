package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing real-time message deletion events.
 *
 * This use case listens for notifications from the messaging infrastructure
 * indicating that a specific message has been deleted.
 *
 * @property messageRepository The repository providing the deleted message events.
 */
class ObserveDeletedMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a reactive stream of deleted message identifiers.
     *
     * @return A [Flow] emitting the string IDs of messages that have just been deleted.
     */
    operator fun invoke(): Flow<String> {
        return messageRepository.observeDeletedMessages()
    }
}
