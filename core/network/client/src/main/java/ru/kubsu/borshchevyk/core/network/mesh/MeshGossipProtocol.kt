package ru.kubsu.borshchevyk.core.network.mesh

import android.util.Log
import com.google.android.gms.nearby.connection.Payload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshGossipProtocol @Inject constructor(
    private val payloadRouter: MeshPayloadRouter,
    private val connectionManager: MeshConnectionManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "MeshGossipProtocol"

    private val MAX_CACHE_SIZE = 1000
    private val seenEnvelopes = Collections.newSetFromMap(
        object : java.util.LinkedHashMap<String, Boolean>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }.let { Collections.synchronizedMap(it) }
    )

    private val _incomingEnvelopes = MutableSharedFlow<MeshEnvelope>(extraBufferCapacity = 64)
    val incomingEnvelopes: SharedFlow<MeshEnvelope> = _incomingEnvelopes.asSharedFlow()

    init {
        payloadRouter.incomingPayloads
            .onEach { received ->
                handleIncomingPayload(received)
            }
            .launchIn(scope)
    }

    private fun handleIncomingPayload(received: MeshPayloadRouter.ReceivedPayload) {
        if (received.payload.type == Payload.Type.BYTES) {
            val bytes = received.payload.asBytes() ?: return
            try {
                val envelope = MeshEnvelope.fromByteArray(bytes)
                processEnvelope(envelope, received.endpointId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to deserialize MeshEnvelope from \${received.endpointId}", e)
            }
        }
    }

    private fun processEnvelope(envelope: MeshEnvelope, senderEndpointId: String?) {
        val isNew = seenEnvelopes.add(envelope.envelopeId)
        
        if (!isNew) {
            Log.d(TAG, "Dropped duplicate envelope: \${envelope.envelopeId}")
            return
        }
        
        Log.d(TAG, "Received new envelope: \${envelope.envelopeId}")
        
        val emitted = _incomingEnvelopes.tryEmit(envelope)
        if (!emitted) {
            Log.w(TAG, "Failed to emit envelope \${envelope.envelopeId} to shared flow")
        }

        flood(envelope, senderEndpointId)
    }

    private fun flood(envelope: MeshEnvelope, senderEndpointId: String?) {
        val connected = connectionManager.connectedEndpoints.value
        val targets = connected.filter { it != senderEndpointId }
        
        if (targets.isNotEmpty()) {
            val payload = Payload.fromBytes(envelope.toByteArray())
            payloadRouter.sendPayload(targets, payload)
            Log.d(TAG, "Flooded envelope \${envelope.envelopeId} to \${targets.size} endpoints")
        }
    }

    fun broadcast(envelope: MeshEnvelope) {
        scope.launch {
            processEnvelope(envelope, senderEndpointId = null)
        }
    }
}