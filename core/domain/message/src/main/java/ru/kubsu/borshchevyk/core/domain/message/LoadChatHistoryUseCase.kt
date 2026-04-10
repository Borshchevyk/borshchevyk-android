package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

class LoadChatHistoryUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, page: Int = 0, size: Int = 50): List<Message> {
        return messageRepository.loadChatHistory(chatId, page, size)
    }
}
