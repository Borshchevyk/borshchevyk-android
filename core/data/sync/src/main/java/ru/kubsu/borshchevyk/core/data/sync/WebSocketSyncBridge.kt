package ru.kubsu.borshchevyk.core.data.sync

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import ru.kubsu.borshchevyk.core.model.domain.EventType
import ru.kubsu.borshchevyk.core.model.domain.VectorClock
import ru.kubsu.borshchevyk.core.network.di.ApplicationScope
import ru.kubsu.borshchevyk.core.network.websocket.ChatWebSocketDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge that forwards sync-relevant WebSocket events into the [SyncRepository]
 * for immediate processing by feature repositories via the sync pipeline.
 *
 * This eliminates the 15-minute delay of the periodic [SyncWorker] for events
 * like MEMBER_ADDED, MEMBER_REMOVED, MEMBER_UPDATED, and MESSAGE_READ.
 */
@Singleton
class WebSocketSyncBridge @Inject constructor(
    private val webSocketDataSource: ChatWebSocketDataSource,
    private val syncRepository: SyncRepository,
    private val json: kotlinx.serialization.json.Json,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val TAG = "WebSocketSyncBridge"

    /**
     * Set of event types that should be forwarded from WebSocket to the sync pipeline.
     * These are events that are NOT already handled by direct WebSocket observers
     * in feature repositories (e.g., MEMBER_* events have no dedicated observer).
     */
    private val syncRelevantTypes = setOf(
        "MEMBER_ADDED", "MEMBER_REMOVED", "MEMBER_UPDATED",
        "MESSAGE_READ",
        "CONTACT_ADDED", "CONTACT_REMOVED", "CONTACT_UPDATED",
        "PRIVACY_SETTINGS_UPDATED"
    )

    private val eventTypeMapping = mapOf(
        "MEMBER_ADDED" to EventType.MEMBER_ADDED,
        "MEMBER_REMOVED" to EventType.MEMBER_REMOVED,
        "MEMBER_UPDATED" to EventType.MEMBER_UPDATED,
        "MESSAGE_READ" to EventType.MESSAGE_READ,
        "CONTACT_ADDED" to EventType.CONTACT_ADDED,
        "CONTACT_REMOVED" to EventType.CONTACT_REMOVED,
        "CONTACT_UPDATED" to EventType.CONTACT_UPDATED,
        "PRIVACY_SETTINGS_UPDATED" to EventType.PRIVACY_SETTINGS_UPDATED
    )

    fun startListening() {
        webSocketDataSource.observeAllEvents()
            .filter { it.eventType in syncRelevantTypes }
            .onEach { appEvent ->
                val eventType = eventTypeMapping[appEvent.eventType] ?: return@onEach
                val payloadString = appEvent.payload.toString()

                // Extract entityId from payload if available
                val entityId = if (appEvent.payload is JsonObject) {
                    val obj = appEvent.payload as JsonObject
                    (obj["chatId"] as? JsonPrimitive)?.content
                        ?: (obj["entityId"] as? JsonPrimitive)?.content
                        ?: ""
                } else ""

                val vectorClock = if (appEvent.payload is JsonObject) {
                    val obj = appEvent.payload as JsonObject
                    obj["vectorClock"]?.let {
                        try {
                            json.decodeFromJsonElement<VectorClock>(it)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to decode vector clock from payload", e)
                            null
                        }
                    }
                } else null

                Log.d(TAG, "Forwarding WebSocket event to sync: ${appEvent.eventType}")
                syncRepository.emitRealtimeEvent(entityId, eventType, payloadString, vectorClock)
            }
            .catch { e -> Log.w(TAG, "Error in WebSocketSyncBridge", e) }
            .launchIn(scope)
    }
}
