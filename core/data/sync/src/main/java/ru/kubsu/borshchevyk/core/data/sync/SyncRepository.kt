package ru.kubsu.borshchevyk.core.data.sync

import kotlinx.coroutines.flow.SharedFlow
import ru.kubsu.borshchevyk.core.model.domain.EventType
import ru.kubsu.borshchevyk.core.model.domain.SyncEvent
import ru.kubsu.borshchevyk.core.model.domain.VectorClock

/**
 * Repository interface for managing synchronization of domain models
 * with the remote backend using Vector Clocks.
 */
interface SyncRepository {
    /**
     * Flow of incoming synchronization events from the server.
     * Feature modules should subscribe to this flow to apply remote mutations locally.
     */
    val incomingEvents: SharedFlow<SyncEvent>

    /**
     * Enqueues an event for synchronization by creating an outbox entry.
     */
    suspend fun enqueueEvent(entityId: String, eventType: EventType, payload: String)
    
    /**
     * Pushes all locally queued events to the server.
     * @return true if successful, false otherwise.
     */
    suspend fun push(): Boolean
    
    /**
     * Pulls new events from the server and applies them locally.
     * @return true if successful, false otherwise.
     */
    suspend fun pull(): Boolean

    /**
     * Emits a real-time event received via WebSocket into the sync processing pipeline.
     * This allows feature repositories to process real-time events using the same
     * sync event handlers they use for pulled events.
     */
    suspend fun emitRealtimeEvent(entityId: String, eventType: EventType, payload: String, vectorClock: VectorClock? = null)
}
