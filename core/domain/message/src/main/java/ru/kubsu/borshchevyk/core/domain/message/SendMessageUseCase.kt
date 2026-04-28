package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        chatId: String, 
        text: String, 
        attachmentIds: List<String>? = null,
        forwardedFromChatId: String? = null,
        forwardedFromUserId: String? = null
    ): Message {
        return messageRepository.sendMessage(
            chatId = chatId,
            text = text,
            attachmentIds = attachmentIds,
            forwardedFromChatId = forwardedFromChatId,
            forwardedFromUserId = forwardedFromUserId
        )
    }
}
