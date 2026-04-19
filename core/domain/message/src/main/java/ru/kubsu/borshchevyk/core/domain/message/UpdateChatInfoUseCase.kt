package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.dto.UpdateChatInfoRequest
import javax.inject.Inject

class UpdateChatInfoUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, request: UpdateChatInfoRequest) {
        chatRepository.updateChatInfo(chatId, request)
    }
}
