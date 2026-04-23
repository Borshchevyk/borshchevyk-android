package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest
import javax.inject.Inject

class UpdateMemberPermissionsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, targetUserId: String, request: UpdatePermissionsRequest) {
        chatRepository.updatePermissions(chatId, targetUserId, request)
    }
}
