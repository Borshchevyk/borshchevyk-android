package ru.kubsu.borshchevyk.core.network.mesh

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshMediaTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val payloadRouter: MeshPayloadRouter
) {
    private val TAG = "MeshMediaTransferManager"

    // Maps payload ID to attachment metadata (e.g. filename, mimeType)
    private val payloadMetadataMap = mutableMapOf<Long, MediaMetadata>()

    // Emits when a file transfer is complete
    private val _incomingFiles = MutableSharedFlow<ReceivedFile>(extraBufferCapacity = 64)
    val incomingFiles: SharedFlow<ReceivedFile> = _incomingFiles.asSharedFlow()

    data class MediaMetadata(val attachmentId: String, val filename: String, val mimeType: String)
    data class ReceivedFile(val endpointId: String, val file: File, val metadata: MediaMetadata?)

    init {
        // Here we could intercept incoming payloads or let PayloadRouter pass them if we update it.
        // For simplicity, we can let PayloadRouter handle the basic callback and we intercept updates if needed.
    }

    fun sendFile(endpointIds: List<String>, file: File, metadata: MediaMetadata): Long {
        val payload = Payload.fromFile(file)
        val payloadId = payload.id
        payloadMetadataMap[payloadId] = metadata
        
        // Broadcast metadata over gossip first so peers know what this file is?
        // (In a real implementation, you'd send a JSON control message first, then the file payload)

        payloadRouter.sendPayload(endpointIds, payload)
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
            _incomingFiles.tryEmit(ReceivedFile(endpointId, payloadFile, metadata))
        }
    }
}
