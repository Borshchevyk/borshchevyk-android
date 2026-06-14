package ru.kubsu.borshchevyk.core.network.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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

    private val progressMap = MutableStateFlow<Map<String, Float>>(emptyMap())

    data class MediaMetadata(val attachmentId: String, val filename: String, val mimeType: String)
    data class ReceivedFile(val endpointId: String, val file: File, val metadata: MediaMetadata?)
    private data class PendingPayload(val endpointId: String, val payload: Payload)

    private val pendingPayloads = ConcurrentHashMap<Long, PendingPayload>()
    private val completedPayloads = ConcurrentHashMap<Long, Boolean>()

    private fun tryFinalize(payloadId: Long) {
        val metadata = payloadMetadataMap[payloadId] ?: return
        val pending = pendingPayloads[payloadId] ?: return
        if (completedPayloads[payloadId] != true) return

        val payloadFile = pending.payload.asFile()?.asJavaFile()
        if (payloadFile != null) {
            val destFile = File(context.cacheDir, "mesh_${metadata.attachmentId}")
            if (payloadFile.absolutePath != destFile.absolutePath) {
                payloadFile.copyTo(destFile, overwrite = true)
                try { payloadFile.delete() } catch (e: Exception) {}
            }
            Log.d(TAG, "Successfully finalized file transfer $payloadId (${metadata.filename})")
            localFiles[metadata.attachmentId] = destFile
            _incomingFiles.tryEmit(ReceivedFile(pending.endpointId, destFile, metadata))
        }

        // Cleanup
        pendingPayloads.remove(payloadId)
        payloadMetadataMap.remove(payloadId)
        completedPayloads.remove(payloadId)
        progressMap.update { it - metadata.attachmentId }
    }

    init {
        payloadRouter.transferProgress
            .onEach { update ->
                val payloadId = update.payloadId
                if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                    if (pendingPayloads.containsKey(payloadId)) {
                        completedPayloads[payloadId] = true
                        tryFinalize(payloadId)
                    } else {
                        // Outgoing payload finished
                        val metadata = payloadMetadataMap.remove(payloadId)
                        if (metadata != null) progressMap.update { it - metadata.attachmentId }
                    }
                } else if (update.status == PayloadTransferUpdate.Status.FAILURE) {
                    pendingPayloads.remove(payloadId)
                    completedPayloads.remove(payloadId)
                    val metadata = payloadMetadataMap.remove(payloadId)
                    if (metadata != null) progressMap.update { it - metadata.attachmentId }
                } else {
                    val metadata = payloadMetadataMap[payloadId]
                    if (metadata != null && update.totalBytes > 0) {
                        val progress = update.bytesTransferred.toFloat() / update.totalBytes.toFloat()
                        progressMap.update { it + (metadata.attachmentId to progress) }
                    }
                }
            }
            .launchIn(scope)

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
                        tryFinalize(header.payloadId)
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
     * Retrieves the locally cached file for a given attachment ID, if it exists.
     */
    fun getLocalFile(attachmentId: String): File? {
        val cached = localFiles[attachmentId]
        if (cached != null && cached.exists()) return cached
        
        val diskFile = File(context.cacheDir, "mesh_$attachmentId")
        if (diskFile.exists() && diskFile.length() > 0) {
            localFiles[attachmentId] = diskFile
            return diskFile
        }
        return null
    }

    /**
     * Returns a flow tracking the download progress of a specific attachment.
     */
    fun observeProgress(attachmentId: String): Flow<Float> {
        return progressMap.map { it[attachmentId] ?: 0f }.distinctUntilChanged()
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
            payload = json.encodeToString(header),
            ttl = 1
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
        Log.d(TAG, "Started receiving file payload ${payload.id} from $endpointId")
        pendingPayloads[payload.id] = PendingPayload(endpointId, payload)
        tryFinalize(payload.id)
    }
}
