package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.DomainCreateChatParam
import javax.inject.Inject

/**
 * Use case for creating a new group chat.
 *
 * Group chats allow multiple users to communicate together. This use case encapsulates
 * the creation logic, constructing the necessary domain parameters and forwarding
 * the request to the underlying repository.
 *
 * @property chatRepository The repository handling the creation of the chat.
 */
class CreateGroupChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to create a group chat.
     *
     * @param title The name/title of the new group chat.
     * @param description An optional description explaining the purpose of the group.
     * @param memberIds An optional list of user IDs to immediately add as participants.
     * @return The unique identifier (UUID) of the newly created group chat.
     */
    suspend operator fun invoke(title: String, description: String? = null, memberIds: List<String> = emptyList()): String {
        return chatRepository.createChat(
            DomainCreateChatParam(
                type = ChatType.GROUP,
                title = title,
                description = description,
                initialMemberIds = memberIds
            )
        )
    }
}
