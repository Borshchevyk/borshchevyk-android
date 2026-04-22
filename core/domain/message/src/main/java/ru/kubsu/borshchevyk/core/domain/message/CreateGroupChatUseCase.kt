package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import javax.inject.Inject

class CreateGroupChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(title: String, description: String? = null, memberIds: List<String> = emptyList()): Chat {
        return chatRepository.createChat(
            CreateChatRequest(
                type = ChatType.GROUP,
                title = title,
                description = description,
                initialMemberIds = memberIds
            )
        )
    }
}
