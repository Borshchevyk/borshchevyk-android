package ru.kubsu.borshchevyk.core.network.mesh

import android.util.Log
import com.google.android.gms.nearby.connection.Payload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.network.di.ApplicationScope
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshFloodingProtocol @Inject constructor(
    private val payloadRouter: MeshPayloadRouter,
    private val connectionManager: MeshConnectionManager,
    private val signatureService: MeshSignatureService,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val TAG = "MeshFloodingProtocol"

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

    private suspend fun handleIncomingPayload(received: MeshPayloadRouter.ReceivedPayload) {
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

    private suspend fun processEnvelope(envelope: MeshEnvelope, senderEndpointId: String?) {
        val isNew = seenEnvelopes.add(envelope.envelopeId)
        
        if (!isNew) {
            Log.d(TAG, "Dropped duplicate envelope: ${envelope.envelopeId}")
            return
        }

        if (envelope.ttl <= 0) {
            Log.d(TAG, "Dropped envelope due to TTL exhaustion: ${envelope.envelopeId}")
            return
        }
        
        // --- SECURITY: Verify Signature ---
        val originId = envelope.originEndpointId
        val signature = envelope.signature
        
        if (originId.isEmpty()) {
            Log.e(TAG, "SECURITY ALERT: Envelope ${envelope.envelopeId} has no origin ID. Discarding.")
            return
        }

        if (signature == null) {
            Log.e(TAG, "SECURITY WARNING: Envelope ${envelope.envelopeId} from $originId is missing a signature. Discarding.")
            return
        }
        
        val dataToVerify = envelope.payload.toByteArray(Charsets.UTF_8)
        val isValid = signatureService.verifySignature(originId, signature, dataToVerify)
        
        if (!isValid) {
            if (!signatureService.hasPublicKey(originId)) {
                // If it's a USER_PROFILE, it shouldn't be processed here, but passed through to MeshProfileListener which will verify it AFTER extracting the key
                if (envelope.action == "USER_PROFILE") {
                    Log.w(TAG, "Key missing for USER_PROFILE. Allowing through to establish identity. Listener MUST verify signature before trusting the key.")
                } else {
                    Log.w(TAG, "Public key missing for origin $originId. Allowing envelope ${envelope.envelopeId} through so it can be buffered until key arrives.")
                }
            } else {
                Log.e(TAG, "SECURITY ALERT: Invalid signature for envelope ${envelope.envelopeId} from claimed origin $originId. Discarding.")
                return
            }
        } else {
            Log.d(TAG, "Signature verified successfully for envelope ${envelope.envelopeId}")
        }
        // ----------------------------------
        
        Log.d(TAG, "Verified envelope ${envelope.envelopeId}. Emitting to SharedFlow...")
        
        val emitted = _incomingEnvelopes.tryEmit(envelope)
        Log.d(TAG, "Emission result for ${envelope.action}: $emitted")
        if (!emitted) {
            Log.w(TAG, "Failed to emit envelope ${envelope.envelopeId} to shared flow")
        }

        flood(envelope, senderEndpointId)
    }

    private fun flood(envelope: MeshEnvelope, senderEndpointId: String?) {
        val nextTtl = envelope.ttl - 1
        if (nextTtl <= 0) {
            Log.d(TAG, "Not flooding envelope ${envelope.envelopeId} further due to TTL exhaustion.")
            return
        }

        val connected = connectionManager.connectedEndpoints.value
        val targets = connected.filter { it != senderEndpointId }
        
        if (targets.isNotEmpty()) {
            val decrementedEnvelope = envelope.copy(ttl = nextTtl)
            val payload = Payload.fromBytes(decrementedEnvelope.toByteArray())
            payloadRouter.sendPayload(targets, payload)
            Log.d(TAG, "Flooded envelope ${envelope.envelopeId} to ${targets.size} endpoints with TTL ${decrementedEnvelope.ttl}")
        }
    }

    fun broadcast(envelope: MeshEnvelope) {
        scope.launch {
            val userId = signatureService.getUserId() ?: "self"
            val envelopeWithOrigin = envelope.copy(originEndpointId = userId)
            val dataToSign = envelopeWithOrigin.payload.toByteArray(Charsets.UTF_8)
            val signature = signatureService.signData(dataToSign)
            val signedEnvelope = envelopeWithOrigin.copy(signature = signature)
            
            // Mark as seen so we don't accidentally process it if it echoes back
            seenEnvelopes.add(signedEnvelope.envelopeId)
            
            // Flood to the network, but DO NOT call processEnvelope locally.
            // Local processing is handled by the calling DataSources directly to the DB.
            flood(signedEnvelope, null)
        }
    }

    fun broadcastTo(envelope: MeshEnvelope, targetEndpointIds: List<String>) {
        scope.launch {
            val userId = signatureService.getUserId() ?: "self"
            val envelopeWithOrigin = envelope.copy(originEndpointId = userId)
            val dataToSign = envelopeWithOrigin.payload.toByteArray(Charsets.UTF_8)
            val signature = signatureService.signData(dataToSign)
            val signedEnvelope = envelopeWithOrigin.copy(signature = signature)
            
            // Mark as seen so we don't process it ourselves
            seenEnvelopes.add(signedEnvelope.envelopeId)
            
            val payload = Payload.fromBytes(signedEnvelope.toByteArray())
            payloadRouter.sendPayload(targetEndpointIds, payload)
        }
    }

    fun replayLocalEnvelope(envelope: MeshEnvelope) {
        scope.launch {
            Log.d(TAG, "Replaying pending envelope locally: ${envelope.envelopeId}")
            _incomingEnvelopes.emit(envelope)
        }
    }
}