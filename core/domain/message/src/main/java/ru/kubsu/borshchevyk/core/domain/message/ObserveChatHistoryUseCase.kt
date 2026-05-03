package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

/**
 * Use case for observing the historical and live list of messages for a chat.
 *
 * This use case provides a reactive stream representing the current state of a chat's
 * message list. It typically emits an initial cached list and then subsequent
 * lists whenever new messages arrive or existing messages are modified/deleted.
 *
 * @property messageRepository The repository providing the message stream.
 */
class ObserveChatHistoryUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a reactive stream of messages for the given chat.
     *
     * @param chatId The unique identifier of the chat.
     * @return A [Flow] emitting the list of [Message] objects, updated reactively.
     */
    operator fun invoke(chatId: String): Flow<List<Message>> {
        return messageRepository.observeChatHistory(chatId)
    }
}
