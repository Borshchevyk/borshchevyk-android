package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

class GetMessageReadersUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, messageId: String): List<User> {
        return messageRepository.getMessageReaders(chatId, messageId)
    }
}
