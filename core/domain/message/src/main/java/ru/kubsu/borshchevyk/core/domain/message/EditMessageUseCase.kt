package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

/**
 * Use case for editing the text content of an existing message.
 *
 * This use case allows modifying a previously sent message, typically subject to
 * business rules (e.g., within a certain time frame or only by the original sender).
 *
 * @property messageRepository The repository responsible for message operations.
 */
class EditMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Edits the text of the specified message.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message to edit.
     * @param newText The updated text content for the message.
     */
    suspend operator fun invoke(chatId: String, messageId: String, newText: String) {
        messageRepository.editMessage(chatId, messageId, newText)
    }
}
