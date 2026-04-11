package ru.kubsu.borshchevyk.core.data.message

import ru.kubsu.borshchevyk.core.domain.message.ChatRepository
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.dto.ChatResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest
import ru.kubsu.borshchevyk.core.network.ChatNetworkDataSource
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val networkDataSource: ChatNetworkDataSource
) : ChatRepository {

    override suspend fun createChat(request: CreateChatRequest): Chat {
        return networkDataSource.createChat(request).toDomain()
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

    private fun ChatResponse.toDomain(): Chat = Chat(
        id = id,
        type = type,
        title = title,
        description = description,
        createdAt = createdAt
    )
}
