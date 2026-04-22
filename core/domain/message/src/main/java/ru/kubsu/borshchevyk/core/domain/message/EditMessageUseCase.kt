package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

class EditMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, messageId: String, newText: String): Message {
        return messageRepository.editMessage(chatId, messageId, newText)
    }
}
