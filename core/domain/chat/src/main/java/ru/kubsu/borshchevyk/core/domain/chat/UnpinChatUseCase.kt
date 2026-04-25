package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

class UnpinChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String) {
        chatRepository.unpinChat(chatId)
    }
}
