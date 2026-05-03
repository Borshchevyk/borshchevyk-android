package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing message unpin events within a specific chat.
 *
 * This use case listens for notifications indicating that a previously pinned
 * message has been unpinned by a participant in the chat.
 *
 * @property messageRepository The repository providing the unpin event stream.
 */
class ObserveUnpinsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a reactive stream of message IDs that have been unpinned.
     *
     * @param chatId The unique identifier of the chat to observe.
     * @return A [Flow] emitting the string IDs of messages that were unpinned.
     */
    operator fun invoke(chatId: String): Flow<String> {
        return messageRepository.observeUnpins(chatId)
    }
}
