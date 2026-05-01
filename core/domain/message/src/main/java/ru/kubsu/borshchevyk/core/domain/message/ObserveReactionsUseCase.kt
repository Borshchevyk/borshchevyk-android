package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainReactionEvent
import javax.inject.Inject

/**
 * Use case for observing real-time reaction updates within a specific chat.
 *
 * This use case provides a stream of events that occur when a user adds or
 * removes a reaction (e.g., a thumbs up or a heart) to a message.
 *
 * @property messageRepository The repository providing the reaction event stream.
 */
class ObserveReactionsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a reactive stream of reaction events for the specified chat.
     *
     * @param chatId The unique identifier of the chat to observe.
     * @return A [Flow] emitting [DomainReactionEvent] instances as reactions change.
     */
    operator fun invoke(chatId: String): Flow<DomainReactionEvent> {
        return messageRepository.observeReactions(chatId)
    }
}
