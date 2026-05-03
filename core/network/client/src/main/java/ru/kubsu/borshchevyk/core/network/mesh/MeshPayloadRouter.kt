package ru.kubsu.borshchevyk.core.network.mesh

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshPayloadRouter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectionsClient = Nearby.getConnectionsClient(context)

    data class ReceivedPayload(val endpointId: String, val payload: Payload)

    private val _incomingPayloads = MutableSharedFlow<ReceivedPayload>(extraBufferCapacity = 64)
    val incomingPayloads: SharedFlow<ReceivedPayload> = _incomingPayloads.asSharedFlow()

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            _incomingPayloads.tryEmit(ReceivedPayload(endpointId, payload))
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Handle transfer progress (useful for STREAM or FILE payloads)
        }
    }

    fun sendPayload(endpointIds: List<String>, payload: Payload) {
        connectionsClient.sendPayload(endpointIds, payload)
    }
}
