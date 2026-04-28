package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

class SyncUserChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke() {
        chatRepository.syncUserChats()
    }
}
