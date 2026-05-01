package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.DomainTargetUserParam
import javax.inject.Inject

/**
 * Use case for initiating a direct, private chat with another user.
 *
 * Private chats are one-on-one conversations. This use case forwards the request
 * with the target user's ID to the repository to establish the connection or
 * retrieve an existing private chat if one already exists.
 *
 * @property chatRepository The repository handling the private chat creation.
 */
class CreatePrivateChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to create a private chat.
     *
     * @param targetUserId The unique identifier of the user to start a chat with.
     * @return The unique identifier (UUID) of the created or existing private chat.
     */
    suspend operator fun invoke(targetUserId: String): String {
        return chatRepository.createPrivateChat(DomainTargetUserParam(targetUserId))
    }
}
