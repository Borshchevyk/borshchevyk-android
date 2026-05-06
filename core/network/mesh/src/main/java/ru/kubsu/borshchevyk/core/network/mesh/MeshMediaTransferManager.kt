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
import ru.kubsu.borshchevyk.core.network.di.ApplicationScope
import ru.kubsu.borshchevyk.core.network.dto.FileHeaderDto
import ru.kubsu.borshchevyk.core.network.dto.FilePullRequestDto
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshMediaTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val payloadRouter: MeshPayloadRouter,
    private val gossipProtocol: MeshFloodingProtocol,
    private val connectionManager: MeshConnectionManager,
    private val json: Json,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val TAG = "MeshMediaTransferManager"

    // Maps payload ID to attachment metadata (e.g. filename, mimeType)
    private val payloadMetadataMap = ConcurrentHashMap<Long, MediaMetadata>()

    // Maps domain attachmentId to actual local File on disk
    private val localFiles = ConcurrentHashMap<String, File>()

    // Tracks files that have already been flooded to prevent infinite loops
    private val seenFiles = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

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

        // 2. Listen for 1-hop FILE_HEADER control messages and PULL requests
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
                } else if (envelope.action == "FILE_PULL_REQUEST") {
                    try {
                        val pullRequest = json.decodeFromString<FilePullRequestDto>(envelope.payload)
                        handlePullRequest(pullRequest.attachmentId, envelope.originEndpointId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse FILE_PULL_REQUEST", e)
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

    /**
     * Registers a local file to the Mesh network. 
     * It stores the file locally and broadcasts the file metadata to the network.
     * The actual bytes are NOT sent until a peer requests them via PULL.
     */
    fun shareLocalFile(attachmentId: String, file: File, contentType: String, originalFilename: String) {
        val metadata = MediaMetadata(attachmentId, originalFilename, contentType)
        localFiles[attachmentId] = file
        seenFiles.add(attachmentId)
        
        val header = FileHeaderDto(
            payloadId = -1L, // Payload ID is -1 because we are just broadcasting metadata, not a payload
            attachmentId = metadata.attachmentId,
            filename = metadata.filename,
            mimeType = metadata.mimeType
        )
        
        val envelope = MeshEnvelope(
            envelopeId = UUID.randomUUID().toString(),
            originEndpointId = "", 
            action = "FILE_HEADER", 
            payload = json.encodeToString(header)
        )
        
        gossipProtocol.broadcast(envelope)
        Log.d(TAG, "Gossip: Broadcasted metadata for local file: $attachmentId")
    }

    /**
     * Requests the raw file bytes from the Mesh network.
     * Floods a PULL request. Any node with the file will respond.
     */
    fun pullFile(attachmentId: String) {
        if (localFiles.containsKey(attachmentId)) {
            Log.d(TAG, "File $attachmentId is already local. Skipping pull.")
            return
        }

        val request = FilePullRequestDto(
            attachmentId = attachmentId,
            requesterEndpointId = "self" // Will be filled by broadcast
        )
        
        val envelope = MeshEnvelope(
            envelopeId = UUID.randomUUID().toString(),
            originEndpointId = "",
            action = "FILE_PULL_REQUEST",
            payload = json.encodeToString(request)
        )
        
        gossipProtocol.broadcast(envelope)
        Log.d(TAG, "Pull: Requested file $attachmentId from Mesh")
    }

    private fun handlePullRequest(attachmentId: String, requesterId: String) {
        val file = localFiles[attachmentId]
        if (file != null) {
            val metadata = payloadMetadataMap.values.find { it.attachmentId == attachmentId } 
                ?: MediaMetadata(attachmentId, file.name, "application/octet-stream")
            
            val connected = connectionManager.connectedEndpoints.value
            if (connected.contains(requesterId)) {
                Log.d(TAG, "Sending file payload directly to requester: $requesterId")
                sendFile(listOf(requesterId), file, metadata)
            } else {
                Log.d(TAG, "Flooding file payload to neighbors to route to $requesterId")
                val targets = connected.filter { it != requesterId }
                if (targets.isNotEmpty()) {
                    sendFile(targets, file, metadata)
                }
            }
        }
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
                // Cache it locally so we can serve it to others!
                localFiles[metadata.attachmentId] = payloadFile
            }
            _incomingFiles.tryEmit(ReceivedFile(endpointId, payloadFile, metadata))
            
            // Clean up the mapping
            payloadMetadataMap.remove(payload.id)
        }
    }
}
