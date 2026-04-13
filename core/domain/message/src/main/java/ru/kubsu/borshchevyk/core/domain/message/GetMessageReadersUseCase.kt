package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class GetMessageReadersUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, messageId: String): List<String> {
        return messageRepository.getMessageReaders(chatId, messageId)
    }
}
