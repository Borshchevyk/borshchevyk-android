package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.dto.TargetUserRequest
import javax.inject.Inject

class CreatePrivateChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(targetUserId: String): Chat {
        return chatRepository.createPrivateChat(TargetUserRequest(targetUserId = targetUserId))
    }
}
