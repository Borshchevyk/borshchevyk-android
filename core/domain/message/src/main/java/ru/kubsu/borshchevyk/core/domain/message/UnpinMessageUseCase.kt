package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for unpinning a previously pinned message in a chat.
 *
 * This use case removes the "pinned" status from a message, removing it from
 * the highlighted view for all participants in the chat.
 *
 * @property messageRepository The repository responsible for message operations.
 */
class UnpinMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Unpins the specified message.
     *
     * @param chatId The unique identifier of the chat containing the pinned message.
     * @param messageId The unique identifier of the message to unpin.
     */
    suspend operator fun invoke(chatId: String, messageId: String) {
        messageRepository.unpinMessage(chatId, messageId)
    }
}
