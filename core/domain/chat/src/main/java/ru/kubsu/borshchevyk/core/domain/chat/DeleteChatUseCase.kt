package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

/**
 * Use case for deleting an existing chat.
 *
 * This action completely removes the chat, including its history and membership,
 * depending on the user's role and permissions within that chat. For private chats,
 * it may clear the chat for the user, while for groups, it might delete the group
 * entirely if the user is the owner.
 *
 * @property chatRepository The repository handling the deletion logic.
 */
class DeleteChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to delete the chat.
     *
     * @param chatId The unique identifier of the chat to be deleted.
     */
    suspend operator fun invoke(chatId: String) {
        chatRepository.deleteChat(chatId)
    }
}
