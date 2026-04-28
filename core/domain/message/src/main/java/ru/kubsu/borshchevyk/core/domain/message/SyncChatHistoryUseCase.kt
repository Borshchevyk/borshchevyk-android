package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class SyncChatHistoryUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, page: Int = 0, size: Int = 50) {
        messageRepository.syncChatHistory(chatId, page, size)
    }
}
