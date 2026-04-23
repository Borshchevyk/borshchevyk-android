package ru.kubsu.borshchevyk.core.network.chat

import ru.kubsu.borshchevyk.core.model.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.model.dto.ChatResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.model.dto.PageResponse
import ru.kubsu.borshchevyk.core.model.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest

interface ChatNetworkDataSource {
    suspend fun createChat(request: CreateChatRequest): ChatResponse
    suspend fun createPrivateChat(request: TargetUserRequest): ChatResponse
    suspend fun getUserChats(): List<ChatResponse>
    suspend fun updatePermissions(chatId: String, targetUserId: String, request: UpdatePermissionsRequest)
    suspend fun updateChatInfo(chatId: String, request: UpdateChatInfoRequest)
    suspend fun clearChatHistory(chatId: String, forAll: Boolean)
    suspend fun deleteChat(chatId: String)
    
    suspend fun getChatMembers(chatId: String, page: Int, size: Int): PageResponse<ChatMemberResponse>
    suspend fun inviteUser(chatId: String, request: TargetUserRequest)
    suspend fun kickUser(chatId: String, targetUserId: String)
    suspend fun leaveChat(chatId: String)
    suspend fun generateInviteLink(chatId: String): String
    suspend fun joinChatByLink(inviteCode: String): ChatResponse
}
