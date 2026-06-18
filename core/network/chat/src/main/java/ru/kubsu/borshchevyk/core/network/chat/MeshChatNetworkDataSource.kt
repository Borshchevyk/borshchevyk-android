package ru.kubsu.borshchevyk.core.network.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.database.dao.ChatDao
import ru.kubsu.borshchevyk.core.database.dao.ChatMemberDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.ChatEntity
import ru.kubsu.borshchevyk.core.database.entity.ChatMemberEntity
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.network.dto.ChatResponse
import ru.kubsu.borshchevyk.core.network.dto.CrdtChatUpdateEvent
import ru.kubsu.borshchevyk.core.network.dto.CrdtMemberUpdateEvent
import ru.kubsu.borshchevyk.core.network.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.network.dto.FullStateSyncEvent
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
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class MeshChatNetworkDataSource @Inject constructor(
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val chatMemberDao: ChatMemberDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val gossipProtocol: MeshFloodingProtocol,
    private val signatureService: MeshSignatureService,
    private val json: Json
) : ChatNetworkDataSource {

    private suspend fun encryptPayloadIfNeeded(chatId: String, payloadString: String): String {
        val partnerPubKey = signatureService.getPartnerPublicKeyForChat(chatId) ?: return payloadString
        val e2eePayload = signatureService.encryptE2EE(payloadString.toByteArray(Charsets.UTF_8), partnerPubKey)
        return if (e2eePayload != null) {
            json.encodeToString(e2eePayload)
        } else {
            payloadString
        }
    }

    override suspend fun createChat(request: CreateChatRequest): NetworkResult<ChatResponse> {
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            val groupId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val createdAtIso = Instant.now().toString()

            val chatEntity = ChatEntity(
                id = groupId,
                type = ChatType.GROUP,
                title = request.title ?: "New Group",
                description = request.description,
                partnerId = null,
                partnerName = null,
                partnerAvatarUrl = null,
                partnerLastOnline = null,
                lastMessage = null,
                unreadCount = 0,
                allowedReactions = null,
                isDeletable = true,
                isPinned = false,
                createdAt = createdAtIso,
                titleUpdatedAt = now,
                descriptionUpdatedAt = now
            )
            chatDao.upsertChat(chatEntity)

            val creatorMember = ChatMemberEntity(
                chatId = groupId,
                userId = localUserId,
                role = "OWNER",
                joinedAt = createdAtIso,
                status = "ACTIVE",
                canSendMessages = true,
                canDeleteMessages = true,
                canInviteUsers = true,
                canChangeInfo = true,
                roleUpdatedAt = now,
                statusUpdatedAt = now,
                permissionsUpdatedAt = now
            )
            chatMemberDao.upsertMemberWithLWW(creatorMember)

            val chatUpdate = CrdtChatUpdateEvent(
                chatId = groupId,
                title = chatEntity.title,
                description = chatEntity.description,
                titleUpdatedAt = chatEntity.titleUpdatedAt,
                descriptionUpdatedAt = chatEntity.descriptionUpdatedAt
            )

            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = localUserId,
                action = "CRDT_CHAT_UPDATE",
                payload = json.encodeToString(chatUpdate)
            )
            gossipProtocol.broadcast(envelope)

            NetworkResult.Success(
                ChatResponse(
                    id = chatEntity.id,
                    type = chatEntity.type,
                    title = chatEntity.title,
                    description = chatEntity.description,
                    createdAt = chatEntity.createdAt,
                    isDeletable = chatEntity.isDeletable,
                    isPinned = chatEntity.isPinned
                )
            )
        }
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
            
            val localUserId = signatureService.getUserId() ?: "self"
            val sortedIds = listOf(localUserId, request.targetUserId).sorted()
            val deterministicId = MessageDigest.getInstance("SHA-256")
                .digest(sortedIds.joinToString("_").toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

            val chatResponse = ChatResponse(
                id = deterministicId,
                type = ChatType.PRIVATE,
                createdAt = System.currentTimeMillis().toString(),
                partnerId = request.targetUserId,
                partnerName = partnerName,
                partnerAvatarUrl = user?.avatarUrl,
                isDeletable = true,
                isPinned = false
            )
            
            // Save it locally so the UI doesn't crash when opening the chat
            val now = System.currentTimeMillis()
            val createdAtIso = Instant.now().toString()
            
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

            // CRITICAL: Add entries to chat_members even for private chats in Mesh mode.
            // This allows ChatSettingsDataLoader to resolve partner metadata (tags, etc.)
            // using the same code path as group chats.
            
            // 1. Add local user
            chatMemberDao.upsertMemberWithLWW(
                ChatMemberEntity(
                    chatId = chatResponse.id,
                    userId = localUserId,
                    role = "OWNER",
                    joinedAt = createdAtIso,
                    status = "ACTIVE",
                    canSendMessages = true,
                    canDeleteMessages = true,
                    canInviteUsers = true,
                    canChangeInfo = true,
                    roleUpdatedAt = now,
                    statusUpdatedAt = now,
                    permissionsUpdatedAt = now
                )
            )

            // 2. Add partner
            chatMemberDao.upsertMemberWithLWW(
                ChatMemberEntity(
                    chatId = chatResponse.id,
                    userId = request.targetUserId,
                    role = "MEMBER",
                    joinedAt = createdAtIso,
                    status = "ACTIVE",
                    canSendMessages = true,
                    canDeleteMessages = false,
                    canInviteUsers = false,
                    canChangeInfo = false,
                    roleUpdatedAt = now,
                    statusUpdatedAt = now,
                    permissionsUpdatedAt = now
                )
            )

            // Broadcast the chat creation to the remote peer, but invert the partner fields
            // so the remote peer receives our name/avatar as their partner, avoiding 'New Mesh Chat'.
            val localUser = userDao.getUser(localUserId)
            val localName = localUser?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim().takeIf { name -> name.isNotEmpty() } ?: it.tag } ?: "Unknown"

            val broadcastChatResponse = chatResponse.copy(
                partnerId = localUserId,
                partnerName = localName,
                partnerAvatarUrl = localUser?.avatarUrl
            )

            val event = NotificationDto.ChatEventDto(broadcastChatResponse, "UPDATE_CHAT")
            val payloadString = json.encodeToString(event)
            val finalPayload = encryptPayloadIfNeeded(chatResponse.id, payloadString)
            
            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "", // Filled by GossipProtocol
                action = "UPDATE_CHAT",
                payload = finalPayload
            )
            gossipProtocol.broadcast(envelope)

            NetworkResult.Success(chatResponse)
        }
    }

    override suspend fun getUserChats(): NetworkResult<List<ChatResponse>> {
        return withContext(ioDispatcher) {
            val localChats = chatDao.observeAllChats().firstOrNull() ?: emptyList()
            val responses = localChats.map { entity ->
                var resolvedPartnerId = entity.partnerId
                var resolvedPartnerName = entity.partnerName
                var resolvedAvatar = entity.partnerAvatarUrl
                
                if (entity.type == ChatType.PRIVATE && resolvedPartnerId != null) {
                    val user = userDao.getUser(resolvedPartnerId)
                    if (user != null) {
                        val newName = "${user.firstName.orEmpty()} ${user.lastName.orEmpty()}".trim().takeIf { it.isNotEmpty() } ?: user.tag
                        val newAvatar = user.avatarUrl
                        
                        // Self-heal the database if name or avatar changed/missing
                        if (newName != resolvedPartnerName || newAvatar != resolvedAvatar) {
                            resolvedPartnerName = newName
                            resolvedAvatar = newAvatar
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
                    partnerId = resolvedPartnerId,
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
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            val callerMember = chatMemberDao.getMember(chatId, localUserId)
            if (callerMember?.role != "OWNER" && callerMember?.role != "ADMIN") {
                return@withContext NetworkResult.Error(403, "Only admins can update permissions")
            }

            val now = System.currentTimeMillis()
            val existing = chatMemberDao.getMember(chatId, targetUserId)
            
            val updateEvent = CrdtMemberUpdateEvent(
                chatId = chatId,
                targetUserId = targetUserId,
                canSendMessages = request.canSendMessages ?: existing?.canSendMessages ?: true,
                canDeleteMessages = request.canDeleteMessages ?: existing?.canDeleteMessages ?: false,
                canInviteUsers = request.canInviteUsers ?: existing?.canInviteUsers ?: false,
                canChangeInfo = request.canChangeInfo ?: existing?.canChangeInfo ?: false,
                permissionsUpdatedAt = now
            )
            
            val mergedEntity = ChatMemberEntity(
                chatId = chatId,
                userId = targetUserId,
                role = existing?.role ?: "MEMBER",
                joinedAt = existing?.joinedAt ?: Instant.now().toString(),
                status = existing?.status ?: "ACTIVE",
                canSendMessages = updateEvent.canSendMessages ?: true,
                canDeleteMessages = updateEvent.canDeleteMessages ?: false,
                canInviteUsers = updateEvent.canInviteUsers ?: false,
                canChangeInfo = updateEvent.canChangeInfo ?: false,
                roleUpdatedAt = existing?.roleUpdatedAt ?: 0L,
                statusUpdatedAt = existing?.statusUpdatedAt ?: 0L,
                permissionsUpdatedAt = now
            )
            
            chatMemberDao.upsertMemberWithLWW(mergedEntity)
            
            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = signatureService.getUserId() ?: "self",
                action = "CRDT_MEMBER_UPDATE",
                payload = json.encodeToString(updateEvent)
            )
            gossipProtocol.broadcast(envelope)
            
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun updateChatInfo(
        chatId: String,
        request: UpdateChatInfoRequest
    ): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val existingChat = chatDao.getChat(chatId)
            if (existingChat != null && existingChat.type == ChatType.GROUP) {
                val localUserId = signatureService.getUserId() ?: "self"
                val callerMember = chatMemberDao.getMember(chatId, localUserId)
                if (callerMember?.canChangeInfo != true && callerMember?.role != "OWNER" && callerMember?.role != "ADMIN") {
                    return@withContext NetworkResult.Error(403, "No permission to change chat info")
                }
            }

            val now = System.currentTimeMillis()
            
            val updateEvent = CrdtChatUpdateEvent(
                chatId = chatId,
                title = request.title,
                description = request.description,
                titleUpdatedAt = now,
                descriptionUpdatedAt = now
            )
            
            if (existingChat != null) {
                chatDao.upsertChat(existingChat.copy(
                    title = request.title,
                    description = request.description,
                    titleUpdatedAt = now,
                    descriptionUpdatedAt = now
                ))
            }
            
            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = signatureService.getUserId() ?: "self",
                action = "CRDT_CHAT_UPDATE",
                payload = json.encodeToString(updateEvent)
            )
            gossipProtocol.broadcast(envelope)
            
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun clearChatHistory(chatId: String, forAll: Boolean): NetworkResult<Unit> {
        if (forAll) {
            withContext(ioDispatcher) {
                val existingChat = chatDao.getChat(chatId)
                if (existingChat != null) {
                    val localUserId = signatureService.getUserId() ?: "self"
                    val callerMember = chatMemberDao.getMember(chatId, localUserId)
                    if (existingChat.type == ChatType.GROUP && callerMember?.role != "OWNER" && callerMember?.role != "ADMIN") {
                        return@withContext NetworkResult.Error(403, "No permission to clear history for all")
                    }

                    val localUser = userDao.getUser(localUserId)
                    val localName = localUser?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim().takeIf { name -> name.isNotEmpty() } ?: it.tag } ?: "Unknown"

                    val chatResponse = ChatResponse(
                        id = existingChat.id,
                        type = existingChat.type,
                        title = existingChat.title,
                        description = existingChat.description,
                        createdAt = existingChat.createdAt,
                        partnerId = localUserId,
                        partnerName = localName,
                        partnerAvatarUrl = localUser?.avatarUrl,
                        partnerLastOnline = existingChat.partnerLastOnline,
                        lastMessage = null, // Clear last message
                        unreadCount = 0,
                        allowedReactions = existingChat.allowedReactions,
                        isDeletable = existingChat.isDeletable,
                        isPinned = existingChat.isPinned
                    )

                    val event = NotificationDto.ChatEventDto(chatResponse, "HISTORY_CLEARED")
                    val envelope = MeshEnvelope(
                        envelopeId = UUID.randomUUID().toString(),
                        originEndpointId = "self",
                        action = "HISTORY_CLEARED",
                        payload = json.encodeToString(event)
                    )
                    gossipProtocol.broadcast(envelope)
                }
            }
        }
        return NetworkResult.Success(Unit)
    }

    override suspend fun deleteChat(chatId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val existingChat = chatDao.getChat(chatId)
            if (existingChat != null) {
                val localUserId = signatureService.getUserId() ?: "self"
                val callerMember = chatMemberDao.getMember(chatId, localUserId)
                if (existingChat.type == ChatType.GROUP && callerMember?.role != "OWNER") {
                    return@withContext NetworkResult.Error(403, "Only the owner can delete the group chat")
                }

                val localUser = userDao.getUser(localUserId)
                val localName = localUser?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim().takeIf { name -> name.isNotEmpty() } ?: it.tag } ?: "Unknown"

                val chatResponse = ChatResponse(
                    id = existingChat.id,
                    type = existingChat.type,
                    title = existingChat.title,
                    description = existingChat.description,
                    createdAt = existingChat.createdAt,
                    partnerId = localUserId,
                    partnerName = localName,
                    partnerAvatarUrl = localUser?.avatarUrl,
                    partnerLastOnline = existingChat.partnerLastOnline,
                    lastMessage = existingChat.lastMessage,
                    unreadCount = existingChat.unreadCount,
                    allowedReactions = existingChat.allowedReactions,
                    isDeletable = existingChat.isDeletable,
                    isPinned = existingChat.isPinned
                )

                val event = NotificationDto.ChatEventDto(chatResponse, "DELETED")
                val envelope = MeshEnvelope(
                    envelopeId = UUID.randomUUID().toString(),
                    originEndpointId = "self",
                    action = "DELETED",
                    payload = json.encodeToString(event)
                )
                gossipProtocol.broadcast(envelope)
            }
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun getChatMembers(
        chatId: String,
        page: Int,
        size: Int
    ): NetworkResult<PageResponse<ChatMemberResponse>> {
        return withContext(ioDispatcher) {
            val activeMembers = chatMemberDao.getActiveMembersSync(chatId)
            val responses = activeMembers.map { member ->
                val user = userDao.getUser(member.userId)
                val shortUser = user?.let {
                    ShortUserDto(
                        id = it.userId,
                        firstName = it.firstName,
                        lastName = it.lastName,
                        tag = it.tag,
                        avatarUrl = it.avatarUrl
                    )
                } ?: ShortUserDto(id = member.userId)
                
                ChatMemberResponse(
                    chatId = member.chatId,
                    userId = member.userId,
                    userDetails = shortUser,
                    role = member.role,
                    joinedAt = member.joinedAt,
                    canSendMessages = member.canSendMessages,
                    canDeleteMessages = member.canDeleteMessages,
                    canInviteUsers = member.canInviteUsers,
                    canChangeInfo = member.canChangeInfo,
                    isPinned = false,
                    historyClearedAt = null
                )
            }
            NetworkResult.Success(
                PageResponse(
                    content = responses,
                    totalElements = responses.size.toLong(),
                    totalPages = 1,
                    size = responses.size,
                    number = 0
                )
            )
        }
    }

    override suspend fun inviteUser(chatId: String, request: TargetUserRequest): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            val callerMember = chatMemberDao.getMember(chatId, localUserId)
            if (callerMember?.canInviteUsers != true && callerMember?.role != "OWNER" && callerMember?.role != "ADMIN") {
                return@withContext NetworkResult.Error(403, "No permission to invite users")
            }

            val now = System.currentTimeMillis()
            
            val updateEvent = CrdtMemberUpdateEvent(
                chatId = chatId,
                targetUserId = request.targetUserId,
                role = "MEMBER",
                status = "ACTIVE",
                canSendMessages = true,
                canDeleteMessages = false,
                canInviteUsers = false,
                canChangeInfo = false,
                roleUpdatedAt = now,
                statusUpdatedAt = now,
                permissionsUpdatedAt = now
            )
            
            val newMemberEntity = ChatMemberEntity(
                chatId = chatId,
                userId = request.targetUserId,
                role = "MEMBER",
                joinedAt = Instant.now().toString(),
                status = "ACTIVE",
                canSendMessages = true,
                canDeleteMessages = false,
                canInviteUsers = false,
                canChangeInfo = false,
                roleUpdatedAt = now,
                statusUpdatedAt = now,
                permissionsUpdatedAt = now
            )
            
            chatMemberDao.upsertMemberWithLWW(newMemberEntity)
            
            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = localUserId,
                action = "CRDT_MEMBER_UPDATE",
                payload = json.encodeToString(updateEvent)
            )
            gossipProtocol.broadcast(envelope)
            
            // Broadcast FULL_STATE_SYNC for the new user to get up to speed
            val chat = chatDao.getChat(chatId)
            if (chat != null) {
                val activeMembers = chatMemberDao.getActiveMembersSync(chatId)
                val memberUpdates = activeMembers.map {
                    CrdtMemberUpdateEvent(
                        chatId = it.chatId,
                        targetUserId = it.userId,
                        role = it.role,
                        status = it.status,
                        canSendMessages = it.canSendMessages,
                        canDeleteMessages = it.canDeleteMessages,
                        canInviteUsers = it.canInviteUsers,
                        canChangeInfo = it.canChangeInfo,
                        roleUpdatedAt = it.roleUpdatedAt,
                        statusUpdatedAt = it.statusUpdatedAt,
                        permissionsUpdatedAt = it.permissionsUpdatedAt
                    )
                }
                val chatUpdate = CrdtChatUpdateEvent(
                    chatId = chat.id,
                    title = chat.title,
                    description = chat.description,
                    titleUpdatedAt = chat.titleUpdatedAt,
                    descriptionUpdatedAt = chat.descriptionUpdatedAt
                )
                val syncEvent = FullStateSyncEvent(chatUpdate, memberUpdates)
                
                val syncEnvelope = MeshEnvelope(
                    envelopeId = UUID.randomUUID().toString(),
                    originEndpointId = localUserId,
                    action = "FULL_STATE_SYNC",
                    payload = json.encodeToString(syncEvent)
                )
                gossipProtocol.broadcast(syncEnvelope)
            }
            
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun kickUser(chatId: String, targetUserId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            val callerMember = chatMemberDao.getMember(chatId, localUserId)
            if (callerMember?.role != "OWNER" && callerMember?.role != "ADMIN") {
                return@withContext NetworkResult.Error(403, "Only admins can kick users")
            }

            val now = System.currentTimeMillis()
            val existing = chatMemberDao.getMember(chatId, targetUserId)
            
            val updateEvent = CrdtMemberUpdateEvent(
                chatId = chatId,
                targetUserId = targetUserId,
                status = "KICKED",
                statusUpdatedAt = now
            )
            
            if (existing != null) {
                val kickedEntity = existing.copy(
                    status = "KICKED",
                    statusUpdatedAt = now
                )
                chatMemberDao.upsertMemberWithLWW(kickedEntity)
            }
            
            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = signatureService.getUserId() ?: "self",
                action = "CRDT_MEMBER_UPDATE",
                payload = json.encodeToString(updateEvent)
            )
            gossipProtocol.broadcast(envelope)
            
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun leaveChat(chatId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            val localUserId = signatureService.getUserId() ?: "self"
            val existing = chatMemberDao.getMember(chatId, localUserId)
            
            val updateEvent = CrdtMemberUpdateEvent(
                chatId = chatId,
                targetUserId = localUserId,
                status = "LEFT",
                statusUpdatedAt = now
            )
            
            if (existing != null) {
                val leftEntity = existing.copy(
                    status = "LEFT",
                    statusUpdatedAt = now
                )
                chatMemberDao.upsertMemberWithLWW(leftEntity)
            }
            
            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = localUserId,
                action = "CRDT_MEMBER_UPDATE",
                payload = json.encodeToString(updateEvent)
            )
            gossipProtocol.broadcast(envelope)
            
            NetworkResult.Success(Unit)
        }
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
