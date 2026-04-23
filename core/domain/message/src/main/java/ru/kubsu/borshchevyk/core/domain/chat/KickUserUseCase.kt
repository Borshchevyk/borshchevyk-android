package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

class KickUserUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, targetUserId: String) {
        chatRepository.kickUser(chatId, targetUserId)
    }
}
