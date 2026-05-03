package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

/**
 * Use case for synchronizing the user's local chat data with the remote source.
 *
 * This forces a refresh of the chat list and their metadata from the server or
 * mesh network, ensuring the local cache is up-to-date. Any changes will be
 * reflected in the reactive flow provided by `ObserveUserChatsUseCase`.
 *
 * @property chatRepository The repository handling the synchronization logic.
 */
class SyncUserChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to synchronize the user's chats.
     */
    suspend operator fun invoke() {
        chatRepository.syncUserChats()
    }
}
