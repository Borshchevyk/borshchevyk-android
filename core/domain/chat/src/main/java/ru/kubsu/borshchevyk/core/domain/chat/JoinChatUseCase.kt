package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.Chat
import javax.inject.Inject

/**
 * Use case for joining a chat using an invitation link or code.
 *
 * This allows a user to become a participant in a chat (usually a group chat)
 * without needing an explicit invite from a current member, provided they have
 * a valid invitation string.
 *
 * @property chatRepository The repository handling the join operation.
 */
class JoinChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to join the chat.
     *
     * @param inviteCode The invitation link or code required to join the chat.
     * @return The [Chat] object representing the chat that was successfully joined.
     */
    suspend operator fun invoke(inviteCode: String): Chat {
        return chatRepository.joinChatByLink(inviteCode)
    }
}
