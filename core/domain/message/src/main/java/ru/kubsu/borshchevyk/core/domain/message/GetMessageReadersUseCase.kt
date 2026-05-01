package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

/**
 * Use case for retrieving the list of users who have read a specific message.
 *
 * This use case fetches the read receipts for a given message, returning a list of
 * users who have seen it. This is typically used in group chats or one-on-one chats
 * to display read status.
 *
 * @property messageRepository The repository responsible for message operations.
 */
class GetMessageReadersUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves the readers of the specified message.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message to check for read receipts.
     * @return A list of [User] objects representing the users who have read the message.
     */
    suspend operator fun invoke(chatId: String, messageId: String): List<User> {
        return messageRepository.getMessageReaders(chatId, messageId)
    }
}
