package ru.kubsu.borshchevyk.core.data.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.ChatDao
import ru.kubsu.borshchevyk.core.domain.chat.ChatRepository
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatMemberRole
import ru.kubsu.borshchevyk.core.model.domain.DomainCreateChatParam
import ru.kubsu.borshchevyk.core.model.domain.DomainPage
import ru.kubsu.borshchevyk.core.model.domain.DomainTargetUserParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateChatInfoParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePermissionsParam
import ru.kubsu.borshchevyk.core.model.domain.GlobalSearchResults
import ru.kubsu.borshchevyk.core.model.domain.getOrThrow
import ru.kubsu.borshchevyk.core.model.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.model.dto.ChatResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.model.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest
import ru.kubsu.borshchevyk.core.network.chat.ChatNetworkDataSource
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val networkDataSource: ChatNetworkDataSource,
    private val chatDao: ChatDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ChatRepository {

    override fun observeUserChats(): Flow<List<Chat>> = chatDao.observeAllChats().map { entities -> 
        entities.map { it.toDomain() }
    }

    override suspend fun createChat(request: DomainCreateChatParam): Chat {
        val chatEntity = networkDataSource.createChat(
            CreateChatRequest(
                type = request.type,
                title = request.title,
                description = request.description,
                initialMemberIds = request.initialMemberIds
            )
        ).getOrThrow().toEntity()
        chatDao.upsertChat(chatEntity)
        return chatEntity.toDomain()
    }

    override suspend fun createPrivateChat(request: DomainTargetUserParam): Chat {
        val chatEntity = networkDataSource.createPrivateChat(
            TargetUserRequest(request.targetUserId)
        ).getOrThrow().toEntity()
        chatDao.upsertChat(chatEntity)
        return chatEntity.toDomain()
    }

    override suspend fun syncUserChats() {
        withContext(ioDispatcher) {
            val networkChats = networkDataSource.getUserChats().getOrThrow()
            chatDao.upsertChats(networkChats.map { it.toEntity() })
        }
    }

    override suspend fun getUserChats(): List<Chat> {
        val cached = chatDao.observeAllChats().firstOrNull()
        if (!cached.isNullOrEmpty()) {
            syncUserChats() // sync in background or await? For simplicity, we just use it directly, but ideally it returns cached.
            return cached.map { it.toDomain() }
        }
        val networkChats = networkDataSource.getUserChats().getOrThrow()
        chatDao.upsertChats(networkChats.map { it.toEntity() })
        return networkChats.map { it.toDomain() }
    }

    override suspend fun updatePermissions(chatId: String, targetUserId: String, request: DomainUpdatePermissionsParam) {
        networkDataSource.updatePermissions(
            chatId, 
            targetUserId, 
            UpdatePermissionsRequest(
                canSendMessages = request.canSendMessages,
                canDeleteMessages = request.canDeleteMessages,
                canInviteUsers = request.canInviteUsers,
                canChangeInfo = request.canChangeInfo
            )
        ).getOrThrow()
    }

    override suspend fun clearChatHistory(chatId: String, forAll: Boolean) {
        networkDataSource.clearChatHistory(chatId, forAll).getOrThrow()
    }

    override suspend fun deleteChat(chatId: String) {
        networkDataSource.deleteChat(chatId).getOrThrow()
    }

    override suspend fun getChatMembers(chatId: String, page: Int, size: Int): DomainPage<ChatMember> {
        val response = networkDataSource.getChatMembers(chatId, page, size).getOrThrow()
        return DomainPage(
            content = response.content.map { it.toDomain() },
            pageNumber = response.number,
            pageSize = response.size,
            totalElements = response.totalElements,
            totalPages = response.totalPages,
            last = response.content.isEmpty() // simplified last check
        )
    }

    override suspend fun inviteUser(chatId: String, request: DomainTargetUserParam) {
        networkDataSource.inviteUser(chatId, TargetUserRequest(request.targetUserId)).getOrThrow()
    }

    override suspend fun kickUser(chatId: String, targetUserId: String) {
        networkDataSource.kickUser(chatId, targetUserId).getOrThrow()
    }

    override suspend fun leaveChat(chatId: String) {
        networkDataSource.leaveChat(chatId).getOrThrow()
    }

    override suspend fun updateChatInfo(chatId: String, request: DomainUpdateChatInfoParam) {
        networkDataSource.updateChatInfo(
            chatId, 
            UpdateChatInfoRequest(
                title = request.title,
                description = request.description
            )
        ).getOrThrow()
    }

    override suspend fun generateInviteLink(chatId: String): String {
        return networkDataSource.generateInviteLink(chatId).getOrThrow()
    }

    override suspend fun joinChatByLink(inviteCode: String): Chat {
        return networkDataSource.joinChatByLink(inviteCode).getOrThrow().toDomain()
    }

    override suspend fun pinChat(chatId: String) {
        networkDataSource.pinChat(chatId).getOrThrow()
    }

    override suspend fun unpinChat(chatId: String) {
        networkDataSource.unpinChat(chatId).getOrThrow()
    }

    override suspend fun globalSearch(query: String): ru.kubsu.borshchevyk.core.model.domain.GlobalSearchResults {
        val response = networkDataSource.globalSearch(query).getOrThrow()
        return ru.kubsu.borshchevyk.core.model.domain.GlobalSearchResults(
            users = response.users.map { 
                ru.kubsu.borshchevyk.core.model.domain.User(
                    userId = it.id,
                    firstName = it.firstName,
                    lastName = it.lastName,
                    tag = it.tag ?: "",
                    avatarUrl = it.avatarUrl
                )
            },
            chats = response.chats.map {
                Chat(
                    id = it.id,
                    type = ru.kubsu.borshchevyk.core.model.domain.ChatType.GROUP, // Global search chats are groups or channels
                    title = it.name,
                    createdAt = "" // default since search dto is short
                )
            }
        )
    }

    private fun ChatResponse.toDomain(): Chat = Chat(
        id = id,
        type = type,
        title = if (type == ru.kubsu.borshchevyk.core.model.domain.ChatType.PRIVATE) partnerName ?: title else title,
        description = description,
        partnerId = partnerId,
        partnerName = partnerName,
        partnerAvatarUrl = partnerAvatarUrl,
        partnerLastOnline = partnerLastOnline,
        lastMessage = lastMessage,
        unreadCount = unreadCount,
        allowedReactions = allowedReactions,
        isDeletable = isDeletable,
        isPinned = isPinned,
        createdAt = createdAt
        )
    private fun ChatMemberResponse.toDomain(): ChatMember = ChatMember(
        chatId = chatId,
        userId = userId,
        user = userDetails?.let {
            ru.kubsu.borshchevyk.core.model.domain.User(
                userId = it.id,
                firstName = it.firstName,
                lastName = it.lastName,
                tag = it.tag ?: "",
                avatarUrl = it.avatarUrl
            )
        },
        role = ChatMemberRole.valueOf(role),
        joinedAt = joinedAt,
        canSendMessages = canSendMessages,
        canDeleteMessages = canDeleteMessages,
        canInviteUsers = canInviteUsers,
        canChangeInfo = canChangeInfo,
        historyClearedAt = historyClearedAt
    )
}
