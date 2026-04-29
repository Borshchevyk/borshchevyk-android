package ru.kubsu.borshchevyk.core.network.websocket

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.PresenceStatusResponse
import ru.kubsu.borshchevyk.core.network.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.network.dto.TypingEvent
import ru.kubsu.borshchevyk.core.network.client.NetworkConstants
import ru.kubsu.borshchevyk.core.network.client.TokenProvider
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionJob: Job? = null
    private var isIntentionalDisconnect = false

    private val sharedFlows = ConcurrentHashMap<String, Flow<String>>()

    override suspend fun connect() {
        if (connectionJob?.isActive == true) {
            Log.d(TAG, "Connect requested, but reconnect loop is already active.")
            return
        }
        isIntentionalDisconnect = false

        connectionJob = scope.launch {
            var attempt = 0
            while (isActive && !isIntentionalDisconnect) {
                val token = tokenProvider.getAccessToken()
                if (token == null) {
                    Log.e(TAG, "Cannot connect to WebSocket: Access token is null. Retrying in 5s...")
                    delay(5000)
                    continue
                }

                try {
                    Log.d(TAG, "Connecting to STOMP WebSocket (attempt ${attempt + 1})...")
                    val session = stompClient.connect(
                        url = NetworkConstants.WS_URL,
                        customStompConnectHeaders = mapOf("Authorization" to "Bearer $token")
                    )
                    _session.value = session
                    Log.i(TAG, "Successfully connected to STOMP WebSocket.")
                    attempt = 0 // reset on success

                    try {
                        // Collect a subscription to block the coroutine until the session closes or fails
                        session.subscribeText("/user/queue/errors").collect {
                            Log.e(TAG, "Received STOMP error from server: $it")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "STOMP Session ended/failed", e)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to STOMP WebSocket", e)
                }

                _session.value = null

                if (isIntentionalDisconnect) {
                    Log.d(TAG, "Intentional disconnect, stopping reconnect loop.")
                    break
                }

                attempt++
                val backoff = min(30000L, 1000L * (1L shl min(attempt, 5)))
                Log.d(TAG, "Reconnecting in $backoff ms...")
                delay(backoff)
            }
        }
    }

    override suspend fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket.")
        isIntentionalDisconnect = true
        connectionJob?.cancel()
        connectionJob = null
        try {
            _session.value?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting WebSocket", e)
        } finally {
            _session.value = null
            sharedFlows.clear()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun getSharedTopicFlow(destination: String): Flow<String> {
        return sharedFlows.getOrPut(destination) {
            _session
                .filterNotNull()
                .flatMapLatest { s ->
                    Log.d(TAG, "Subscribing to $destination")
                    s.subscribeText(destination)
                        .catch { e ->
                            Log.e(TAG, "WS Subscription error on $destination", e)
                        }
                }
                .shareIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                    replay = 1
                )
        }
    }

    private inline fun <reified T> observeTopic(destination: String): Flow<T> {
        return getSharedTopicFlow(destination)
            .map { msg ->
                Log.d(TAG, "WS Received on $destination: $msg")
                json.decodeFromString<T>(msg)
            }
            .catch { e ->
                Log.e(TAG, "WS Mapping error on $destination", e)
            }
    }

    private fun observeTopicString(destination: String): Flow<String> {
        return getSharedTopicFlow(destination)
            .map { msg ->
                Log.d(TAG, "WS Received on $destination: $msg")
                msg.replace("\"", "").trim()
            }
            .catch { e ->
                Log.e(TAG, "WS Mapping error on $destination", e)
            }
    }

    override fun observeNewMessages(): Flow<NotificationDto.MessageDto> = observeTopic("/user/queue/messages")
    override fun observeChatEvents(): Flow<NotificationDto.ChatEventDto> = observeTopic("/user/queue/chats")
    override fun observeCallEvents(): Flow<NotificationDto.CallEventDto> = observeTopic("/user/queue/calls")
    override fun observeDeletedMessages(): Flow<String> = observeTopicString("/user/queue/messages/deleted")
    override fun observeTyping(chatId: String): Flow<TypingEvent> = observeTopic("/topic/chat/$chatId/typing")
    override fun observeReactions(chatId: String): Flow<ReactionEvent> = observeTopic("/topic/chat/$chatId/reactions")
    override fun observePins(chatId: String): Flow<String> = observeTopicString("/topic/chat/$chatId/pin")
    override fun observeUnpins(chatId: String): Flow<String> = observeTopicString("/topic/chat/$chatId/unpin")
    override fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent> = observeTopic("/topic/chat/$chatId/read")
    
    override fun observePresence(userId: String): Flow<PresenceStatusResponse> = 
        kotlinx.coroutines.flow.merge(
            observeTopic("/app/user/$userId/presence"),
            observeTopic("/topic/user/$userId/presence")
        )

    override suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        try {
            val payload = if (isTyping) "true" else "false"
            _session.value?.sendText("/app/chat/$chatId/typing", payload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send typing event", e)
        }
    }
}
