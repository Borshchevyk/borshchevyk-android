package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import javax.inject.Inject

class CreateGroupChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(request: CreateChatRequest): Chat {
        return chatRepository.createChat(request)
    }
}
