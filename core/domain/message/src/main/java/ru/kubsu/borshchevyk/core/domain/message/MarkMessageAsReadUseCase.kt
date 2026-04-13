package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class MarkMessageAsReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, messageId: String) {
        messageRepository.readMessage(chatId, messageId)
    }
}
