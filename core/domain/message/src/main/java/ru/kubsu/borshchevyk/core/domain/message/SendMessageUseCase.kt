package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

/**
 * Use case for sending a new message to a chat.
 *
 * This use case encapsulates the business logic for creating and sending
 * a message, potentially including text, attachments, or forwarding data.
 * It delegates the actual network or local persistence operation to the
 * [MessageRepository].
 *
 * @property messageRepository The repository responsible for message operations.
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Sends a message to the specified chat.
     *
     * @param chatId The unique identifier of the destination chat.
     * @param text The text content of the message.
     * @param attachmentIds An optional list of IDs representing previously uploaded attachments.
     * @param forwardedFromChatId If this is a forwarded message, the ID of the original chat.
     * @param forwardedFromUserId If this is a forwarded message, the ID of the original sender.
     */
    suspend operator fun invoke(
        chatId: String, 
        text: String, 
        attachmentIds: List<String>? = null,
        forwardedFromChatId: String? = null,
        forwardedFromUserId: String? = null
    ) {
        messageRepository.sendMessage(
            chatId = chatId,
            text = text,
            attachmentIds = attachmentIds,
            forwardedFromChatId = forwardedFromChatId,
            forwardedFromUserId = forwardedFromUserId
        )
    }
}
