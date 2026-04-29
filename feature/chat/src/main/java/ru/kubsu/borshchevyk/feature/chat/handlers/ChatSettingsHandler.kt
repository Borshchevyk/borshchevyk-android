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
 * Handles chat settings actions like member management, invite links, and chat info updates.
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
    suspend fun inviteUser(chatId: String, userId: String) = inviteUserUseCase(chatId, userId)
    
    suspend fun generateInviteLink(chatId: String) = generateInviteLinkUseCase(chatId)
    
    suspend fun updatePermissions(
        chatId: String,
        targetUserId: String,
        canSendMessages: Boolean,
        canDeleteMessages: Boolean,
        canInviteUsers: Boolean,
        canChangeInfo: Boolean
    ) = updateMemberPermissionsUseCase(chatId, targetUserId, canSendMessages, canDeleteMessages, canInviteUsers, canChangeInfo)
    
    suspend fun clearHistory(chatId: String, forAll: Boolean) = clearChatHistoryUseCase(chatId, forAll)
    
    suspend fun deleteChat(chatId: String) = deleteChatUseCase(chatId)
    
    suspend fun kickUser(chatId: String, targetUserId: String) = kickUserUseCase(chatId, targetUserId)
    
    suspend fun leaveChat(chatId: String) = leaveChatUseCase(chatId)
    
    suspend fun updateChatInfo(chatId: String, title: String?, description: String?) = updateChatInfoUseCase(chatId, title, description)
}
