package ru.kubsu.borshchevyk.core.data.message

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.channels.Channel
import android.util.Log
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.MessageDao
import ru.kubsu.borshchevyk.core.database.dao.ChatDao
import ru.kubsu.borshchevyk.core.data.chat.toEntity
import ru.kubsu.borshchevyk.core.data.chat.toDomain
import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainGlobalChatEvent
import ru.kubsu.borshchevyk.core.model.domain.GlobalChatAction
import ru.kubsu.borshchevyk.core.model.domain.DomainPresenceStatus
import ru.kubsu.borshchevyk.core.model.domain.DomainReactionEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainTypingEvent
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.network.client.getOrThrow
import ru.kubsu.borshchevyk.core.network.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.message.MessageNetworkDataSource
import ru.kubsu.borshchevyk.core.network.websocket.ChatWebSocketDataSource
import ru.kubsu.borshchevyk.core.network.websocket.PresenceWebSocketDataSource
import javax.inject.Inject

/**
 * Implementation of [MessageRepository] managing chat messaging and real-time events.
 *
 * Handles sending messages, observing chat history, managing reactions, and handling real-time
 * WebSocket connections for presence, typing events, and read receipts.
 *
 * @property networkDataSource Source for messaging REST API operations.
 * @property chatWebSocketDataSource Source for real-time WebSocket chat events.
 * @property presenceWebSocketDataSource Source for real-time WebSocket user presence events.
 * @property messageDao Local Room database DAO for caching message entities.
 * @property ioDispatcher Coroutine dispatcher for executing I/O bound database and network operations.
 */
