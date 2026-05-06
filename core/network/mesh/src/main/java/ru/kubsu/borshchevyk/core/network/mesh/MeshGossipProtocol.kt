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
class MeshGossipProtocol @Inject constructor(
    private val payloadRouter: MeshPayloadRouter,
    private val connectionManager: MeshConnectionManager,
    private val signatureService: MeshSignatureService,
    @ApplicationScope private val scope: CoroutineScope
) {
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
        
        // --- SECURITY: Verify Signature ---
        val originId = envelope.originEndpointId
        val signature = envelope.signature
        
        if (originId.isNotEmpty() && signature != null) {
            val dataToVerify = envelope.payload.toByteArray(Charsets.UTF_8)
            val isValid = signatureService.verifySignature(originId, signature, dataToVerify)
            if (!isValid) {
                Log.e(TAG, "SECURITY ALERT: Invalid signature for envelope ${envelope.envelopeId} from claimed origin $originId. Discarding.")
                return
            }
            Log.d(TAG, "Signature verified successfully for envelope ${envelope.envelopeId}")
        } else if (originId.isNotEmpty() && signature == null) {
             Log.w(TAG, "SECURITY WARNING: Envelope ${envelope.envelopeId} from $originId is missing a signature. Discarding.")
             return
        }
        // ----------------------------------
        
        Log.d(TAG, "Received new verified envelope: ${envelope.envelopeId}")
        
        val emitted = _incomingEnvelopes.tryEmit(envelope)
        if (!emitted) {
            Log.w(TAG, "Failed to emit envelope ${envelope.envelopeId} to shared flow")
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
            val userId = signatureService.getUserId() ?: "self"
            val envelopeWithOrigin = envelope.copy(originEndpointId = userId)
            val dataToSign = envelopeWithOrigin.payload.toByteArray(Charsets.UTF_8)
            val signature = signatureService.signData(dataToSign)
            val signedEnvelope = envelopeWithOrigin.copy(signature = signature)
            processEnvelope(signedEnvelope, senderEndpointId = null)
        }
    }
}