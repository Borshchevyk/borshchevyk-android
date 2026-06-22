package ru.kubsu.borshchevyk.core.data.sync

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.SyncDao
import ru.kubsu.borshchevyk.core.model.domain.EventType
import ru.kubsu.borshchevyk.core.model.domain.SyncEvent
import ru.kubsu.borshchevyk.core.model.domain.VectorClock
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.toDomain
import ru.kubsu.borshchevyk.core.network.dto.toDto
import ru.kubsu.borshchevyk.core.network.sync.SyncNetworkDataSource
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncDao: SyncDao,
    private val syncNetworkDataSource: SyncNetworkDataSource,
    private val syncPreferences: SyncPreferences,
    private val webSocketSyncBridge: dagger.Lazy<WebSocketSyncBridge>
) : SyncRepository {
    
    // Removed hardcoded nodeId

    private val _incomingEvents = MutableSharedFlow<SyncEvent>(replay = 0, extraBufferCapacity = 100)
    override val incomingEvents: SharedFlow<SyncEvent> = _incomingEvents
    private val clockMutex = Mutex()

    init {
        // Start forwarding real-time WebSocket events to the sync pipeline
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.Default) {
            webSocketSyncBridge.get().startListening()
        }
    }

    override suspend fun enqueueEvent(entityId: String, eventType: EventType, payload: String) {
        withContext(Dispatchers.IO) {
            clockMutex.withLock {
                val currentClock = syncPreferences.getVectorClock()
                val nodeId = syncPreferences.getNodeId()
                val newClock = currentClock.increment(nodeId)
                syncPreferences.saveVectorClock(newClock)
                
                val event = SyncEvent(
                    id = UUID.randomUUID().toString(),
                    entityId = entityId,
                    eventType = eventType,
                    payload = payload,
                    vectorClock = newClock,
                    timestamp = Instant.now().toString()
                )
                syncDao.insertSyncEvent(event.toEntity())
            }
        }
    }

    override suspend fun push(): Boolean {
        return withContext(Dispatchers.IO) {
            val outboxEvents = syncDao.getUnsyncedEvents()
            if (outboxEvents.isEmpty()) return@withContext true
            
            val dtos = outboxEvents.map { it.toDomain().toDto() }
            val result = syncNetworkDataSource.pushEvents(dtos)
            
            if (result is NetworkResult.Success) {
                val serverClock = result.data
                clockMutex.withLock {
                    val localClock = syncPreferences.getVectorClock()
                    syncPreferences.saveVectorClock(localClock.merge(serverClock))
                }
                
                syncDao.deleteEvents(outboxEvents.map { it.id })
                return@withContext true
            }
            return@withContext false
        }
    }

    override suspend fun pull(): Boolean {
        return withContext(Dispatchers.IO) {
            var hasMore = true
            var isSuccess = false
            
            while (hasMore) {
                var currentClock = clockMutex.withLock { syncPreferences.getVectorClock() }
                val result = syncNetworkDataSource.pullEvents(currentClock, 100)
                
                if (result is NetworkResult.Success) {
                    isSuccess = true
                    val response = result.data
                    val events = response.events.map { it.toDomain() }
                    
                    for (event in events) {
                        _incomingEvents.emit(event)
                        clockMutex.withLock {
                            val localClock = syncPreferences.getVectorClock()
                            val mergedClock = localClock.merge(event.vectorClock)
                            syncPreferences.saveVectorClock(mergedClock)
                        }
                    }
                    
                    hasMore = response.hasMore
                } else {
                    hasMore = false
                }
            }
            return@withContext isSuccess
        }
    }

    override suspend fun emitRealtimeEvent(entityId: String, eventType: EventType, payload: String, vectorClock: VectorClock?) {
        withContext(Dispatchers.IO) {
            val resolvedClock = vectorClock ?: VectorClock()
            val event = SyncEvent(
                id = UUID.randomUUID().toString(),
                entityId = entityId,
                eventType = eventType,
                payload = payload,
                vectorClock = resolvedClock,
                timestamp = java.time.Instant.now().toString()
            )
            _incomingEvents.emit(event)

            if (vectorClock != null) {
                clockMutex.withLock {
                    val localClock = syncPreferences.getVectorClock()
                    syncPreferences.saveVectorClock(localClock.merge(vectorClock))
                }
            }
        }
    }
}
