package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

/**
 * Use case for removing (kicking) a specific user from a chat.
 *
 * This action typically requires the current user to have administrative or
 * owner privileges within the group chat. Once kicked, the target user will
 * no longer have access to the chat or its messages.
 *
 * @property chatRepository The repository handling the removal process.
 */
class KickUserUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to kick a user from the chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param targetUserId The unique identifier of the user to be removed.
     */
    suspend operator fun invoke(chatId: String, targetUserId: String) {
        chatRepository.kickUser(chatId, targetUserId)
    }
}
