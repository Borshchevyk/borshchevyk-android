package ru.kubsu.borshchevyk.core.data.message

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.DomainGlobalChatEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainPresenceStatus
import ru.kubsu.borshchevyk.core.model.domain.DomainReactionEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainTypingEvent
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.core.model.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.model.dto.MessageResponse
import ru.kubsu.borshchevyk.core.model.dto.NotificationDto
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.network.message.MessageNetworkDataSource
import ru.kubsu.borshchevyk.core.network.websocket.WebSocketDataSource
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val networkDataSource: MessageNetworkDataSource,
    private val webSocketDataSource: WebSocketDataSource
) : MessageRepository {

    override suspend fun sendMessage(
        chatId: String, 
        text: String, 
        attachmentIds: List<String>?, 
        forwardedFromChatId: String?, 
        forwardedFromUserId: String?
    ): Message {
        return networkDataSource.sendMessage(
            chatId, 
            SendMessageRequest(
                text = text,
                attachmentIds = attachmentIds,
                forwardedFromChatId = forwardedFromChatId,
                forwardedFromUserId = forwardedFromUserId
            )
        ).toDomain()
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

    override fun observeNewMessages(): Flow<Message> = 
        webSocketDataSource.observeNewMessages().map { it.toDomain() }

    override fun observeChatEvents(): Flow<DomainGlobalChatEvent> = 
        webSocketDataSource.observeChatEvents().map { DomainGlobalChatEvent(it.chat.id, it.action) }

    override fun observeDeletedMessages(): Flow<String> = 
        webSocketDataSource.observeDeletedMessages()

    override fun observeTyping(chatId: String): Flow<DomainTypingEvent> = 
        webSocketDataSource.observeTyping(chatId).map { DomainTypingEvent(it.user.id, it.isTyping) }

    override fun observeReactions(chatId: String): Flow<DomainReactionEvent> = 
        webSocketDataSource.observeReactions(chatId).map { DomainReactionEvent(it.messageId, it.user.id, it.reaction, it.isAdded) }

    override fun observePins(chatId: String): Flow<String> = 
        webSocketDataSource.observePins(chatId)

    override fun observeUnpins(chatId: String): Flow<String> = 
        webSocketDataSource.observeUnpins(chatId)

    override fun observeReadReceipts(chatId: String): Flow<DomainReadReceiptEvent> = 
        webSocketDataSource.observeReadReceipts(chatId).map { DomainReadReceiptEvent(it.messageId, it.user.id) }

    override fun observePresence(userId: String): Flow<DomainPresenceStatus> = 
        webSocketDataSource.observePresence(userId).map { DomainPresenceStatus(it.userId, it.isOnline, it.lastSeenAt) }

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

    override suspend fun getMessageReaders(chatId: String, messageId: String): List<ru.kubsu.borshchevyk.core.model.domain.User> {
        return networkDataSource.getMessageReaders(chatId, messageId).map {
            ru.kubsu.borshchevyk.core.model.domain.User(
                userId = it.id,
                firstName = it.firstName,
                lastName = it.lastName,
                tag = it.tag ?: "",
                avatarUrl = it.avatarUrl
            )
        }
    }

    override suspend fun getMessageComments(
        chatId: String,
        messageId: String,
        page: Int,
        size: Int
    ): List<Message> {
        return networkDataSource.getMessageComments(chatId, messageId, page, size).map { it.toDomain() }
    }

    private fun NotificationDto.MessageDto.toDomain(): Message = Message(
        id = id,
        chatId = chat.id,
        authorId = author.id,
        author = ru.kubsu.borshchevyk.core.model.domain.User(
            userId = author.id,
            firstName = author.firstName,
            lastName = author.lastName,
            tag = author.tag ?: "",
            avatarUrl = author.avatarUrl
        ),
        text = text,
        createdAt = createdAt,
        status = status?.let { MessageStatus.valueOf(it) },
        isDeleted = isDeleted,
        source = MessageSource.ONLINE,
        isPinned = false,
        reactions = emptyList(),
        commentsCount = 0,
        parentMessageId = null,
        forwardedFromChatId = forwardedFromChat?.id,
        forwardedFromUserId = forwardedFromUser?.id,
        forwardedFromUser = forwardedFromUser?.let { 
            ru.kubsu.borshchevyk.core.model.domain.User(
                userId = it.id,
                firstName = it.firstName,
                lastName = it.lastName,
                tag = it.tag ?: "",
                avatarUrl = it.avatarUrl
            )
        },
        attachments = attachments?.map {
            Attachment(
                id = it.id,
                type = it.type ?: AttachmentType.FILE,
                originalFilename = it.originalFilename ?: "file",
                extension = it.extension ?: "",
                sizeBytes = it.sizeBytes ?: 0L,
                thumbnailKey = it.thumbnailKey,
                updatedAt = it.updatedAt,
                width = it.width,
                height = it.height,
                duration = it.duration
            )
        } ?: attachmentIdsOld?.map { 
            Attachment(id = it.id, type = AttachmentType.FILE) 
        } ?: emptyList()
    )

    private fun MessageResponse.toDomain(): Message = Message(
        id = id,
        chatId = chat.id,
        authorId = author.id,
        author = ru.kubsu.borshchevyk.core.model.domain.User(
            userId = author.id,
            firstName = author.firstName,
            lastName = author.lastName,
            tag = author.tag ?: "",
            avatarUrl = author.avatarUrl
        ),
        text = text,
        createdAt = createdAt,
        updatedAt = updatedAt,
        status = status,
        isDeleted = isDeleted,
        source = source,
        isPinned = pinnedAt != null,
        reactions = reactions?.map { MessageReaction(userId = it.userId, reaction = it.reaction) } ?: emptyList(),
        commentsCount = commentsCount,
        parentMessageId = parentMessageId,
        forwardedFromChatId = forwardedFromChat?.id,
        forwardedFromUserId = forwardedFromUser?.id,
        forwardedFromUser = forwardedFromUser?.let { 
            ru.kubsu.borshchevyk.core.model.domain.User(
                userId = it.id,
                firstName = it.firstName,
                lastName = it.lastName,
                tag = it.tag ?: "",
                avatarUrl = it.avatarUrl
            )
        },
        attachments = attachments?.map {
            Attachment(
                id = it.id,
                type = it.type ?: AttachmentType.FILE,
                originalFilename = it.originalFilename ?: "file",
                extension = it.extension ?: "",
                sizeBytes = it.sizeBytes ?: 0L,
                thumbnailKey = it.thumbnailKey,
                updatedAt = it.updatedAt,
                width = it.width,
                height = it.height,
                duration = it.duration
            )
        } ?: attachmentIdsOld?.map { Attachment(id = it, type = AttachmentType.FILE) 
        } ?: emptyList()
    )
}
