package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for explicitly synchronizing the message history of a chat.
 *
 * This use case triggers a network request to fetch historical messages for a
 * specific chat and updates the local cache. It is typically used for pagination
 * (loading older messages) or when initially entering a chat screen.
 *
 * @property messageRepository The repository responsible for history synchronization.
 */
class SyncChatHistoryUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Synchronizes the chat history for the specified chat.
     *
     * @param chatId The unique identifier of the chat to sync.
     * @param page The zero-based page index to retrieve. Defaults to 0.
     * @param size The number of messages to fetch per page. Defaults to 50.
     */
    suspend operator fun invoke(chatId: String, page: Int = 0, size: Int = 50) {
        messageRepository.syncChatHistory(chatId, page, size)
    }
}