class MessageRepositoryImpl @Inject constructor(
    private val networkDataSource: MessageNetworkDataSource,
    private val chatWebSocketDataSource: ChatWebSocketDataSource,
    private val presenceWebSocketDataSource: PresenceWebSocketDataSource,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MessageRepository {

    /**
     * Sends a new message to a specific chat, saving the resulting message into the local database cache.
     *
     * @param chatId The ID of the chat.
     * @param text The text content of the message.
     * @param attachmentIds Optional list of attachment IDs.
     * @param forwardedFromChatId Optional ID of the chat a message is forwarded from.
     * @param forwardedFromUserId Optional ID of the user a message is forwarded from.
     */
    override suspend fun sendMessage(
        chatId: String, 
        text: String, 
        attachmentIds: List<String>?, 
        forwardedFromChatId: String?, 
        forwardedFromUserId: String?
    ) {
        val message = networkDataSource.sendMessage(
            chatId, 
            SendMessageRequest(
                text = text,
                attachmentIds = attachmentIds,
                forwardedFromChatId = forwardedFromChatId,
                forwardedFromUserId = forwardedFromUserId
            )
        ).getOrThrow().toDomain()
        
        withContext(ioDispatcher) {
            message.saveToDb()
        }
    }

    /**
     * Edits an existing message in a chat and updates the local cache.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message to edit.
     * @param newText The new text content for the message.
     */
    override suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        val message = networkDataSource.editMessage(chatId, messageId, EditMessageRequest(text = newText)).getOrThrow().toDomain()
        withContext(ioDispatcher) {
            message.saveToDb()
        }
    }

    /**
     * Observes the message history for a specific chat from the local database.
     *
     * @param chatId The ID of the chat to observe.
     * @return A Flow emitting a list of messages for the given chat.
     */
    override fun observeChatHistory(chatId: String): Flow<List<Message>> {
        return messageDao.observeChatMessages(chatId).map { entities -> 
            entities.map { it.toDomain() } 
        }
    }

    /**
     * Synchronizes a page of chat history from the backend into the local database cache.
     *
     * @param chatId The ID of the chat to sync.
     * @param page The page number to fetch.
     * @param size The number of items per page.
     */
    override suspend fun syncChatHistory(chatId: String, page: Int, size: Int) {
        withContext(ioDispatcher) {
            val messages = networkDataSource.loadChatHistory(chatId, page, size).getOrThrow().map { it.toDomain() }
            
            messages.filter { it.isDeleted }.forEach {
                messageDao.deleteMessage(it.id)
            }

            val activeMessages = messages.filter { !it.isDeleted }
            val messageEntities = activeMessages.map { it.toMessageEntity() }
            val users = activeMessages.flatMap { listOfNotNull(it.toAuthorEntity(), it.toForwardedUserEntity()) }.distinctBy { it.userId }
            val attachments = activeMessages.flatMap { it.toAttachmentEntities() }
            val reactions = activeMessages.flatMap { it.toReactionEntities() }
            
            messageDao.upsertMessagesWithDetails(messageEntities, users, attachments, reactions)
        }
    }

    /**
     * Establishes a WebSocket connection for real-time chat updates.
     */
    override suspend fun connectWebSocket() {
        chatWebSocketDataSource.connect()
    }

    /**
     * Terminates the WebSocket connection for real-time chat updates.
     */
    override suspend fun disconnectWebSocket() {
        chatWebSocketDataSource.disconnect()
    }

    /**
     * Observes real-time incoming messages from the WebSocket and saves them locally.
     *
     * @return A [Flow] emitting the newly received [Message] models.
     */
    override fun observeNewMessages(): Flow<Message> = 
        chatWebSocketDataSource.observeNewMessages()
            .buffer(capacity = Channel.BUFFERED)
            .map { it.toDomain() }
            .onEach { domainMsg -> 
                try {
                    withContext(ioDispatcher) {
                        domainMsg.saveToDb() 
                    }
                } catch (e: Exception) {
                    Log.e("MessageRepository", "Failed to save new message to db: ${domainMsg.id}", e)
                }
            }
            .catch { e -> Log.e("MessageRepository", "Fatal error in observeNewMessages pipeline", e) }

    /**
     * Observes global chat events (e.g., chat created, deleted, updated) from the WebSocket.
     *
     * @return A [Flow] emitting [DomainGlobalChatEvent] models.
     */
    override fun observeChatEvents(): Flow<DomainGlobalChatEvent> = 
        chatWebSocketDataSource.observeChatEvents()
            .buffer(capacity = Channel.BUFFERED)
            .map { event -> 
                val action = GlobalChatAction.fromString(event.action)
                DomainGlobalChatEvent(event.chat.toDomain(), action) to event.chat
            }
            .onEach { (domainEvent, chatDto) ->
                try {
                    withContext(ioDispatcher) {
                        val action = domainEvent.action
                        if (action == GlobalChatAction.DELETED || action == GlobalChatAction.KICKED || action == GlobalChatAction.LEFT) {
                            chatDao.deleteChat(domainEvent.chat.id)
                            messageDao.deleteMessagesByChat(domainEvent.chat.id)
                        } else if (action == GlobalChatAction.HISTORY_CLEARED) {
                            chatDao.upsertChat(chatDto.toEntity())
                            messageDao.deleteMessagesByChat(domainEvent.chat.id)
                        } else {
                            chatDao.upsertChat(chatDto.toEntity())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MessageRepository", "Failed to sync chat event: ${domainEvent.action}", e)
                }
            }
            .map { it.first }
            .catch { e -> Log.e("MessageRepository", "Fatal error in observeChatEvents pipeline", e) }

    /**
     * Observes real-time deleted message events and removes them from the local cache.
     *
     * @return A [Flow] emitting the ID of the deleted message.
     */
    override fun observeDeletedMessages(): Flow<String> = 
        chatWebSocketDataSource.observeDeletedMessages()
            .onEach { messageId -> 
                withContext(ioDispatcher) {
                    messageDao.deleteMessage(messageId) 
                }
            }

    /**
     * Observes typing events within a specific chat.
     *
     * @param chatId The ID of the chat.
     * @return A [Flow] emitting [DomainTypingEvent] models.
     */
    override fun observeTyping(chatId: String): Flow<DomainTypingEvent> = 
        chatWebSocketDataSource.observeTyping(chatId).map { DomainTypingEvent(it.user.id, it.isTyping) }

    /**
     * Observes reaction updates (added or removed) within a specific chat.
     *
     * @param chatId The ID of the chat.
     * @return A [Flow] emitting [DomainReactionEvent] models.
     */
    override fun observeReactions(chatId: String): Flow<DomainReactionEvent> = 
        chatWebSocketDataSource.observeReactions(chatId).map { DomainReactionEvent(it.messageId, it.user.id, it.reaction, it.isAdded) }

    /**
     * Observes newly pinned messages within a specific chat.
     *
     * @param chatId The ID of the chat.
     * @return A [Flow] emitting the ID of the pinned message.
     */
    override fun observePins(chatId: String): Flow<String> = 
        chatWebSocketDataSource.observePins(chatId)

    /**
     * Observes newly unpinned messages within a specific chat.
     *
     * @param chatId The ID of the chat.
     * @return A [Flow] emitting the ID of the unpinned message.
     */
    override fun observeUnpins(chatId: String): Flow<String> = 
        chatWebSocketDataSource.observeUnpins(chatId)

    /**
     * Observes read receipt updates for messages and updates the local cache accordingly.
     *
     * @param chatId The ID of the chat.
     * @return A [Flow] emitting [DomainReadReceiptEvent] models.
     */
    override fun observeReadReceipts(chatId: String): Flow<DomainReadReceiptEvent> = 
        chatWebSocketDataSource.observeReadReceipts(chatId).map { DomainReadReceiptEvent(it.messageId, it.user.id) }
            .onEach { event ->
                withContext(ioDispatcher) {
                    val msgWithDetails = messageDao.getMessage(event.messageId)
                    if (msgWithDetails != null && msgWithDetails.message.status != MessageStatus.READ) {
                        val updatedMsg = msgWithDetails.message.copy(status = MessageStatus.READ)
                        messageDao.upsertMessageWithDetails(
                            message = updatedMsg,
                            author = msgWithDetails.author,
                            forwardedFromUser = msgWithDetails.forwardedFromUser,
                            attachments = msgWithDetails.attachments,
                            reactions = msgWithDetails.reactions
                        )
                    }
                }
            }

    /**
     * Observes real-time online presence status for a specific user.
     *
     * @param userId The ID of the user.
     * @return A [Flow] emitting [DomainPresenceStatus] models.
     */
    override fun observePresence(userId: String): Flow<DomainPresenceStatus> = 
        presenceWebSocketDataSource.observePresence(userId).map { DomainPresenceStatus(it.userId, it.isOnline, it.lastSeenAt) }

    /**
     * Sends a typing event indicator to the chat via WebSocket.
     *
     * @param chatId The ID of the chat.
     * @param isTyping True if the user is currently typing, false otherwise.
     */
    override suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        chatWebSocketDataSource.sendTypingEvent(chatId, isTyping)
    }

    /**
     * Deletes a message from a chat.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message to delete.
     * @param forAll Whether to delete the message for everyone or just the local user.
     */
    override suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean) {
        networkDataSource.deleteMessage(chatId, messageId, forAll).getOrThrow()
        withContext(ioDispatcher) {
            messageDao.deleteMessage(messageId)
        }
    }

    /**
     * Adds a reaction to a specific message.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message.
     * @param reaction The reaction string (e.g., an emoji).
     */
    override suspend fun addReaction(chatId: String, messageId: String, reaction: String) {
        networkDataSource.addReaction(chatId, messageId, reaction).getOrThrow()
    }

    /**
     * Removes a reaction from a specific message.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message.
     * @param reaction The reaction string to remove.
     */
    override suspend fun removeReaction(chatId: String, messageId: String, reaction: String) {
        networkDataSource.removeReaction(chatId, messageId, reaction).getOrThrow()
    }

    /**
     * Pins a specific message in a chat.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message to pin.
     */
    override suspend fun pinMessage(chatId: String, messageId: String) {
        networkDataSource.pinMessage(chatId, messageId).getOrThrow()
    }

    /**
     * Unpins a previously pinned message in a chat.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message to unpin.
     */
    override suspend fun unpinMessage(chatId: String, messageId: String) {
        networkDataSource.unpinMessage(chatId, messageId).getOrThrow()
    }

    /**
     * Retrieves all pinned messages in a chat.
     *
     * @param chatId The ID of the chat.
     * @return A list of pinned [Message]s.
     */
    override suspend fun getPinnedMessages(chatId: String): List<Message> {
        return networkDataSource.getPinnedMessages(chatId).getOrThrow().map { it.toDomain() }
    }

    /**
     * Marks a message as read, notifying the server.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the read message.
     */
    override suspend fun readMessage(chatId: String, messageId: String) {
        networkDataSource.readMessage(chatId, messageId).getOrThrow()
    }

    /**
     * Retrieves the list of users who have read a specific message.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message.
     * @return A list of [User]s who read the message.
     */
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

    /**
     * Retrieves comments (replies) to a specific message.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the parent message.
     * @param page The zero-based page index.
     * @param size The number of comments per page.
     * @return A list of comment [Message]s.
     */
    override suspend fun getMessageComments(
        chatId: String,
        messageId: String,
        page: Int,
        size: Int
    ): List<Message> {
        return networkDataSource.getMessageComments(chatId, messageId, page, size).getOrThrow().map { it.toDomain() }
    }

    /**
     * Helper extension to save a domain [Message] and its related details (author, attachments, etc.)
     * into the local Room database cache.
     */
    private fun Message.saveToDb() {
        if (isDeleted) {
            messageDao.deleteMessage(id)
            return
        }
        messageDao.upsertMessageWithDetails(
            message = toMessageEntity(),
            author = toAuthorEntity(),
            forwardedFromUser = toForwardedUserEntity(),
            attachments = toAttachmentEntities(),
            reactions = toReactionEntities()
        )
    }
}
