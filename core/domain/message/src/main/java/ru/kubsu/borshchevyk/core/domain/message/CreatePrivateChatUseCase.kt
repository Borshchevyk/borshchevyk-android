package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import javax.inject.Inject

class CreatePrivateChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(targetUserId: String): Chat {
        return chatRepository.createChat(
            CreateChatRequest(
                type = ChatType.PRIVATE,
                initialMemberIds = listOf(targetUserId)
            )
        )
    }
}
