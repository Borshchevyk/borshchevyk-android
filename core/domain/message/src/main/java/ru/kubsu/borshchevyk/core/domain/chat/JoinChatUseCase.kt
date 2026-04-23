package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.Chat
import javax.inject.Inject

class JoinChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(inviteCode: String): Chat {
        return chatRepository.joinChatByLink(inviteCode)
    }
}
