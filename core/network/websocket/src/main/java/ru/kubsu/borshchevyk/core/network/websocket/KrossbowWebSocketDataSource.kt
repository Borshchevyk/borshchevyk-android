package ru.kubsu.borshchevyk.core.network.websocket

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.PresenceStatusResponse
import ru.kubsu.borshchevyk.core.network.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.network.dto.TypingEvent
import ru.kubsu.borshchevyk.core.network.client.NetworkConstants
import ru.kubsu.borshchevyk.core.network.client.NetworkMonitor
import ru.kubsu.borshchevyk.core.network.client.TokenProvider
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

@Singleton
class KrossbowWebSocketDataSource @Inject constructor(
    private val json: Json,
    private val tokenProvider: TokenProvider,
    private val networkMonitor: NetworkMonitor
) : WebSocketDataSource {

    private val TAG = "WebSocketDataSource"

    private val httpClient = HttpClient {
        install(WebSockets) {
            pingInterval = 15.seconds
        }
    }

    private val stompClient = StompClient(KtorWebSocketClient(httpClient)) {
        heartBeat = HeartBeat(10.seconds, 10.seconds)
    }
    private val _session = MutableStateFlow<StompSession?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionJob: Job? = null
    private val _shouldBeConnected = MutableStateFlow(false)

    private val sharedFlows = ConcurrentHashMap<String, Flow<String>>()

    override suspend fun connect() {
        _shouldBeConnected.value = true
        if (connectionJob?.isActive == true) {
            Log.d(TAG, "Connect requested, but reconnect loop is already active.")
            return
        }

        connectionJob = scope.launch {
            combine(networkMonitor.isOnline, _shouldBeConnected) { isOnline, shouldReconnect ->
                isOnline && shouldReconnect
            }.collectLatest { canConnect ->
                if (canConnect) {
                    var attempt = 0
                    while (isActive && _shouldBeConnected.value) {
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
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                val msg = e.message ?: ""
                                if (e is java.net.SocketException || e.cause is java.net.SocketException || msg.contains("Connection abort")) {
                                    Log.d(TAG, "STOMP Session ended normally or connection aborted: $msg")
                                } else {
                                    Log.w(TAG, "STOMP Session ended/failed", e)
                                }
                            }

                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            val msg = e.message ?: ""
                            if (e is java.net.SocketException || e.cause is java.net.SocketException || msg.contains("Connection abort") || msg.contains("Connection refused")) {
                                Log.d(TAG, "Failed to connect to STOMP WebSocket (Network error): $msg")
                            } else {
                                Log.w(TAG, "Failed to connect to STOMP WebSocket", e)
                            }
                        }

                        _session.value = null

                        if (!_shouldBeConnected.value) {
                            Log.d(TAG, "Intentional disconnect, stopping reconnect loop.")
                            break
                        }

                        attempt++
                        val backoff = min(30000L, 1000L * (1L shl min(attempt, 5)))
                        Log.d(TAG, "Reconnecting in $backoff ms...")
                        delay(backoff)
                    }
                } else {
                    Log.d(TAG, "Network offline or disconnected, closing session.")
                    _session.value?.disconnect()
                    _session.value = null
                }
            }
        }
    }

    override suspend fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket.")
        _shouldBeConnected.value = false
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
                            val msg = e.message ?: ""
                            if (e is java.net.SocketException || e.cause is java.net.SocketException || msg.contains("Connection abort")) {
                                Log.d(TAG, "WS Subscription ended/aborted on $destination: $msg")
                            } else {
                                Log.w(TAG, "WS Subscription error on $destination", e)
                            }
                        }
                }
                .shareIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                    replay = 1
                )
        }
    }

    private val appEvents: Flow<ru.kubsu.borshchevyk.core.network.dto.AppEventDto> by lazy {
        getSharedTopicFlow("/user/queue/events")
            .mapNotNull { msg ->
                try {
                    Log.d(TAG, "WS Received on /user/queue/events: $msg")
                    json.decodeFromString<ru.kubsu.borshchevyk.core.network.dto.AppEventDto>(msg)
                } catch (e: Exception) {
                    Log.w(TAG, "WS Mapping error on /user/queue/events: msg=$msg", e)
                    null
                }
            }
            .shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private inline fun <reified T> observeEvent(vararg types: String): Flow<T> {
        return appEvents
            .filter { it.eventType in types }
            .mapNotNull { 
                try {
                    json.decodeFromJsonElement<T>(it.payload)
                } catch (e: Exception) {
                    Log.w(TAG, "Error decoding event of type ${T::class.simpleName}: payload=${it.payload}", e)
                    null
                }
            }
    }

    private fun observeEventString(vararg types: String): Flow<String> {
        return appEvents
            .filter { it.eventType in types }
            .mapNotNull { event ->
                try {
                    val payload = event.payload
                    if (payload is JsonPrimitive && payload.isString) {
                        payload.content
                    } else {
                        payload.toString().replace("\"", "")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error decoding string event: payload=${event.payload}", e)
                    null
                }
            }
    }

    override fun observeNewMessages(): Flow<NotificationDto.MessageDto> = observeEvent("MESSAGE_CREATED")
    
    override fun observeChatEvents(): Flow<NotificationDto.ChatEventDto> = observeEvent("CHAT_EVENT", "CHAT_INFO_UPDATED", "CHAT_SETTINGS_UPDATED")
    
    override fun observeCallEvents(): Flow<NotificationDto.CallEventDto> = observeEvent("CALL_EVENT")
    
    override fun observeDeletedMessages(): Flow<String> = appEvents
        .filter { it.eventType == "MESSAGE_DELETED" }
        .mapNotNull { event ->
            try {
                val payload = event.payload
                if (payload is JsonPrimitive && payload.isString) {
                    payload.content
                } else {
                    json.decodeFromJsonElement<NotificationDto.MessageDto>(payload).id
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error decoding MESSAGE_DELETED: payload=${event.payload}", e)
                null
            }
        }
        .catch { e -> Log.w(TAG, "Error decoding MESSAGE_DELETED", e) }

    override fun observeTyping(chatId: String): Flow<TypingEvent> = 
        observeEvent<TypingEvent>("TYPING").filter { it.chatId == chatId }
        
    override fun observeReactions(chatId: String): Flow<ReactionEvent> = 
        observeEvent<ReactionEvent>("REACTION_ADDED", "REACTION_REMOVED").filter { it.chatId == chatId }
        
    override fun observePins(chatId: String): Flow<String> = 
        observeEventString("MESSAGE_PINNED") // Should filter by chatId, but backend doesn't send chatId for pins currently
        
    override fun observeUnpins(chatId: String): Flow<String> = 
        observeEventString("MESSAGE_UNPINNED")
        
    override fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent> = 
        observeEvent<ReadReceiptEvent>("MESSAGE_READ").filter { it.chatId == chatId }
    
    override fun observePresence(userId: String): Flow<PresenceStatusResponse> = 
        observeEvent<PresenceStatusResponse>("PRESENCE_UPDATE").filter { it.userId == userId }

    override suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        try {
            val payload = if (isTyping) "true" else "false"
            _session.value?.sendText("/app/chat/$chatId/typing", payload)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Failed to send typing event", e)
        }
    }

    override fun observeAllEvents(): Flow<ru.kubsu.borshchevyk.core.network.dto.AppEventDto> = appEvents
}
