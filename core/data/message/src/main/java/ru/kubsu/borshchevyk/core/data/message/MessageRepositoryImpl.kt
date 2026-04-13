package ru.kubsu.borshchevyk.core.data.message

import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.dto.MessageResponse
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.network.ChatNetworkDataSource
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val networkDataSource: ChatNetworkDataSource
) : MessageRepository {

    override suspend fun sendMessage(chatId: String, request: SendMessageRequest): Message {
        return networkDataSource.sendMessage(chatId, request).toDomain()
    }

    override suspend fun loadChatHistory(chatId: String, page: Int, size: Int): List<Message> {
        return networkDataSource.loadChatHistory(chatId, page, size).map { it.toDomain() }
    }

    override suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean) {
        networkDataSource.deleteMessage(chatId, messageId, forAll)
    }

    override suspend fun addReaction(chatId: String, messageId: String, reaction: String) {
        networkDataSource.addReaction(chatId, messageId, reaction)
    }

    override suspend fun removeReaction(chatId: String, messageId: String, reaction: String) {
        networkDataSource.removeReaction(chatId, messageId, reaction)
    }

    override suspend fun pinMessage(chatId: String, messageId: String) {
        networkDataSource.pinMessage(chatId, messageId)
    }

    override suspend fun unpinMessage(chatId: String, messageId: String) {
        networkDataSource.unpinMessage(chatId, messageId)
    }

    private fun MessageResponse.toDomain(): Message = Message(
        id = id,
        chatId = chatId,
        authorId = authorId,
        text = text,
        createdAt = createdAt,
        isDeleted = isDeleted,
        source = source,
        isPinned = pinnedAt != null,
        reactions = reactions?.map { MessageReaction(userId = it.userId, reaction = it.reaction) } ?: emptyList()
    )
}
