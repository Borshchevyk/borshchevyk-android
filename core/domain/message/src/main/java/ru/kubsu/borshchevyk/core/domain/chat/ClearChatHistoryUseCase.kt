package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

class ClearChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, forAll: Boolean = false) {
        chatRepository.clearChatHistory(chatId, forAll)
    }
}
