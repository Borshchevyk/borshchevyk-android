package ru.kubsu.borshchevyk.core.network.websocket

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.dto.NotificationDto
import ru.kubsu.borshchevyk.core.model.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.model.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.dto.TypingEvent

interface WebSocketDataSource {
    suspend fun connect()
    suspend fun disconnect()
    
    fun observeNewMessages(): Flow<NotificationDto.MessageDto>
    fun observeChatEvents(): Flow<NotificationDto.ChatEventDto>
    fun observeCallEvents(): Flow<NotificationDto.CallEventDto>
    fun observeDeletedMessages(): Flow<String>
    fun observeTyping(chatId: String): Flow<TypingEvent>
    fun observeReactions(chatId: String): Flow<ReactionEvent>
    fun observePins(chatId: String): Flow<String>
    fun observeUnpins(chatId: String): Flow<String>
    fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent>
    fun observePresence(userId: String): Flow<ru.kubsu.borshchevyk.core.model.dto.PresenceStatusResponse>
    
    suspend fun sendTypingEvent(chatId: String, isTyping: Boolean)
}
