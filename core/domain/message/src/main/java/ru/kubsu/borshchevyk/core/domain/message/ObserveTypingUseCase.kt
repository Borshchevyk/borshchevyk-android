package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainTypingEvent
import javax.inject.Inject

/**
 * Use case for observing typing status events within a specific chat.
 *
 * This use case provides a reactive stream of notifications indicating when
 * another user in the chat starts or stops typing a message.
 *
 * @property messageRepository The repository providing the typing event stream.
 */
class ObserveTypingUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a reactive stream of typing events for the specified chat.
     *
     * @param chatId The unique identifier of the chat to observe.
     * @return A [Flow] emitting [DomainTypingEvent] instances as users type.
     */
    operator fun invoke(chatId: String): Flow<DomainTypingEvent> {
        return messageRepository.observeTyping(chatId)
    }
}
