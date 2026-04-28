package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.DomainCreateChatParam
import ru.kubsu.borshchevyk.core.model.domain.DomainPage
import ru.kubsu.borshchevyk.core.model.domain.DomainTargetUserParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateChatInfoParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePermissionsParam
import ru.kubsu.borshchevyk.core.model.domain.GlobalSearchResults

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun createChat(request: DomainCreateChatParam): String
    suspend fun createPrivateChat(request: DomainTargetUserParam): String
    fun observeUserChats(): Flow<List<Chat>>
    suspend fun syncUserChats()
    suspend fun getUserChats(): List<Chat>
    suspend fun updatePermissions(chatId: String, targetUserId: String, request: DomainUpdatePermissionsParam)
    suspend fun updateChatInfo(chatId: String, request: DomainUpdateChatInfoParam)
    suspend fun clearChatHistory(chatId: String, forAll: Boolean)
    suspend fun deleteChat(chatId: String)
    
    suspend fun getChatMembers(chatId: String, page: Int, size: Int): DomainPage<ChatMember>
    suspend fun inviteUser(chatId: String, request: DomainTargetUserParam)
    suspend fun kickUser(chatId: String, targetUserId: String)
    suspend fun leaveChat(chatId: String)
    suspend fun generateInviteLink(chatId: String): String
    suspend fun joinChatByLink(inviteCode: String): Chat
    suspend fun pinChat(chatId: String)
    suspend fun unpinChat(chatId: String)
    suspend fun globalSearch(query: String): GlobalSearchResults
}
