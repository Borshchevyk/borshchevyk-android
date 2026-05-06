package ru.kubsu.borshchevyk.core.network.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.PageResponse
import ru.kubsu.borshchevyk.core.network.dto.ShortUserDto
import ru.kubsu.borshchevyk.core.network.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePermissionsRequest
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope
import ru.kubsu.borshchevyk.core.network.mesh.MeshFloodingProtocol
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import java.util.UUID
import javax.inject.Inject

class MeshChatNetworkDataSource @Inject constructor(
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val gossipProtocol: MeshFloodingProtocol,
    private val signatureService: MeshSignatureService,
    private val json: Json
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
            val partnerName = user?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim().takeIf { name -> name.isNotEmpty() } ?: it.tag } ?: "Unknown"
            val chatResponse = ChatResponse(
                id = UUID.randomUUID().toString(),
                type = ChatType.PRIVATE,
                createdAt = System.currentTimeMillis().toString(),
                partnerId = request.targetUserId,
                partnerName = partnerName,
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

            // Broadcast the chat creation to the remote peer, but invert the partner fields
            // so the remote peer receives our name/avatar as their partner, avoiding 'New Mesh Chat'.
            val localUserId = signatureService.getUserId() ?: "self"
            val localUser = userDao.getUser(localUserId)
            val localName = localUser?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim().takeIf { name -> name.isNotEmpty() } ?: it.tag } ?: "Unknown"

            val broadcastChatResponse = chatResponse.copy(
                partnerId = localUserId,
                partnerName = localName,
                partnerAvatarUrl = localUser?.avatarUrl
            )

            val event = NotificationDto.ChatEventDto(broadcastChatResponse, "UPDATE_CHAT")
            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "", // Filled by GossipProtocol
                action = "UPDATE_CHAT",
                payload = json.encodeToString(event)
            )
            gossipProtocol.broadcast(envelope)

            NetworkResult.Success(chatResponse)
        }
    }

    override suspend fun getUserChats(): NetworkResult<List<ChatResponse>> {
        return withContext(ioDispatcher) {
            val localChats = chatDao.observeAllChats().firstOrNull() ?: emptyList()
            val responses = localChats.map { entity ->
                var resolvedPartnerName = entity.partnerName
                var resolvedAvatar = entity.partnerAvatarUrl
                
                val currentPartnerId = entity.partnerId
                if (entity.type == ChatType.PRIVATE && currentPartnerId != null) {
                    val user = userDao.getUser(currentPartnerId)
                    if (user != null) {
                        resolvedPartnerName = "${user.firstName.orEmpty()} ${user.lastName.orEmpty()}".trim().takeIf { it.isNotEmpty() } ?: user.tag
                        resolvedAvatar = user.avatarUrl
                        
                        // Self-heal the database
                        if (resolvedPartnerName != entity.partnerName || resolvedAvatar != entity.partnerAvatarUrl) {
                            chatDao.upsertChat(entity.copy(partnerName = resolvedPartnerName, partnerAvatarUrl = resolvedAvatar))
                        }
                    }
                }

                ChatResponse(
                    id = entity.id,
                    type = entity.type,
                    title = entity.title,
                    description = entity.description,
                    createdAt = entity.createdAt,
                    partnerId = entity.partnerId,
                    partnerName = resolvedPartnerName,
                    partnerAvatarUrl = resolvedAvatar,
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
        val chat = ChatResponse(id = chatId, type = ChatType.GROUP, createdAt = "", title = request.title, description = request.description)
        val event = NotificationDto.ChatEventDto(chat, "UPDATE_CHAT")
        val envelope = MeshEnvelope(
            envelopeId = UUID.randomUUID().toString(),
            originEndpointId = "self",
            action = "UPDATE_CHAT",
            payload = json.encodeToString(event)
        )
        gossipProtocol.broadcast(envelope)
        return NetworkResult.Success(Unit)
    }

    override suspend fun clearChatHistory(chatId: String, forAll: Boolean): NetworkResult<Unit> {
        val chat = ChatResponse(id = chatId, type = ChatType.GROUP, createdAt = "")
        val event = NotificationDto.ChatEventDto(chat, "CLEAR_HISTORY")
        val envelope = MeshEnvelope(
            envelopeId = UUID.randomUUID().toString(),
            originEndpointId = "self",
            action = "CLEAR_HISTORY",
            payload = json.encodeToString(event)
        )
        gossipProtocol.broadcast(envelope)
        return NetworkResult.Success(Unit)
    }

    override suspend fun deleteChat(chatId: String): NetworkResult<Unit> {
        val chat = ChatResponse(id = chatId, type = ChatType.GROUP, createdAt = "")
        val event = NotificationDto.ChatEventDto(chat, "DELETE_CHAT")
        val envelope = MeshEnvelope(
            envelopeId = UUID.randomUUID().toString(),
            originEndpointId = "self",
            action = "DELETE_CHAT",
            payload = json.encodeToString(event)
        )
        gossipProtocol.broadcast(envelope)
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
        val chat = ChatResponse(id = chatId, type = ChatType.GROUP, createdAt = "", partnerId = request.targetUserId)
        val event = NotificationDto.ChatEventDto(chat, "INVITE_USER")
        val envelope = MeshEnvelope(
            envelopeId = UUID.randomUUID().toString(),
            originEndpointId = "self",
            action = "INVITE_USER",
            payload = json.encodeToString(event)
        )
        gossipProtocol.broadcast(envelope)
        return NetworkResult.Success(Unit)
    }

    override suspend fun kickUser(chatId: String, targetUserId: String): NetworkResult<Unit> {
        val chat = ChatResponse(id = chatId, type = ChatType.GROUP, createdAt = "", partnerId = targetUserId)
        val event = NotificationDto.ChatEventDto(chat, "KICK_USER")
        val envelope = MeshEnvelope(
            envelopeId = UUID.randomUUID().toString(),
            originEndpointId = "self",
            action = "KICK_USER",
            payload = json.encodeToString(event)
        )
        gossipProtocol.broadcast(envelope)
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
