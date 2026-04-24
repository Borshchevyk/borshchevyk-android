package ru.kubsu.borshchevyk.core.network.websocket

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import ru.kubsu.borshchevyk.core.model.dto.NotificationDto
import ru.kubsu.borshchevyk.core.model.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.model.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.dto.TypingEvent
import ru.kubsu.borshchevyk.core.network.client.NetworkConstants
import ru.kubsu.borshchevyk.core.network.client.TokenProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KrossbowWebSocketDataSource @Inject constructor(
    private val json: Json,
    private val tokenProvider: TokenProvider
) : WebSocketDataSource {

    private val TAG = "WebSocketDataSource"

    private val httpClient = HttpClient {
        install(WebSockets)
    }

    private val stompClient = StompClient(KtorWebSocketClient(httpClient))
    private val _session = MutableStateFlow<StompSession?>(null)

    override suspend fun connect() {
        if (_session.value != null) {
            Log.d(TAG, "Connect requested, but session already exists.")
            return
        }
        val token = tokenProvider.getAccessToken() ?: run {
            Log.e(TAG, "Cannot connect to WebSocket: Access token is null.")
            return
        }
        Log.d(TAG, "Connecting to WebSocket at ${NetworkConstants.WS_URL}...")
        try {
            val s = stompClient.connect(
                url = NetworkConstants.WS_URL,
                customStompConnectHeaders = mapOf("Authorization" to "Bearer $token")
            )
            _session.value = s
            Log.i(TAG, "Successfully connected to STOMP WebSocket.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to STOMP WebSocket", e)
        }
    }

    override suspend fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket.")
        try {
            _session.value?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting WebSocket", e)
        } finally {
            _session.value = null
        }
    }

    private fun <T> observeTopic(destination: String, mapper: (String) -> T): Flow<T> {
        return _session
            .filterNotNull()
            .flatMapLatest { s ->
                Log.d(TAG, "Subscribing to $destination")
                s.subscribeText(destination)
                    .map { msg ->
                        Log.d(TAG, "WS Received on $destination: $msg")
                        mapper(msg)
                    }
                    .catch { e ->
                        Log.e(TAG, "WS Subscription error on $destination", e)
                    }
            }
            .catch { e ->
                Log.e(TAG, "WS Flow error on $destination", e)
            }
    }

    override fun observeNewMessages(): Flow<NotificationDto.MessageDto> = 
        observeTopic("/user/queue/messages") { json.decodeFromString<NotificationDto.MessageDto>(it) }

    override fun observeDeletedMessages(): Flow<String> = 
        observeTopic("/user/queue/messages/deleted") { it.replace("\"", "").trim() }

    override fun observeTyping(chatId: String): Flow<TypingEvent> = 
        observeTopic("/topic/chat/$chatId/typing") { json.decodeFromString<TypingEvent>(it) }

    override fun observeReactions(chatId: String): Flow<ReactionEvent> = 
        observeTopic("/topic/chat/$chatId/reactions") { json.decodeFromString<ReactionEvent>(it) }

    override fun observePins(chatId: String): Flow<String> = 
        observeTopic("/topic/chat/$chatId/pin") { it.replace("\"", "").trim() }

    override fun observeUnpins(chatId: String): Flow<String> = 
        observeTopic("/topic/chat/$chatId/unpin") { it.replace("\"", "").trim() }

    override fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent> = 
        observeTopic("/topic/chat/$chatId/read") { json.decodeFromString<ReadReceiptEvent>(it) }

    override suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        try {
            val payload = if (isTyping) "true" else "false"
            _session.value?.sendText("/app/chat/$chatId/typing", payload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send typing event", e)
        }
    }
}
