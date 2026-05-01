package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for pinning a specific message in a chat.
 *
 * Pinned messages are typically highlighted or easily accessible to all
 * participants in the chat. This use case delegates the pinning action
 * to the underlying [MessageRepository].
 *
 * @property messageRepository The repository responsible for message operations.
 */
class PinMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Pins the specified message.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message to be pinned.
     */
    suspend operator fun invoke(chatId: String, messageId: String) {
        messageRepository.pinMessage(chatId, messageId)
    }
}
