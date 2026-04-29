package ru.kubsu.borshchevyk.core.network.websocket

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.PresenceStatusResponse
import ru.kubsu.borshchevyk.core.network.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.network.dto.TypingEvent

interface WebSocketConnectionManager {
    suspend fun connect()
    suspend fun disconnect()
}

interface ChatWebSocketDataSource : WebSocketConnectionManager {
    fun observeNewMessages(): Flow<NotificationDto.MessageDto>
    fun observeChatEvents(): Flow<NotificationDto.ChatEventDto>
    fun observeDeletedMessages(): Flow<String>
    fun observeTyping(chatId: String): Flow<TypingEvent>
    fun observeReactions(chatId: String): Flow<ReactionEvent>
    fun observePins(chatId: String): Flow<String>
    fun observeUnpins(chatId: String): Flow<String>
    fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent>
    suspend fun sendTypingEvent(chatId: String, isTyping: Boolean)
}

interface CallWebSocketDataSource : WebSocketConnectionManager {
    fun observeCallEvents(): Flow<NotificationDto.CallEventDto>
}

interface PresenceWebSocketDataSource : WebSocketConnectionManager {
    fun observePresence(userId: String): Flow<PresenceStatusResponse>
}

// Keep a combined interface for backward compatibility during the transition,
// or for the implementation class to inherit from.
interface WebSocketDataSource : ChatWebSocketDataSource, CallWebSocketDataSource, PresenceWebSocketDataSource
