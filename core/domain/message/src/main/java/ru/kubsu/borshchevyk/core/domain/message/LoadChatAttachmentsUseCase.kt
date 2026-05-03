package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

/**
 * Use case for loading chat attachments of a specific type.
 */
class LoadChatAttachmentsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Invokes the use case to fetch a paginated list of messages containing attachments.
     *
     * @param chatId The ID of the chat.
     * @param type The attachment type category (e.g., PHOTO, VIDEO, FILE, VOICE, CIRCLE).
     * @param page The pagination page index.
     * @param size The number of messages per page.
     * @return A list of [Message] objects.
     */
    suspend operator fun invoke(chatId: String, type: String, page: Int = 0, size: Int = 50): List<Message> {
        return messageRepository.loadChatAttachments(chatId, type, page, size)
    }
}
