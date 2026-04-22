package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, text: String, attachmentIds: List<String>? = null): Message {
        return messageRepository.sendMessage(
            chatId, 
            SendMessageRequest(
                text = text,
                attachmentIds = attachmentIds
            )
        )
    }
}
