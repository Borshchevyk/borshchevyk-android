package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for marking a specific message as read by the current user.
 *
 * This use case updates the read status of a message, which typically triggers
 * a read receipt event for other participants in the chat.
 *
 * @property messageRepository The repository responsible for message operations.
 */
class MarkMessageAsReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Marks the specified message as read.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message to be marked as read.
     */
    suspend operator fun invoke(chatId: String, messageId: String) {
        messageRepository.readMessage(chatId, messageId)
    }
}
