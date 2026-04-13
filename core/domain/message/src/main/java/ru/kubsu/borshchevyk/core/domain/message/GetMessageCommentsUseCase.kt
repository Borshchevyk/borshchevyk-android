package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

class GetMessageCommentsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, messageId: String, page: Int = 0, size: Int = 50): List<Message> {
        return messageRepository.getMessageComments(chatId, messageId, page, size)
    }
}
