package ru.kubsu.borshchevyk.feature.chat.handlers

import ru.kubsu.borshchevyk.core.domain.chat.ClearChatHistoryUseCase
import ru.kubsu.borshchevyk.core.domain.chat.DeleteChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GenerateInviteLinkUseCase
import ru.kubsu.borshchevyk.core.domain.chat.InviteUserUseCase
import ru.kubsu.borshchevyk.core.domain.chat.KickUserUseCase
import ru.kubsu.borshchevyk.core.domain.chat.LeaveChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.UpdateChatInfoUseCase
import ru.kubsu.borshchevyk.core.domain.chat.UpdateMemberPermissionsUseCase
import javax.inject.Inject

/**
 * Handles chat settings actions such as managing members, invite links, chat information, and history.
 *
 * @property inviteUserUseCase Use case to invite a user to the chat.
 * @property generateInviteLinkUseCase Use case to generate a shareable invite link for the chat.
 * @property updateMemberPermissionsUseCase Use case to modify a specific member's permissions.
 * @property clearChatHistoryUseCase Use case to clear the chat's message history.
 * @property deleteChatUseCase Use case to permanently delete the chat.
 * @property kickUserUseCase Use case to remove a user from the chat.
 * @property leaveChatUseCase Use case for the current user to leave the chat.
 * @property updateChatInfoUseCase Use case to update the chat's title and description.
 */
class ChatSettingsHandler @Inject constructor(
    private val inviteUserUseCase: InviteUserUseCase,
    private val generateInviteLinkUseCase: GenerateInviteLinkUseCase,
    private val updateMemberPermissionsUseCase: UpdateMemberPermissionsUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val deleteChatUseCase: DeleteChatUseCase,
    private val kickUserUseCase: KickUserUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val updateChatInfoUseCase: UpdateChatInfoUseCase
) {
    /**
     * Invites a specified user to join the chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param userId The unique identifier of the user to invite.
     */
    suspend fun inviteUser(chatId: String, userId: String) = inviteUserUseCase(chatId, userId)
    
    /**
     * Generates a new invite link for the chat.
     *
     * @param chatId The unique identifier of the chat.
     * @return The generated invite link as a string.
     */
    suspend fun generateInviteLink(chatId: String) = generateInviteLinkUseCase(chatId)
    
    /**
     * Updates the permissions for a specific member in the chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param targetUserId The unique identifier of the user whose permissions are being updated.
     * @param canSendMessages Whether the user is allowed to send messages.
     * @param canDeleteMessages Whether the user is allowed to delete messages.
     * @param canInviteUsers Whether the user is allowed to invite others to the chat.
     * @param canChangeInfo Whether the user is allowed to change chat information (title/description).
     */
    suspend fun updatePermissions(
        chatId: String,
        targetUserId: String,
        canSendMessages: Boolean,
        canDeleteMessages: Boolean,
        canInviteUsers: Boolean,
        canChangeInfo: Boolean
    ) = updateMemberPermissionsUseCase(chatId, targetUserId, canSendMessages, canDeleteMessages, canInviteUsers, canChangeInfo)
    
    /**
     * Clears the message history of the chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param forAll If `true`, the history will be cleared for all members of the chat.
     */
    suspend fun clearHistory(chatId: String, forAll: Boolean) = clearChatHistoryUseCase(chatId, forAll)
    
    /**
     * Deletes the chat entirely.
     *
     * @param chatId The unique identifier of the chat to delete.
     */
    suspend fun deleteChat(chatId: String) = deleteChatUseCase(chatId)
    
    /**
     * Kicks (removes) a specific user from the chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param targetUserId The unique identifier of the user to remove.
     */
    suspend fun kickUser(chatId: String, targetUserId: String) = kickUserUseCase(chatId, targetUserId)
    
    /**
     * Leaves the specified chat.
     *
     * @param chatId The unique identifier of the chat to leave.
     */
    suspend fun leaveChat(chatId: String) = leaveChatUseCase(chatId)
    
    /**
     * Updates the title and description of the chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param title The new title for the chat, or `null` to leave unchanged.
     * @param description The new description for the chat, or `null` to leave unchanged.
     */
    suspend fun updateChatInfo(chatId: String, title: String?, description: String?) = updateChatInfoUseCase(chatId, title, description)
}
