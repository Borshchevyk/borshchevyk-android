package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

/**
 * Use case for unpinning a previously pinned chat.
 *
 * Unpinning a chat removes its forced position at the top of the chat list,
 * allowing it to be sorted normally (usually by the timestamp of the last message).
 *
 * @property chatRepository The repository handling the unpinning operation.
 */
class UnpinChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to unpin the chat.
     *
     * @param chatId The unique identifier of the chat to unpin.
     */
    suspend operator fun invoke(chatId: String) {
        chatRepository.unpinChat(chatId)
    }
}
