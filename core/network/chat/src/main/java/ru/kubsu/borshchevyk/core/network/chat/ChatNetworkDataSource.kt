package ru.kubsu.borshchevyk.core.network.chat

import ru.kubsu.borshchevyk.core.model.domain.NetworkResult
import ru.kubsu.borshchevyk.core.model.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.model.dto.ChatResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.model.dto.GlobalSearchResponse
import ru.kubsu.borshchevyk.core.model.dto.PageResponse
import ru.kubsu.borshchevyk.core.model.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest

interface ChatNetworkDataSource {
    suspend fun createChat(request: CreateChatRequest): NetworkResult<ChatResponse>
    suspend fun createPrivateChat(request: TargetUserRequest): NetworkResult<ChatResponse>
    suspend fun getUserChats(): NetworkResult<List<ChatResponse>>
    suspend fun updatePermissions(chatId: String, targetUserId: String, request: UpdatePermissionsRequest): NetworkResult<Unit>
    suspend fun updateChatInfo(chatId: String, request: UpdateChatInfoRequest): NetworkResult<Unit>
    suspend fun clearChatHistory(chatId: String, forAll: Boolean): NetworkResult<Unit>
    suspend fun deleteChat(chatId: String): NetworkResult<Unit>
    
    suspend fun getChatMembers(chatId: String, page: Int, size: Int): NetworkResult<PageResponse<ChatMemberResponse>>
    suspend fun inviteUser(chatId: String, request: TargetUserRequest): NetworkResult<Unit>
    suspend fun kickUser(chatId: String, targetUserId: String): NetworkResult<Unit>
    suspend fun leaveChat(chatId: String): NetworkResult<Unit>
    suspend fun generateInviteLink(chatId: String): NetworkResult<String>
    suspend fun joinChatByLink(inviteCode: String): NetworkResult<ChatResponse>
    suspend fun pinChat(chatId: String): NetworkResult<Unit>
    suspend fun unpinChat(chatId: String): NetworkResult<Unit>
    suspend fun globalSearch(query: String): NetworkResult<GlobalSearchResponse>
}
