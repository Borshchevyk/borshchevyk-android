package ru.kubsu.borshchevyk.feature.chat.settings.interactor

import ru.kubsu.borshchevyk.core.domain.chat.ClearChatHistoryUseCase
import ru.kubsu.borshchevyk.core.domain.chat.DeleteChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GenerateInviteLinkUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetChatMembersUseCase
import ru.kubsu.borshchevyk.core.domain.chat.InviteUserUseCase
import ru.kubsu.borshchevyk.core.domain.chat.KickUserUseCase
import ru.kubsu.borshchevyk.core.domain.chat.LeaveChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.UpdateChatInfoUseCase
import ru.kubsu.borshchevyk.core.domain.chat.UpdateMemberPermissionsUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import javax.inject.Inject

class ChatSettingsHandler @Inject constructor(
    private val inviteUserUseCase: InviteUserUseCase,
    private val generateInviteLinkUseCase: GenerateInviteLinkUseCase,
    private val updateMemberPermissionsUseCase: UpdateMemberPermissionsUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val deleteChatUseCase: DeleteChatUseCase,
    private val kickUserUseCase: KickUserUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val updateChatInfoUseCase: UpdateChatInfoUseCase,
    private val getChatMembersUseCase: GetChatMembersUseCase
) {
    suspend fun inviteUser(chatId: String, userId: String) {
        inviteUserUseCase(chatId, userId)
    }

    suspend fun generateInviteLink(chatId: String): String {
        return generateInviteLinkUseCase(chatId)
    }

    suspend fun updatePermissions(
        chatId: String, targetUserId: String, canSendMessages: Boolean, 
        canDeleteMessages: Boolean, canInviteUsers: Boolean, canChangeInfo: Boolean
    ): List<ChatMember> {
        updateMemberPermissionsUseCase(chatId, targetUserId, canSendMessages, canDeleteMessages, canInviteUsers, canChangeInfo)
        return getChatMembersUseCase(chatId, 0, 100).content
    }

    suspend fun clearHistory(chatId: String, forAll: Boolean) {
        clearChatHistoryUseCase(chatId, forAll)
    }

    suspend fun deleteChat(chatId: String) {
        deleteChatUseCase(chatId)
    }

    suspend fun kickUser(chatId: String, targetUserId: String): List<ChatMember> {
        kickUserUseCase(chatId, targetUserId)
        return getChatMembersUseCase(chatId, 0, 100).content
    }

    suspend fun leaveChat(chatId: String) {
        leaveChatUseCase(chatId)
    }

    suspend fun updateChatInfo(chatId: String, title: String?, description: String?) {
        updateChatInfoUseCase(chatId, title, description)
    }
}
