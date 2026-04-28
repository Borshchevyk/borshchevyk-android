package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateChatInfoParam
import javax.inject.Inject

class UpdateChatInfoUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, title: String?, description: String?) {
        chatRepository.updateChatInfo(chatId, DomainUpdateChatInfoParam(title, description))
    }
}
