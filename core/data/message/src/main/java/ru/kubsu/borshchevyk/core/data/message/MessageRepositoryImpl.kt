package ru.kubsu.borshchevyk.core.data.message

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.MessageDao
import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainGlobalChatEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainPresenceStatus
import ru.kubsu.borshchevyk.core.model.domain.DomainReactionEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainTypingEvent
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.model.domain.getOrThrow
import ru.kubsu.borshchevyk.core.model.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.message.MessageNetworkDataSource
import ru.kubsu.borshchevyk.core.network.websocket.ChatWebSocketDataSource
import ru.kubsu.borshchevyk.core.network.websocket.PresenceWebSocketDataSource
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val networkDataSource: MessageNetworkDataSource,
    private val chatWebSocketDataSource: ChatWebSocketDataSource,
    private val presenceWebSocketDataSource: PresenceWebSocketDataSource,
    private val messageDao: MessageDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MessageRepository {

    override suspend fun sendMessage(
        chatId: String, 
        text: String, 
        attachmentIds: List<String>?, 
        forwardedFromChatId: String?, 
        forwardedFromUserId: String?
    ) {
        val messageEntity = networkDataSource.sendMessage(
            chatId, 
            SendMessageRequest(
                text = text,
                attachmentIds = attachmentIds,
                forwardedFromChatId = forwardedFromChatId,
                forwardedFromUserId = forwardedFromUserId
            )
        ).getOrThrow().toEntity()
        
        withContext(ioDispatcher) {
            messageDao.upsertMessage(messageEntity)
        }
    }

    override suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        val messageEntity = networkDataSource.editMessage(chatId, messageId, EditMessageRequest(text = newText)).getOrThrow().toEntity()
        withContext(ioDispatcher) {
            messageDao.upsertMessage(messageEntity)
        }
    }

    override fun observeChatHistory(chatId: String): Flow<List<Message>> {
        return messageDao.observeChatMessages(chatId).map { entities -> 
            entities.map { it.toDomain() } 
        }
    }

    override suspend fun syncChatHistory(chatId: String, page: Int, size: Int) {
        withContext(ioDispatcher) {
            val messages = networkDataSource.loadChatHistory(chatId, page, size).getOrThrow().map { it.toEntity() }
            messageDao.upsertMessages(messages)
        }
    }

    override suspend fun connectWebSocket() {
        chatWebSocketDataSource.connect()
    }

    override suspend fun disconnectWebSocket() {
        chatWebSocketDataSource.disconnect()
    }

    override fun observeNewMessages(): Flow<Message> = 
        chatWebSocketDataSource.observeNewMessages()
            .map { it.toEntity() }
            .onEach { messageDao.upsertMessage(it) }
            .map { it.toDomain() }

    override fun observeChatEvents(): Flow<DomainGlobalChatEvent> = 
        chatWebSocketDataSource.observeChatEvents().map { DomainGlobalChatEvent(it.chat.id, it.action) }

    override fun observeDeletedMessages(): Flow<String> = 
        chatWebSocketDataSource.observeDeletedMessages()
            .onEach { messageId -> 
                messageDao.deleteMessage(messageId) 
            }

    override fun observeTyping(chatId: String): Flow<DomainTypingEvent> = 
        chatWebSocketDataSource.observeTyping(chatId).map { DomainTypingEvent(it.user.id, it.isTyping) }

    override fun observeReactions(chatId: String): Flow<DomainReactionEvent> = 
        chatWebSocketDataSource.observeReactions(chatId).map { DomainReactionEvent(it.messageId, it.user.id, it.reaction, it.isAdded) }

    override fun observePins(chatId: String): Flow<String> = 
        chatWebSocketDataSource.observePins(chatId)

    override fun observeUnpins(chatId: String): Flow<String> = 
        chatWebSocketDataSource.observeUnpins(chatId)

    override fun observeReadReceipts(chatId: String): Flow<DomainReadReceiptEvent> = 
        chatWebSocketDataSource.observeReadReceipts(chatId).map { DomainReadReceiptEvent(it.messageId, it.user.id) }
            .onEach { event ->
                withContext(ioDispatcher) {
                    val msg = messageDao.getMessage(event.messageId)
                    if (msg != null && msg.status != MessageStatus.READ) {
                        messageDao.upsertMessage(msg.copy(status = MessageStatus.READ))
                    }
                }
            }

    override fun observePresence(userId: String): Flow<DomainPresenceStatus> = 
        presenceWebSocketDataSource.observePresence(userId).map { DomainPresenceStatus(it.userId, it.isOnline, it.lastSeenAt) }

    override suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        chatWebSocketDataSource.sendTypingEvent(chatId, isTyping)
    }

    override suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean) {
        networkDataSource.deleteMessage(chatId, messageId, forAll).getOrThrow()
        withContext(ioDispatcher) {
            messageDao.deleteMessage(messageId)
        }
    }

    override suspend fun addReaction(chatId: String, messageId: String, reaction: String) {
        networkDataSource.addReaction(chatId, messageId, reaction).getOrThrow()
    }

    override suspend fun removeReaction(chatId: String, messageId: String, reaction: String) {
        networkDataSource.removeReaction(chatId, messageId, reaction).getOrThrow()
    }

    override suspend fun pinMessage(chatId: String, messageId: String) {
        networkDataSource.pinMessage(chatId, messageId).getOrThrow()
    }

    override suspend fun unpinMessage(chatId: String, messageId: String) {
        networkDataSource.unpinMessage(chatId, messageId).getOrThrow()
    }

    override suspend fun getPinnedMessages(chatId: String): List<Message> {
        return networkDataSource.getPinnedMessages(chatId).getOrThrow().map { it.toEntity().toDomain() }
    }

    override suspend fun readMessage(chatId: String, messageId: String) {
        networkDataSource.readMessage(chatId, messageId).getOrThrow()
    }

    override suspend fun getMessageReaders(chatId: String, messageId: String): List<ru.kubsu.borshchevyk.core.model.domain.User> {
        return networkDataSource.getMessageReaders(chatId, messageId).getOrThrow().map {
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
        return networkDataSource.getMessageComments(chatId, messageId, page, size).getOrThrow().map { it.toEntity().toDomain() }
    }
}
