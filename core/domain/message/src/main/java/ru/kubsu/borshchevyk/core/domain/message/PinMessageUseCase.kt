package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class PinMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, messageId: String) {
        messageRepository.pinMessage(chatId, messageId)
    }
}
