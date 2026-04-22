package ru.kubsu.borshchevyk.core.data.message

import ru.kubsu.borshchevyk.core.domain.message.ChatRepository
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatMemberRole
import ru.kubsu.borshchevyk.core.model.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.model.dto.ChatResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.model.dto.PageResponse
import ru.kubsu.borshchevyk.core.model.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest
import ru.kubsu.borshchevyk.core.network.ChatNetworkDataSource
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val networkDataSource: ChatNetworkDataSource
) : ChatRepository {

    override suspend fun createChat(request: CreateChatRequest): Chat {
        return networkDataSource.createChat(request).toDomain()
    }

    override suspend fun createPrivateChat(request: TargetUserRequest): Chat {
        return networkDataSource.createPrivateChat(request).toDomain()
    }

    override suspend fun getUserChats(): List<Chat> {
        return networkDataSource.getUserChats().map { it.toDomain() }
    }

    override suspend fun updatePermissions(chatId: String, targetUserId: String, request: UpdatePermissionsRequest) {
        networkDataSource.updatePermissions(chatId, targetUserId, request)
    }

    override suspend fun clearChatHistory(chatId: String, forAll: Boolean) {
        networkDataSource.clearChatHistory(chatId, forAll)
    }

    override suspend fun deleteChat(chatId: String) {
        networkDataSource.deleteChat(chatId)
    }

    override suspend fun getChatMembers(chatId: String, page: Int, size: Int): PageResponse<ChatMember> {
        val response = networkDataSource.getChatMembers(chatId, page, size)
        return PageResponse(
            content = response.content.map { it.toDomain() },
            totalElements = response.totalElements,
            totalPages = response.totalPages,
            size = response.size,
            number = response.number
        )
    }

    override suspend fun inviteUser(chatId: String, request: TargetUserRequest) {
        networkDataSource.inviteUser(chatId, request)
    }

    override suspend fun generateInviteLink(chatId: String): String {
        return networkDataSource.generateInviteLink(chatId)
    }

    override suspend fun joinChatByLink(inviteCode: String): Chat {
        return networkDataSource.joinChatByLink(inviteCode).toDomain()
    }

    private fun ChatResponse.toDomain(): Chat = Chat(
        id = id,
        type = type,
        title = title,
        description = description,
        createdAt = createdAt
    )

    private fun ChatMemberResponse.toDomain(): ChatMember = ChatMember(
        chatId = chatId,
        userId = userId,
        role = ChatMemberRole.valueOf(role),
        joinedAt = joinedAt,
        canSendMessages = canSendMessages,
        canDeleteMessages = canDeleteMessages,
        canInviteUsers = canInviteUsers,
        canChangeInfo = canChangeInfo,
        historyClearedAt = historyClearedAt
    )
}
