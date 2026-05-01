package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainGlobalChatEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainPresenceStatus
import ru.kubsu.borshchevyk.core.model.domain.DomainReactionEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainTypingEvent
import ru.kubsu.borshchevyk.core.model.domain.Message

/**
 * Repository interface for managing messaging operations.
 *
 * This interface defines the primary contract for interacting with messages,
 * real-time chat events, and chat history. It abstracts away the data sources
 * (e.g., local database, network API, WebSocket) to provide a unified API
 * for the domain layer.
 */
interface MessageRepository {
    /**
     * Sends a new message to a specific chat.
     *
     * @param chatId The unique identifier of the destination chat.
     * @param text The text content of the message.
     * @param attachmentIds An optional list of previously uploaded attachment IDs.
     * @param forwardedFromChatId The ID of the chat if this message is forwarded.
     * @param forwardedFromUserId The ID of the original sender if this message is forwarded.
     */
    suspend fun sendMessage(
        chatId: String, 
        text: String, 
        attachmentIds: List<String>?, 
        forwardedFromChatId: String?, 
        forwardedFromUserId: String?
    )

    /**
     * Edits the text content of an existing message.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message to edit.
     * @param newText The updated text content.
     */
    suspend fun editMessage(chatId: String, messageId: String, newText: String)

    /**
     * Observes the history of messages for a specific chat.
     *
     * @param chatId The unique identifier of the chat.
     * @return A reactive [Flow] emitting the list of messages in the chat.
     */
    fun observeChatHistory(chatId: String): Flow<List<Message>>

    /**
     * Requests synchronization of chat history from the remote server.
     *
     * @param chatId The unique identifier of the chat to sync.
     * @param page The pagination page index to retrieve.
     * @param size The number of messages per page.
     */
    suspend fun syncChatHistory(chatId: String, page: Int = 0, size: Int = 50)

    /**
     * Establishes a WebSocket connection for real-time messaging and events.
     */
    suspend fun connectWebSocket()

    /**
     * Terminates the active WebSocket connection.
     */
    suspend fun disconnectWebSocket()

    /**
     * Observes newly incoming messages across all chats.
     *
     * @return A [Flow] emitting newly received [Message] objects.
     */
    fun observeNewMessages(): Flow<Message>

    /**
     * Observes global chat events (e.g., system announcements, global state changes).
     *
     * @return A [Flow] emitting [DomainGlobalChatEvent].
     */
    fun observeChatEvents(): Flow<DomainGlobalChatEvent>

    /**
     * Observes notifications of message deletions.
     *
     * @return A [Flow] emitting the IDs of deleted messages.
     */
    fun observeDeletedMessages(): Flow<String>

    /**
     * Observes typing events within a specific chat.
     *
     * @param chatId The unique identifier of the chat.
     * @return A [Flow] emitting [DomainTypingEvent].
     */
    fun observeTyping(chatId: String): Flow<DomainTypingEvent>

    /**
     * Observes reaction updates (added or removed) within a specific chat.
     *
     * @param chatId The unique identifier of the chat.
     * @return A [Flow] emitting [DomainReactionEvent].
     */
    fun observeReactions(chatId: String): Flow<DomainReactionEvent>

    /**
     * Observes events when messages are pinned in a specific chat.
     *
     * @param chatId The unique identifier of the chat.
     * @return A [Flow] emitting the ID of the pinned message.
     */
    fun observePins(chatId: String): Flow<String>

    /**
     * Observes events when messages are unpinned in a specific chat.
     *
     * @param chatId The unique identifier of the chat.
     * @return A [Flow] emitting the ID of the unpinned message.
     */
    fun observeUnpins(chatId: String): Flow<String>

    /**
     * Observes read receipt events within a specific chat.
     *
     * @param chatId The unique identifier of the chat.
     * @return A [Flow] emitting [DomainReadReceiptEvent].
     */
    fun observeReadReceipts(chatId: String): Flow<DomainReadReceiptEvent>

    /**
     * Observes the online presence status of a specific user.
     *
     * @param userId The unique identifier of the user.
     * @return A [Flow] emitting the [DomainPresenceStatus] of the user.
     */
    fun observePresence(userId: String): Flow<DomainPresenceStatus>
    
    /**
     * Broadcasts a typing event to other participants in a chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param isTyping True if the user has started typing, false if they stopped.
     */
    suspend fun sendTypingEvent(chatId: String, isTyping: Boolean)

    /**
     * Deletes a specific message.
     *
     * @param chatId The unique identifier of the chat.
     * @param messageId The unique identifier of the message to delete.
     * @param forAll If true, attempts to delete the message for everyone in the chat.
     */
    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean)
    
    /**
     * Adds a reaction to a specific message.
     *
     * @param chatId The unique identifier of the chat.
     * @param messageId The unique identifier of the message.
     * @param reaction The string representation of the reaction.
     */
    suspend fun addReaction(chatId: String, messageId: String, reaction: String)

    /**
     * Removes an existing reaction from a specific message.
     *
     * @param chatId The unique identifier of the chat.
     * @param messageId The unique identifier of the message.
     * @param reaction The string representation of the reaction to remove.
     */
    suspend fun removeReaction(chatId: String, messageId: String, reaction: String)

    /**
     * Pins a specific message in a chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param messageId The unique identifier of the message to pin.
     */
    suspend fun pinMessage(chatId: String, messageId: String)

    /**
     * Unpins a previously pinned message in a chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param messageId The unique identifier of the message to unpin.
     */
    suspend fun unpinMessage(chatId: String, messageId: String)
    
    /**
     * Retrieves all pinned messages for a specific chat.
     *
     * @param chatId The unique identifier of the chat.
     * @return A list of pinned [Message] objects.
     */
    suspend fun getPinnedMessages(chatId: String): List<Message>

    /**
     * Marks a specific message as read.
     *
     * @param chatId The unique identifier of the chat.
     * @param messageId The unique identifier of the message.
     */
    suspend fun readMessage(chatId: String, messageId: String)

    /**
     * Retrieves the list of users who have read a specific message.
     *
     * @param chatId The unique identifier of the chat.
     * @param messageId The unique identifier of the message.
     * @return A list of users representing the readers.
     */
    suspend fun getMessageReaders(chatId: String, messageId: String): List<ru.kubsu.borshchevyk.core.model.domain.User>

    /**
     * Retrieves a paginated list of comments (replies) for a specific message.
     *
     * @param chatId The unique identifier of the chat.
     * @param messageId The unique identifier of the root message.
     * @param page The pagination page index.
     * @param size The number of comments per page.
     * @return A list of comment [Message] objects.
     */
    suspend fun getMessageComments(chatId: String, messageId: String, page: Int, size: Int): List<Message>
}
