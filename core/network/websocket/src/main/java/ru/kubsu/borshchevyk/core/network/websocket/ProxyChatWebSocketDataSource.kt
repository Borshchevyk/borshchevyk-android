@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package ru.kubsu.borshchevyk.core.network.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.network.dto.TypingEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyChatWebSocketDataSource @Inject constructor(
    private val krossbowWebSocket: KrossbowWebSocketDataSource,
    private val meshWebSocket: MeshChatWebSocketDataSource,
    private val transportModeManager: TransportModeManager
) : ChatWebSocketDataSource {

    private val currentDataSource: ChatWebSocketDataSource
        get() = if (transportModeManager.networkMode.value == NetworkMode.MESH) {
            meshWebSocket
        } else {
            krossbowWebSocket
        }

    override fun observeNewMessages(): Flow<NotificationDto.MessageDto> {
        return transportModeManager.networkMode.flatMapLatest { mode ->
            if (mode == NetworkMode.MESH) meshWebSocket.observeNewMessages()
            else krossbowWebSocket.observeNewMessages()
        }
    }

    override fun observeChatEvents(): Flow<NotificationDto.ChatEventDto> {
        return transportModeManager.networkMode.flatMapLatest { mode ->
            if (mode == NetworkMode.MESH) meshWebSocket.observeChatEvents()
            else krossbowWebSocket.observeChatEvents()
        }
    }

    override fun observeDeletedMessages(): Flow<String> {
        return transportModeManager.networkMode.flatMapLatest { mode ->
            if (mode == NetworkMode.MESH) meshWebSocket.observeDeletedMessages()
            else krossbowWebSocket.observeDeletedMessages()
        }
    }

    override fun observeTyping(chatId: String): Flow<TypingEvent> {
        return transportModeManager.networkMode.flatMapLatest { mode ->
            if (mode == NetworkMode.MESH) meshWebSocket.observeTyping(chatId)
            else krossbowWebSocket.observeTyping(chatId)
        }
    }

    override fun observeReactions(chatId: String): Flow<ReactionEvent> {
        return transportModeManager.networkMode.flatMapLatest { mode ->
            if (mode == NetworkMode.MESH) meshWebSocket.observeReactions(chatId)
            else krossbowWebSocket.observeReactions(chatId)
        }
    }

    override fun observePins(chatId: String): Flow<String> {
        return transportModeManager.networkMode.flatMapLatest { mode ->
            if (mode == NetworkMode.MESH) meshWebSocket.observePins(chatId)
            else krossbowWebSocket.observePins(chatId)
        }
    }

    override fun observeUnpins(chatId: String): Flow<String> {
        return transportModeManager.networkMode.flatMapLatest { mode ->
            if (mode == NetworkMode.MESH) meshWebSocket.observeUnpins(chatId)
            else krossbowWebSocket.observeUnpins(chatId)
        }
    }

    override fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent> {
        return transportModeManager.networkMode.flatMapLatest { mode ->
            if (mode == NetworkMode.MESH) meshWebSocket.observeReadReceipts(chatId)
            else krossbowWebSocket.observeReadReceipts(chatId)
        }
    }

    override suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        currentDataSource.sendTypingEvent(chatId, isTyping)
    }

    override suspend fun connect() {
        currentDataSource.connect()
    }

    override suspend fun disconnect() {
        currentDataSource.disconnect()
    }
}
