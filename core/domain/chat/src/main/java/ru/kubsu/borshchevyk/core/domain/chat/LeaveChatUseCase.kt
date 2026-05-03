package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

/**
 * Use case for the current authenticated user to leave a specific chat.
 *
 * When a user leaves a chat, they are removed from the participant list and
 * will generally lose access to future messages in that chat.
 *
 * @property chatRepository The repository handling the leave operation.
 */
class LeaveChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to leave the chat.
     *
     * @param chatId The unique identifier of the chat to leave.
     */
    suspend operator fun invoke(chatId: String) {
        chatRepository.leaveChat(chatId)
    }
}
