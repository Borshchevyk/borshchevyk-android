package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

/**
 * Use case for clearing the message history of a specific chat.
 *
 * This operation removes the stored messages within the chat. Depending on the `forAll` flag
 * and the user's permissions, this may clear the history only locally for the current user,
 * or globally for all participants in the chat.
 *
 * @property chatRepository The repository responsible for executing the chat history clearing logic.
 */
class ClearChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to clear the chat's history.
     *
     * @param chatId The unique identifier of the chat whose history should be cleared.
     * @param forAll If `true`, requests to clear the history for all members of the chat. 
     *               If `false` (default), clears the history only for the current user.
     */
    suspend operator fun invoke(chatId: String, forAll: Boolean = false) {
        chatRepository.clearChatHistory(chatId, forAll)
    }
}
