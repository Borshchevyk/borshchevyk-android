package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.model.dto.PageResponse
import ru.kubsu.borshchevyk.core.model.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest

interface ChatRepository {
    suspend fun createChat(request: CreateChatRequest): Chat
    suspend fun createPrivateChat(request: TargetUserRequest): Chat
    suspend fun getUserChats(): List<Chat>
    suspend fun updatePermissions(chatId: String, targetUserId: String, request: UpdatePermissionsRequest)
    suspend fun clearChatHistory(chatId: String, forAll: Boolean)
    suspend fun deleteChat(chatId: String)
    
    suspend fun getChatMembers(chatId: String, page: Int, size: Int): PageResponse<ChatMember>
    suspend fun inviteUser(chatId: String, request: TargetUserRequest)
    suspend fun generateInviteLink(chatId: String): String
    suspend fun joinChatByLink(inviteCode: String): Chat
}
