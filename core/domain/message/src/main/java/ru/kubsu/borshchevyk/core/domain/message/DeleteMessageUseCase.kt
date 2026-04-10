package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class DeleteMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, messageId: String, forAll: Boolean = false) {
        messageRepository.deleteMessage(chatId, messageId, forAll)
    }
}
