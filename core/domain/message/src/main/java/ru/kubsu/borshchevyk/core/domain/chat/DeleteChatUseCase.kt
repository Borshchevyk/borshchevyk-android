package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

class DeleteChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String) {
        chatRepository.deleteChat(chatId)
    }
}
