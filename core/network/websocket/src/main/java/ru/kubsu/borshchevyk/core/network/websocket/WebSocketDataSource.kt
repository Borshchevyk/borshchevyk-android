package ru.kubsu.borshchevyk.core.network.websocket

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.PresenceStatusResponse
import ru.kubsu.borshchevyk.core.network.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.network.dto.TypingEvent

/**
 * Core interface for managing the underlying WebSocket connection.
 */
interface WebSocketConnectionManager {
    /**
     * Establishes the WebSocket connection.
     */
    suspend fun connect()

    /**
     * Gracefully closes the WebSocket connection.
     */
    suspend fun disconnect()
}

/**
 * Data source interface defining operations for observing real-time chat events via WebSockets.
 */
interface ChatWebSocketDataSource : WebSocketConnectionManager {
    /**
     * Observes incoming new messages across all chats.
     * @return A [Flow] emitting newly received [NotificationDto.MessageDto].
     */
    fun observeNewMessages(): Flow<NotificationDto.MessageDto>

    /**
     * Observes overarching chat events (e.g., chat created, member joined).
     * @return A [Flow] emitting [NotificationDto.ChatEventDto].
     */
    fun observeChatEvents(): Flow<NotificationDto.ChatEventDto>

    /**
     * Observes message deletion events.
     * @return A [Flow] emitting the IDs of deleted messages.
     */
    fun observeDeletedMessages(): Flow<String>

    /**
     * Observes typing indicators within a specific chat.
     * @param chatId The ID of the chat to monitor.
     * @return A [Flow] emitting [TypingEvent] updates.
     */
    fun observeTyping(chatId: String): Flow<TypingEvent>

    /**
     * Observes reaction changes within a specific chat.
     * @param chatId The ID of the chat to monitor.
     * @return A [Flow] emitting [ReactionEvent] updates.
     */
    fun observeReactions(chatId: String): Flow<ReactionEvent>

    /**
     * Observes message pin events within a specific chat.
     * @param chatId The ID of the chat to monitor.
     * @return A [Flow] emitting the IDs of pinned messages.
     */
    fun observePins(chatId: String): Flow<String>

    /**
     * Observes message unpin events within a specific chat.
     * @param chatId The ID of the chat to monitor.
     * @return A [Flow] emitting the IDs of unpinned messages.
     */
    fun observeUnpins(chatId: String): Flow<String>

    /**
     * Observes read receipt updates within a specific chat.
     * @param chatId The ID of the chat to monitor.
     * @return A [Flow] emitting [ReadReceiptEvent] updates.
     */
    fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent>

    /**
     * Broadcasts the current user's typing status to a specific chat.
     * @param chatId The ID of the target chat.
     * @param isTyping True if the user is typing, false if they stopped.
     */
    suspend fun sendTypingEvent(chatId: String, isTyping: Boolean)
}

/**
 * Data source interface defining operations for observing real-time call events via WebSockets.
 */
interface CallWebSocketDataSource : WebSocketConnectionManager {
    /**
     * Observes overarching call events (e.g., call initiated, call ended).
     * @return A [Flow] emitting [NotificationDto.CallEventDto].
     */
    fun observeCallEvents(): Flow<NotificationDto.CallEventDto>
}

/**
 * Data source interface defining operations for observing user presence statuses via WebSockets.
 */
interface PresenceWebSocketDataSource : WebSocketConnectionManager {
    /**
     * Observes the online presence status of a specific user.
     * @param userId The ID of the user to monitor.
     * @return A [Flow] emitting [PresenceStatusResponse] updates.
     */
    fun observePresence(userId: String): Flow<PresenceStatusResponse>
}

/**
 * An aggregate interface combining all real-time WebSocket data sources.
 * Maintained for backward compatibility and simplified dependency injection.
 */
interface WebSocketDataSource : ChatWebSocketDataSource, CallWebSocketDataSource, PresenceWebSocketDataSource
