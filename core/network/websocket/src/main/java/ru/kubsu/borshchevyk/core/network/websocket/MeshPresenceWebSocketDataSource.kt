package ru.kubsu.borshchevyk.core.network.websocket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.network.di.ApplicationScope
import ru.kubsu.borshchevyk.core.network.dto.PresencePingDto
import ru.kubsu.borshchevyk.core.network.dto.PresenceStatusResponse
import ru.kubsu.borshchevyk.core.network.mesh.MeshConnectionManager
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope
import ru.kubsu.borshchevyk.core.network.mesh.MeshFloodingProtocol
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshPresenceWebSocketDataSource @Inject constructor(
    private val meshConnectionManager: MeshConnectionManager,
    private val gossipProtocol: MeshFloodingProtocol,
    private val signatureService: MeshSignatureService,
    private val json: Json,
    @ApplicationScope private val scope: CoroutineScope
) : PresenceWebSocketDataSource {

    private val TAG = "MeshPresence"
    
    // Map of endpointId to their last seen timestamp in milliseconds
    private val lastSeenMap = ConcurrentHashMap<String, Long>()
    
    // Flow emitting the map to allow observers to react to changes
    private val _presenceMapFlow = MutableStateFlow<Map<String, Long>>(emptyMap())
    
    private val TTL_MILLIS = 3 * 60 * 1000L // 3 minutes
    private val PING_INTERVAL_MILLIS = 2 * 60 * 1000L // 2 minutes

    init {
        // 1. Passive observation: update lastSeen on ANY incoming envelope
        gossipProtocol.incomingEnvelopes
            .onEach { envelope ->
                if (envelope.originEndpointId.isNotEmpty()) {
                    updateLastSeen(envelope.originEndpointId)
                }
                
                // If it's a direct PING, we don't need to do anything else,
                // the originId update above is enough.
                if (envelope.action == "NODE_LOST") {
                     val lostNodeId = envelope.payload
                     markNodeOffline(lostNodeId)
                }
            }
            .launchIn(scope)

        // 2. Direct connection drops: immediately mark direct neighbors as offline
        // and broadcast NODE_LOST so others know.
        var previousSet = emptySet<String>()
        meshConnectionManager.connectedEndpoints
            .onEach { currentSet ->
                val disconnected = previousSet - currentSet
                for (lostNodeId in disconnected) {
                    markNodeOffline(lostNodeId)
                    
                    val envelope = MeshEnvelope(
                        envelopeId = UUID.randomUUID().toString(),
                        originEndpointId = "", // originEndpointId will be filled by broadcast
                        action = "NODE_LOST",
                        payload = lostNodeId
                    )
                    gossipProtocol.broadcast(envelope)
                    Log.d(TAG, "Direct neighbor lost: $lostNodeId. Broadcasted NODE_LOST.")
                }
                previousSet = currentSet
            }
            .launchIn(scope)
            
        // 3. TTL Pruning and Smart Pinging
        scope.launch {
            var lastPingTime = System.currentTimeMillis()
            while (true) {
                delay(10_000) // Check every 10 seconds
                val now = System.currentTimeMillis()
                
                // Prune dead nodes
                var mapChanged = false
                val iterator = lastSeenMap.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (now - entry.value > TTL_MILLIS) {
                        iterator.remove()
                        mapChanged = true
                        Log.d(TAG, "Node ${entry.key} TTL expired, marked offline")
                    }
                }
                if (mapChanged) {
                    _presenceMapFlow.value = lastSeenMap.toMap()
                }
                
                // Send periodic ping if needed
                if (now - lastPingTime > PING_INTERVAL_MILLIS) {
                    notifyActive()
                    lastPingTime = now
                }
            }
        }
    }

    private fun updateLastSeen(userId: String) {
        val now = System.currentTimeMillis()
        lastSeenMap[userId] = now
        _presenceMapFlow.value = lastSeenMap.toMap()
    }

    private fun markNodeOffline(userId: String) {
        lastSeenMap.remove(userId)
        _presenceMapFlow.value = lastSeenMap.toMap()
    }

    // Called by the app when user comes to foreground or is active
    fun notifyActive() {
        scope.launch {
            val myId = signatureService.getUserId() ?: return@launch
            val ping = PresencePingDto(myId, System.currentTimeMillis())
            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = myId,
                action = "PRESENCE_PING",
                payload = json.encodeToString(ping)
            )
            gossipProtocol.broadcast(envelope)
            Log.d(TAG, "Sent smart PRESENCE_PING")
        }
    }

    override fun observePresence(userId: String): Flow<PresenceStatusResponse> {
        return _presenceMapFlow.map { map ->
            val lastSeen = map[userId]
            val isOnline = lastSeen != null && (System.currentTimeMillis() - lastSeen <= TTL_MILLIS)
            PresenceStatusResponse(
                userId = userId,
                isOnline = isOnline,
                lastSeenAt = lastSeen
            )
        }
    }

    override suspend fun connect() {
        // Handled by MeshConnectionManager
    }

    override suspend fun disconnect() {
        // Handled by MeshConnectionManager
    }
}
