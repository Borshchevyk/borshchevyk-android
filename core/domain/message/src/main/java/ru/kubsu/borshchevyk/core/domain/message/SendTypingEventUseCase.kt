package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class SendTypingEventUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, isTyping: Boolean) {
        messageRepository.sendTypingEvent(chatId, isTyping)
    }
}
