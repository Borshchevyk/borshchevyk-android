package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing message pin events within a specific chat.
 *
 * This use case listens for notifications indicating that a message has been
 * pinned by a participant in the chat.
 *
 * @property messageRepository The repository providing the pin event stream.
 */
class ObservePinsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a reactive stream of message IDs that have been pinned.
     *
     * @param chatId The unique identifier of the chat to observe.
     * @return A [Flow] emitting the string IDs of newly pinned messages.
     */
    operator fun invoke(chatId: String): Flow<String> {
        return messageRepository.observePins(chatId)
    }
}
