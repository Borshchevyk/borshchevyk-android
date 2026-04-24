package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.dto.TargetUserRequest
import javax.inject.Inject

class InviteUserUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, targetUserId: String) {
        chatRepository.inviteUser(chatId, TargetUserRequest(targetUserId))
    }
}
