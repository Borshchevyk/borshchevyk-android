package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

class PinChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String) {
        chatRepository.pinChat(chatId)
    }
}
