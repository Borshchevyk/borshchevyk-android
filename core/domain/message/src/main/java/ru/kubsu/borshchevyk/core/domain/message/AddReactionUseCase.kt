package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for adding a reaction to a specific message in a chat.
 *
 * This use case encapsulates the business logic for reacting to messages,
 * delegating the actual operation to the [MessageRepository].
 *
 * @property messageRepository The repository responsible for message operations.
 */
class AddReactionUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Adds a reaction to the specified message.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message to react to.
     * @param reaction The string representation of the reaction (e.g., an emoji or a short text code).
     */
    suspend operator fun invoke(chatId: String, messageId: String, reaction: String) {
        messageRepository.addReaction(chatId, messageId, reaction)
    }
}
