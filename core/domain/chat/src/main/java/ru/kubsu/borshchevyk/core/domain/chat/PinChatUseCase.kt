package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

/**
 * Use case for pinning a chat to the top of the user's chat list.
 *
 * Pinned chats remain at the top of the list regardless of recent activity,
 * allowing users to keep important conversations easily accessible.
 *
 * @property chatRepository The repository handling the pinning operation.
 */
class PinChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to pin the chat.
     *
     * @param chatId The unique identifier of the chat to pin.
     */
    suspend operator fun invoke(chatId: String) {
        chatRepository.pinChat(chatId)
    }
}
