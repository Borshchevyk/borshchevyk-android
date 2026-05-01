package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

/**
 * Use case for retrieving all pinned messages within a specific chat.
 *
 * This use case fetches the list of messages that have been explicitly pinned
 * by participants in the chat, usually for quick access or visibility.
 *
 * @property messageRepository The repository responsible for message operations.
 */
class GetPinnedMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves the pinned messages for the specified chat.
     *
     * @param chatId The unique identifier of the chat.
     * @return A list of [Message] objects that are currently pinned in the chat.
     */
    suspend operator fun invoke(chatId: String): List<Message> {
        return messageRepository.getPinnedMessages(chatId)
    }
}
