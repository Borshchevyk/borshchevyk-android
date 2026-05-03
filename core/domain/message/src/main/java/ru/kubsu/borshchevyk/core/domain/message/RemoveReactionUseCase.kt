package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for removing an existing reaction from a message.
 *
 * This use case allows a user to retract a reaction they previously added
 * to a specific message within a chat.
 *
 * @property messageRepository The repository responsible for message operations.
 */
class RemoveReactionUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Removes a reaction from the specified message.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message from which to remove the reaction.
     * @param reaction The string representation of the reaction to be removed.
     */
    suspend operator fun invoke(chatId: String, messageId: String, reaction: String) {
        messageRepository.removeReaction(chatId, messageId, reaction)
    }
}
