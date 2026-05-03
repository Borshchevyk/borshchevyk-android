package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePermissionsParam
import javax.inject.Inject

/**
 * Use case for modifying the permissions of a specific member within a chat.
 *
 * This is crucial for group moderation, allowing administrators to restrict or
 * grant capabilities like sending messages, deleting messages, inviting others,
 * or altering chat information. Only users with sufficient administrative rights
 * can successfully execute this action.
 *
 * @property chatRepository The repository handling the permission updates.
 */
class UpdateMemberPermissionsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to update a member's permissions.
     *
     * @param chatId The unique identifier of the chat.
     * @param targetUserId The unique identifier of the user whose permissions are being modified.
     * @param canSendMessages If true/false, explicitly grants or revokes the ability to send messages. If null, leaves it unchanged.
     * @param canDeleteMessages If true/false, explicitly grants or revokes the ability to delete messages. If null, leaves it unchanged.
     * @param canInviteUsers If true/false, explicitly grants or revokes the ability to invite new users. If null, leaves it unchanged.
     * @param canChangeInfo If true/false, explicitly grants or revokes the ability to modify chat metadata. If null, leaves it unchanged.
     */
    suspend operator fun invoke(
        chatId: String,
        targetUserId: String,
        canSendMessages: Boolean? = null,
        canDeleteMessages: Boolean? = null,
        canInviteUsers: Boolean? = null,
        canChangeInfo: Boolean? = null
    ) {
        chatRepository.updatePermissions(
            chatId,
            targetUserId,
            DomainUpdatePermissionsParam(
                userId = targetUserId,
                canSendMessages = canSendMessages,
                canDeleteMessages = canDeleteMessages,
                canInviteUsers = canInviteUsers,
                canChangeInfo = canChangeInfo
            )
        )
    }
}
