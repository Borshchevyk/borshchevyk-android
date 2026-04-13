package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class UnpinMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, messageId: String) {
        messageRepository.unpinMessage(chatId, messageId)
    }
}
