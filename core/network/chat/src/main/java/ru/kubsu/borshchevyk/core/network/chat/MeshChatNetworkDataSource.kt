package ru.kubsu.borshchevyk.core.network.chat

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.ChatDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.ChatEntity
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.network.dto.ChatResponse
import ru.kubsu.borshchevyk.core.network.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.network.dto.GlobalSearchResponse
import ru.kubsu.borshchevyk.core.network.dto.PageResponse
import ru.kubsu.borshchevyk.core.network.dto.ShortUserDto
import ru.kubsu.borshchevyk.core.network.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePermissionsRequest
import java.util.UUID
import javax.inject.Inject

class MeshChatNetworkDataSource @Inject constructor(
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ChatNetworkDataSource {

    override suspend fun createChat(request: CreateChatRequest): NetworkResult<ChatResponse> {
        return NetworkResult.Error(501, "Not implemented in Mesh mode yet")
    }

    override suspend fun createPrivateChat(request: TargetUserRequest): NetworkResult<ChatResponse> {
        return withContext(ioDispatcher) {
            val existingChat = chatDao.getChatByPartnerId(request.targetUserId)
            if (existingChat != null) {
                return@withContext NetworkResult.Success(
                    ChatResponse(
                        id = existingChat.id,
                        type = existingChat.type,
                        createdAt = existingChat.createdAt,
                        partnerId = existingChat.partnerId,
                        partnerName = existingChat.partnerName,
                        partnerAvatarUrl = existingChat.partnerAvatarUrl,
                        isDeletable = existingChat.isDeletable,
                        isPinned = existingChat.isPinned
                    )
                )
            }

            val user = userDao.getUser(request.targetUserId)
            val chatResponse = ChatResponse(
                id = UUID.randomUUID().toString(),
                type = ChatType.PRIVATE,
                createdAt = System.currentTimeMillis().toString(),
                partnerId = request.targetUserId,
                partnerName = user?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim() } ?: "Unknown",
                partnerAvatarUrl = user?.avatarUrl,
                isDeletable = true,
                isPinned = false
            )
            
            // Save it locally so the UI doesn't crash when opening the chat
            chatDao.upsertChat(
                ChatEntity(
                    id = chatResponse.id,
                    type = chatResponse.type,
                    title = chatResponse.title,
                    description = chatResponse.description,
                    partnerId = chatResponse.partnerId,
                    partnerName = chatResponse.partnerName,
                    partnerAvatarUrl = chatResponse.partnerAvatarUrl,
                    partnerLastOnline = chatResponse.partnerLastOnline,
                    lastMessage = chatResponse.lastMessage,
                    unreadCount = chatResponse.unreadCount,
                    allowedReactions = chatResponse.allowedReactions,
                    isDeletable = chatResponse.isDeletable,
                    isPinned = chatResponse.isPinned,
                    createdAt = chatResponse.createdAt
                )
            )

            NetworkResult.Success(chatResponse)
        }
    }

    override suspend fun getUserChats(): NetworkResult<List<ChatResponse>> {
        return withContext(ioDispatcher) {
            val localChats = chatDao.observeAllChats().firstOrNull() ?: emptyList()
            val responses = localChats.map { entity ->
                ChatResponse(
                    id = entity.id,
                    type = entity.type,
                    title = entity.title,
                    description = entity.description,
                    createdAt = entity.createdAt,
                    partnerId = entity.partnerId,
                    partnerName = entity.partnerName,
                    partnerAvatarUrl = entity.partnerAvatarUrl,
                    partnerLastOnline = entity.partnerLastOnline,
                    lastMessage = entity.lastMessage,
                    unreadCount = entity.unreadCount,
                    allowedReactions = entity.allowedReactions,
                    isDeletable = entity.isDeletable,
                    isPinned = entity.isPinned
                )
            }
            NetworkResult.Success(responses)
        }
    }

    override suspend fun updatePermissions(
        chatId: String,
        targetUserId: String,
        request: UpdatePermissionsRequest
    ): NetworkResult<Unit> {
         return NetworkResult.Success(Unit)
    }

    override suspend fun updateChatInfo(
        chatId: String,
        request: UpdateChatInfoRequest
    ): NetworkResult<Unit> {
         return NetworkResult.Success(Unit)
    }

    override suspend fun clearChatHistory(chatId: String, forAll: Boolean): NetworkResult<Unit> {
         return NetworkResult.Success(Unit)
    }

    override suspend fun deleteChat(chatId: String): NetworkResult<Unit> {
         return NetworkResult.Success(Unit)
    }

    override suspend fun getChatMembers(
        chatId: String,
        page: Int,
        size: Int
    ): NetworkResult<PageResponse<ChatMemberResponse>> {
        return NetworkResult.Error(501, "Not implemented in Mesh mode yet")
    }

    override suspend fun inviteUser(chatId: String, request: TargetUserRequest): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun kickUser(chatId: String, targetUserId: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun leaveChat(chatId: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun generateInviteLink(chatId: String): NetworkResult<String> {
        return NetworkResult.Error(501, "Not implemented in Mesh mode yet")
    }

    override suspend fun joinChatByLink(inviteCode: String): NetworkResult<ChatResponse> {
        return NetworkResult.Error(501, "Not implemented in Mesh mode yet")
    }

    override suspend fun pinChat(chatId: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun unpinChat(chatId: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun globalSearch(query: String): NetworkResult<GlobalSearchResponse> {
        return withContext(ioDispatcher) {
            val users = userDao.searchUsers(query).map { entity ->
                ShortUserDto(
                    id = entity.userId,
                    firstName = entity.firstName,
                    lastName = entity.lastName,
                    tag = entity.tag,
                    avatarUrl = entity.avatarUrl
                )
            }
            NetworkResult.Success(GlobalSearchResponse(users = users, chats = emptyList()))
        }
    }
}
