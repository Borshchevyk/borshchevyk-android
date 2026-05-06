package ru.kubsu.borshchevyk.core.network.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.connection.Payload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.network.dto.FileHeaderDto
import java.io.File
import java.util.Collections
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshMediaTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val payloadRouter: MeshPayloadRouter,
    private val gossipProtocol: MeshGossipProtocol,
    private val connectionManager: MeshConnectionManager,
    private val json: Json,
    @ru.kubsu.borshchevyk.core.network.di.ApplicationScope private val scope: CoroutineScope
) {
    private val TAG = "MeshMediaTransferManager"

    // Maps payload ID to attachment metadata (e.g. filename, mimeType)
    private val payloadMetadataMap = java.util.concurrent.ConcurrentHashMap<Long, MediaMetadata>()

    // Tracks files that have already been flooded to prevent infinite loops
    private val seenFiles = Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    // Emits when a file transfer is complete
    private val _incomingFiles = MutableSharedFlow<ReceivedFile>(extraBufferCapacity = 64)
    val incomingFiles: SharedFlow<ReceivedFile> = _incomingFiles.asSharedFlow()

    data class MediaMetadata(val attachmentId: String, val filename: String, val mimeType: String)
    data class ReceivedFile(val endpointId: String, val file: File, val metadata: MediaMetadata?)

    init {
        // 1. Listen for incoming raw FILE payloads from Nearby Connections
        payloadRouter.incomingPayloads
            .onEach { received ->
                if (received.payload.type == Payload.Type.FILE) {
                    handleIncomingFilePayload(received.endpointId, received.payload)
                }
            }
            .launchIn(scope)

        // 2. Listen for 1-hop FILE_HEADER control messages to map payload IDs to metadata
        gossipProtocol.incomingEnvelopes
            .onEach { envelope ->
                if (envelope.action == "FILE_HEADER") {
                    try {
                        val header = json.decodeFromString<FileHeaderDto>(envelope.payload)
                        payloadMetadataMap[header.payloadId] = MediaMetadata(
                            attachmentId = header.attachmentId,
                            filename = header.filename,
                            mimeType = header.mimeType
                        )
                        Log.d(TAG, "Cached metadata for incoming payload ${header.payloadId}: ${header.filename}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse FILE_HEADER", e)
                    }
                }
            }
            .launchIn(scope)

        // 3. Listen for successfully received files and flood them to other peers
        incomingFiles
            .onEach { received ->
                val metadata = received.metadata ?: return@onEach
                val isNew = seenFiles.add(metadata.attachmentId)
                if (isNew) {
                    val connected = connectionManager.connectedEndpoints.value
                    // Forward to all endpoints EXCEPT the one we received it from
                    val targets = connected.filter { it != received.endpointId }
                    if (targets.isNotEmpty()) {
                        Log.d(TAG, "Flooding file ${metadata.attachmentId} to ${targets.size} endpoints")
                        sendFile(targets, received.file, metadata)
                    }
                }
            }
            .launchIn(scope)
    }

    fun sendFile(endpointIds: List<String>, file: File, metadata: MediaMetadata): Long {
        if (endpointIds.isEmpty()) return -1L

        val payload = Payload.fromFile(file)
        val payloadId = payload.id
        payloadMetadataMap[payloadId] = metadata
        
        // Mark as seen so we don't flood it again if it loops back
        seenFiles.add(metadata.attachmentId)
        
        // 1. Construct the control header
        val header = FileHeaderDto(
            payloadId = payloadId,
            attachmentId = metadata.attachmentId,
            filename = metadata.filename,
            mimeType = metadata.mimeType
        )
        
        val envelope = MeshEnvelope(
            envelopeId = UUID.randomUUID().toString(),
            originEndpointId = "", // Will be filled by broadcastTo
            action = "FILE_HEADER",
            payload = json.encodeToString(header)
        )
        
        // 2. Send the 1-hop header to the specific targets
        gossipProtocol.broadcastTo(envelope, endpointIds)

        // 3. Send the actual file payload
        payloadRouter.sendPayload(endpointIds, payload)
        
        Log.d(TAG, "Initiated transfer of payload $payloadId to ${endpointIds.size} endpoints")
        return payloadId
    }

    fun sendStream(endpointIds: List<String>, stream: java.io.InputStream): Long {
        val payload = Payload.fromStream(stream)
        payloadRouter.sendPayload(endpointIds, payload)
        return payload.id
    }
    
    // Call this from MeshPayloadRouter when Payload.Type.FILE is received
    fun handleIncomingFilePayload(endpointId: String, payload: Payload) {
        val payloadFile = payload.asFile()?.asJavaFile()
        if (payloadFile != null) {
            val metadata = payloadMetadataMap[payload.id]
            if (metadata == null) {
                Log.w(TAG, "Received file payload ${payload.id} but no metadata found in cache!")
            } else {
                Log.d(TAG, "Successfully received file payload ${payload.id} (${metadata.filename})")
            }
            _incomingFiles.tryEmit(ReceivedFile(endpointId, payloadFile, metadata))
            
            // Clean up the mapping
            payloadMetadataMap.remove(payload.id)
        }
    }
}
