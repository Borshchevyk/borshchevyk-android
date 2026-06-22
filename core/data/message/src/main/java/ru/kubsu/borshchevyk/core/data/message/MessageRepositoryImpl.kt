package ru.kubsu.borshchevyk.core.data.message

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.data.chat.toDomain
import ru.kubsu.borshchevyk.core.data.chat.toEntity
import ru.kubsu.borshchevyk.core.database.dao.ChatDao
import ru.kubsu.borshchevyk.core.database.dao.MessageDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.ReactionEntity
import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainGlobalChatEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainPresenceStatus
import ru.kubsu.borshchevyk.core.model.domain.DomainReactionEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainTypingEvent
import ru.kubsu.borshchevyk.core.model.domain.GlobalChatAction
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.network.client.getOrThrow
import ru.kubsu.borshchevyk.core.network.di.ApplicationScope
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import ru.kubsu.borshchevyk.core.network.message.MessageNetworkDataSource
import ru.kubsu.borshchevyk.core.network.websocket.ChatWebSocketDataSource
import ru.kubsu.borshchevyk.core.network.websocket.PresenceWebSocketDataSource
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.data.sync.SyncRepository
import ru.kubsu.borshchevyk.core.model.domain.EventType
import java.util.UUID
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
    private val userDao: UserDao,
    private val meshProfileListener: MeshProfileListener,
    private val signatureService: MeshSignatureService,
    private val syncRepository: SyncRepository,
    private val getUserIdUseCase: GetUserIdUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) : MessageRepository {

    private val json = Json { ignoreUnknownKeys = true }

    init {
        meshProfileListener.startListening()
        // Ensure that Mesh chat events are processed even when the UI is not active.
        observeChatEvents().launchIn(scope)
        
        // Subscribe to incoming background sync events for the Message Domain
        syncRepository.incomingEvents
            .filter { it.eventType.name.startsWith("MESSAGE_") }
            .onEach { processMessageEvent(it) }
            .launchIn(scope)
    }

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
     * Retrieves a paginated list of messages containing attachments of a specific category.
     * Attempts to fetch from the network first. If the network returns empty (e.g., in Mesh mode),
     * it falls back to querying the local database to support offline and P2P environments.
     *
     * @param chatId The ID of the chat.
     * @param type The attachment type category.
     * @param page The page number to fetch.
     * @param size The number of items per page.
     * @return A list of [Message] objects.
     */
    override suspend fun loadChatAttachments(chatId: String, type: String, page: Int, size: Int): List<Message> {
        return withContext(ioDispatcher) {
            try {
                val networkResult = networkDataSource.loadChatAttachments(chatId, type, page, size)
                if (networkResult is ru.kubsu.borshchevyk.core.network.client.NetworkResult.Success && networkResult.data.isNotEmpty()) {
                    networkResult.data.map { it.toDomain() }
                } else {
                    val offset = page * size
                    messageDao.getMessagesWithAttachments(chatId, type, size, offset).map { it.toDomain() }
                }
            } catch (e: Exception) {
                val offset = page * size
                messageDao.getMessagesWithAttachments(chatId, type, size, offset).map { it.toDomain() }
            }
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
                        val existingMsgWithDetails = messageDao.getMessage(domainMsg.id)
                        if (existingMsgWithDetails != null) {
                            // This is an update (e.g., EDIT_MESSAGE from Mesh) or a redelivery.
                            // We should merge the new data (like text and updatedAt) while preserving 
                            // existing state (like source, attachments, reactions, read status).
                            val updatedMessageEntity = existingMsgWithDetails.message.copy(
                                text = domainMsg.text.takeIf { it.isNotEmpty() } ?: existingMsgWithDetails.message.text,
                                updatedAt = domainMsg.updatedAt ?: existingMsgWithDetails.message.updatedAt,
                                // Prevent overriding a READ status with an older RECEIVED_BY_USER status
                                status = if (existingMsgWithDetails.message.status == MessageStatus.READ) MessageStatus.READ else domainMsg.status
                            )
                            messageDao.upsertMessageWithDetails(
                                message = updatedMessageEntity,
                                author = existingMsgWithDetails.author,
                                forwardedFromUser = existingMsgWithDetails.forwardedFromUser,
                                attachments = existingMsgWithDetails.attachments,
                                reactions = existingMsgWithDetails.reactions
                            )
                        } else {
                            // Completely new message
                            domainMsg.saveToDb() 
                        }
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
                            val entityToSave = chatDto.toEntity()
                            chatDao.upsertChat(entityToSave)
                            messageDao.deleteMessagesByChat(domainEvent.chat.id)
                        } else {
                            val entityToSave = chatDto.toEntity()
                            // For non-MESSAGE actions (e.g. INFO_UPDATED, PINNED),
                            // preserve the local unreadCount to avoid overwriting
                            // a freshly-zeroed value with stale server data.
                            val action = domainEvent.action
                            if (action != GlobalChatAction.MESSAGE) {
                                val existingChat = chatDao.getChat(entityToSave.id)
                                if (existingChat != null && existingChat.unreadCount == 0L && entityToSave.unreadCount > 0) {
                                    chatDao.upsertChat(entityToSave.copy(unreadCount = 0))
                                } else {
                                    chatDao.upsertChat(entityToSave)
                                }
                            } else {
                                chatDao.upsertChat(entityToSave)
                            }
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
            .onEach { event ->
                withContext(ioDispatcher) {
                    if (event.isAdded) {
                        val reactionEntity = ru.kubsu.borshchevyk.core.database.entity.ReactionEntity(
                            messageId = event.messageId,
                            userId = event.userId,
                            reaction = event.reaction
                        )
                        messageDao.insertReactions(listOf(reactionEntity))
                    } else {
                        // Delete a specific user's reaction on a message
                        messageDao.deleteReaction(event.messageId, event.userId, event.reaction)
                    }
                }
            }

    /**
     * Observes newly pinned messages within a specific chat.
     *
     * @param chatId The ID of the chat.
     * @return A [Flow] emitting the ID of the pinned message.
     */
    override fun observePins(chatId: String): Flow<String> = 
        chatWebSocketDataSource.observePins(chatId)
            .onEach { messageId ->
                withContext(ioDispatcher) {
                    val msgWithDetails = messageDao.getMessage(messageId)
                    if (msgWithDetails != null && !msgWithDetails.message.isPinned) {
                        val updatedMsg = msgWithDetails.message.copy(isPinned = true)
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
     * Observes newly unpinned messages within a specific chat.
     *
     * @param chatId The ID of the chat.
     * @return A [Flow] emitting the ID of the unpinned message.
     */
    override fun observeUnpins(chatId: String): Flow<String> = 
        chatWebSocketDataSource.observeUnpins(chatId)
            .onEach { messageId ->
                withContext(ioDispatcher) {
                    val msgWithDetails = messageDao.getMessage(messageId)
                    if (msgWithDetails != null && msgWithDetails.message.isPinned) {
                        val updatedMsg = msgWithDetails.message.copy(isPinned = false)
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
        val result = networkDataSource.deleteMessage(chatId, messageId, forAll)
        withContext(ioDispatcher) {
            messageDao.deleteMessage(messageId)
        }
        result.getOrThrow()
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
        
        withContext(ioDispatcher) {
            val userId = signatureService.getUserId() ?: "self"
            val reactionEntity = ReactionEntity(
                messageId = messageId,
                userId = userId,
                reaction = reaction
            )
            messageDao.insertReactions(listOf(reactionEntity))
        }
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
        
        withContext(ioDispatcher) {
            val userId = signatureService.getUserId() ?: "self"
            messageDao.deleteReaction(messageId, userId, reaction)
        }
    }

    /**
     * Pins a specific message in a chat.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message to pin.
     */
    override suspend fun pinMessage(chatId: String, messageId: String) {
        networkDataSource.pinMessage(chatId, messageId).getOrThrow()
        withContext(ioDispatcher) {
            val msgWithDetails = messageDao.getMessage(messageId)
            if (msgWithDetails != null && !msgWithDetails.message.isPinned) {
                val updatedMsg = msgWithDetails.message.copy(isPinned = true)
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
     * Unpins a previously pinned message in a chat.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message to unpin.
     */
    override suspend fun unpinMessage(chatId: String, messageId: String) {
        networkDataSource.unpinMessage(chatId, messageId).getOrThrow()
        withContext(ioDispatcher) {
            val msgWithDetails = messageDao.getMessage(messageId)
            if (msgWithDetails != null && msgWithDetails.message.isPinned) {
                val updatedMsg = msgWithDetails.message.copy(isPinned = false)
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
        // Optimistically zero out unread count locally so the chat list
        // shows the correct badge immediately when the user navigates back.
        withContext(ioDispatcher) {
            val chat = chatDao.getChat(chatId)
            if (chat != null && chat.unreadCount > 0) {
                chatDao.upsertChat(chat.copy(unreadCount = 0))
            }
        }
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
        val networkReaders = try {
            networkDataSource.getMessageReaders(chatId, messageId).getOrThrow()
        } catch (e: Exception) {
            emptyList()
        }

        withContext(ioDispatcher) {
            if (networkReaders.isNotEmpty()) {
                val users = networkReaders.map { 
                    ru.kubsu.borshchevyk.core.database.entity.UserEntity(
                        userId = it.id,
                        email = null,
                        tag = it.tag ?: "user_${it.id.take(4)}",
                        firstName = it.firstName,
                        lastName = it.lastName,
                        bio = null,
                        avatarUrl = it.avatarUrl,
                        avatars = emptyList()
                    )
                }
                // Save fetched users and map them as readers
                users.forEach { messageDao.insertUserIgnore(it) }
                networkReaders.forEach { user ->
                    messageDao.insertMessageReader(
                        ru.kubsu.borshchevyk.core.database.entity.MessageReaderEntity(messageId, user.id)
                    )
                }
            }
        }

        return withContext(ioDispatcher) {
            messageDao.getMessageReaders(messageId).map { 
                ru.kubsu.borshchevyk.core.model.domain.User(
                    userId = it.userId,
                    firstName = it.firstName,
                    lastName = it.lastName,
                    tag = it.tag,
                    avatarUrl = it.avatarUrl
                )
            }
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
    private suspend fun Message.saveToDb() {
        if (isDeleted) {
            messageDao.deleteMessage(id)
            return
        }
        val isNewMessage = messageDao.getMessage(id) == null

        messageDao.upsertMessageWithDetails(
            message = toMessageEntity(),
            author = toAuthorEntity(),
            forwardedFromUser = toForwardedUserEntity(),
            attachments = toAttachmentEntities(),
            reactions = toReactionEntities()
        )
        
        // Update the chat's last message to show in the chat list
        val chat = chatDao.getChat(chatId)
        if (chat != null) {
            val previewText = if (attachments.isNotEmpty() && text.isBlank()) "Attachment" else text
            
            val currentUserId = getUserIdUseCase().firstOrNull() ?: ""
            val isFromOtherUser = authorId != currentUserId
            
            var newUnreadCount = chat.unreadCount
            if (isNewMessage && isFromOtherUser && status != MessageStatus.READ) {
                newUnreadCount += 1
            }

            chatDao.upsertChat(chat.copy(
                lastMessage = previewText,
                unreadCount = newUnreadCount
            ))
        }
    }

    /**
     * Processes incoming synchronization events from the background SyncRepository.
     * Implements Last-Write-Wins (LWW) conflict resolution logic using timestamps.
     */
    private suspend fun processMessageEvent(event: ru.kubsu.borshchevyk.core.model.domain.SyncEvent) {
        withContext(ioDispatcher) {
            try {
                when (event.eventType) {
                    EventType.MESSAGE_CREATED, EventType.MESSAGE_UPDATED -> {
                        // Deserialize the backend payload into our local domain model
                        val incomingMessage = json.decodeFromString<Message>(event.payload)
                        
                        // LWW Conflict Resolution
                        val localMsgWithDetails = messageDao.getMessage(incomingMessage.id)
                        if (localMsgWithDetails != null) {
                            val localTime = localMsgWithDetails.message.updatedAt ?: localMsgWithDetails.message.createdAt
                            val remoteTime = incomingMessage.updatedAt ?: incomingMessage.createdAt
                            
                            // If local is strictly newer, ignore the remote event
                            if (localTime > remoteTime) {
                                return@withContext
                            }
                        }
                        
                        // Overwrite local state with incoming state
                        incomingMessage.saveToDb()
                    }
                    EventType.MESSAGE_DELETED -> {
                        messageDao.deleteMessage(event.entityId)
                    }
                    EventType.MESSAGE_READ -> {
                        // The payload contains chatId — reset unread count for this chat
                        try {
                            val root = json.parseToJsonElement(event.payload)
                            val chatId = if (root is kotlinx.serialization.json.JsonObject) {
                                root["chatId"]?.let { 
                                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
                                } ?: event.entityId
                            } else event.entityId
                            
                            val existingChat = chatDao.getChat(chatId)
                            if (existingChat != null) {
                                chatDao.upsertChat(existingChat.copy(unreadCount = 0))
                            }
                        } catch (e: Exception) {
                            Log.w("MessageRepository", "MESSAGE_READ: failed to process", e)
                        }
                    }
                    else -> {
                        // Ignore events from other domains
                    }
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Failed to process message sync event: ${event.id}", e)
            }
        }
    }
}
