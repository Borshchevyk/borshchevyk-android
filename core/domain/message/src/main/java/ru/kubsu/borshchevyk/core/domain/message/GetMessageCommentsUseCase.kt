package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

/**
 * Use case for retrieving comments (replies) associated with a specific message.
 *
 * This use case fetches a paginated list of messages that form a thread or
 * comment section under a root message within a chat.
 *
 * @property messageRepository The repository responsible for message operations.
 */
class GetMessageCommentsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a paginated list of comments for the specified message.
     *
     * @param chatId The unique identifier of the chat containing the root message.
     * @param messageId The unique identifier of the root message to fetch comments for.
     * @param page The zero-based page index for pagination. Defaults to 0.
     * @param size The maximum number of comments to return per page. Defaults to 50.
     * @return A list of [Message] objects representing the comments.
     */
    suspend operator fun invoke(chatId: String, messageId: String, page: Int = 0, size: Int = 50): List<Message> {
        return messageRepository.getMessageComments(chatId, messageId, page, size)
    }
}
