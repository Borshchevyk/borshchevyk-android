package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.DomainTargetUserParam
import javax.inject.Inject

/**
 * Use case for inviting a specific user to join an existing chat.
 *
 * This is primarily used for adding new participants to a group chat. The inviter
 * typically needs the appropriate permissions within the chat to perform this action.
 *
 * @property chatRepository The repository handling the invitation process.
 */
class InviteUserUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to invite a user to a chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param targetUserId The unique identifier of the user being invited.
     */
    suspend operator fun invoke(chatId: String, targetUserId: String) {
        chatRepository.inviteUser(chatId, DomainTargetUserParam(targetUserId))
    }
}
