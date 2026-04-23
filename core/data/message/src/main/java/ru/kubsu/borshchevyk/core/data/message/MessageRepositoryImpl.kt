package ru.kubsu.borshchevyk.core.data.message
import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.core.model.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.model.dto.MessageResponse
import ru.kubsu.borshchevyk.core.model.dto.NotificationDto
import ru.kubsu.borshchevyk.core.model.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.model.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.model.dto.TypingEvent
import ru.kubsu.borshchevyk.core.network.ChatNetworkDataSource
import ru.kubsu.borshchevyk.core.network.WebSocketDataSource
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val networkDataSource: ChatNetworkDataSource,
    private val webSocketDataSource: WebSocketDataSource
) : MessageRepository {

    override suspend fun sendMessage(chatId: String, request: SendMessageRequest): Message {
        return networkDataSource.sendMessage(chatId, request).toDomain()
    }

    override suspend fun editMessage(chatId: String, messageId: String, newText: String): Message {
        return networkDataSource.editMessage(chatId, messageId, EditMessageRequest(text = newText)).toDomain()
    }

    override suspend fun loadChatHistory(chatId: String, page: Int, size: Int): List<Message> {
        return networkDataSource.loadChatHistory(chatId, page, size).map { it.toDomain() }
    }

    override suspend fun connectWebSocket() {
        webSocketDataSource.connect()
    }

    override suspend fun disconnectWebSocket() {
        webSocketDataSource.disconnect()
    }

    override fun observeNewMessages(): Flow<NotificationDto.MessageDto> = webSocketDataSource.observeNewMessages()

    override fun observeDeletedMessages(): Flow<String> = webSocketDataSource.observeDeletedMessages()

    override fun observeTyping(chatId: String): Flow<TypingEvent> = webSocketDataSource.observeTyping(chatId)

    override fun observeReactions(chatId: String): Flow<ReactionEvent> = webSocketDataSource.observeReactions(chatId)

    override fun observePins(chatId: String): Flow<String> = webSocketDataSource.observePins(chatId)

    override fun observeUnpins(chatId: String): Flow<String> = webSocketDataSource.observeUnpins(chatId)

    override fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent> = webSocketDataSource.observeReadReceipts(chatId)

    override suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        webSocketDataSource.sendTypingEvent(chatId, isTyping)
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

    override suspend fun getPinnedMessages(chatId: String): List<Message> {
        return networkDataSource.getPinnedMessages(chatId).map { it.toDomain() }
    }

    override suspend fun readMessage(chatId: String, messageId: String) {
        networkDataSource.readMessage(chatId, messageId)
    }

    override suspend fun getMessageReaders(chatId: String, messageId: String): List<String> {
        return networkDataSource.getMessageReaders(chatId, messageId)
    }

    override suspend fun getMessageComments(
        chatId: String,
        messageId: String,
        page: Int,
        size: Int
    ): List<Message> {
        return networkDataSource.getMessageComments(chatId, messageId, page, size).map { it.toDomain() }
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
        reactions = reactions?.map { MessageReaction(userId = it.userId, reaction = it.reaction) } ?: emptyList(),
        commentsCount = commentsCount,
        parentMessageId = parentMessageId,
        forwardedFromChatId = forwardedFromChatId,
        forwardedFromUserId = forwardedFromUserId,
        attachments = attachments?.map { 
            Attachment(
                id = it.id,
                type = it.type ?: AttachmentType.FILE,
                originalFilename = it.originalFilename ?: "file",
                extension = it.extension ?: "",
                sizeBytes = it.sizeBytes ?: 0L
            )
        } ?: attachmentIdsOld?.map { 
            Attachment(id = it, type = AttachmentType.FILE) 
        } ?: emptyList()
    )
}
