package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for deleting a message from a chat.
 *
 * This use case delegates the deletion operation to the [MessageRepository].
 * It supports both local deletion (for the current user only) and global deletion
 * (for all participants in the chat), if permitted.
 *
 * @property messageRepository The repository responsible for message operations.
 */
class DeleteMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Deletes the specified message.
     *
     * @param chatId The unique identifier of the chat where the message exists.
     * @param messageId The unique identifier of the message to be deleted.
     * @param forAll If true, attempts to delete the message for all participants in the chat. Defaults to false.
     */
    suspend operator fun invoke(chatId: String, messageId: String, forAll: Boolean = false) {
        messageRepository.deleteMessage(chatId, messageId, forAll)
    }
}
