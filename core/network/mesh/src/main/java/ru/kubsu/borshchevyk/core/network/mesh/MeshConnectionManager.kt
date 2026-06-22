package ru.kubsu.borshchevyk.core.network.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class MeshPeer(
    val endpointId: String,
    val name: String
)

@Singleton
class MeshConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val payloadRouter: MeshPayloadRouter,
    private val signatureService: MeshSignatureService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    
    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints: StateFlow<Set<String>> = _connectedEndpoints.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<MeshPeer>>(emptyList())
    val connectedPeers: StateFlow<List<MeshPeer>> = _connectedPeers.asStateFlow()
    
    private val endpointNames = ConcurrentHashMap<String, String>()
    
    private var currentLocalEndpointName: String = "Unknown User"
    private var currentLocalUserId: String = "unknown"

    private val strategy = Strategy.P2P_CLUSTER
    private val serviceId = "ru.kubsu.borshchevyk.mesh"
    private val TAG = "MeshConnectionManager"

    private fun updatePeers() {
        val peers = _connectedEndpoints.value.map { id ->
            val fullName = endpointNames[id] ?: "Unknown"
            val displayname = fullName.substringBefore("|")
            MeshPeer(id, displayname)
        }
        _connectedPeers.value = peers
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with endpoint: $endpointId (${connectionInfo.endpointName})")
            endpointNames[endpointId] = connectionInfo.endpointName
            // Automatically accept connections for the cluster
            connectionsClient.acceptConnection(endpointId, payloadRouter.payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Successfully connected to endpoint: $endpointId")
                    _connectedEndpoints.value = _connectedEndpoints.value + endpointId
                    updatePeers()
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(TAG, "Connection rejected by endpoint: $endpointId")
                    endpointNames.remove(endpointId)
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Error connecting to endpoint: $endpointId")
                    endpointNames.remove(endpointId)
                }
                else -> {
                    Log.w(TAG, "Unknown connection status ${result.status.statusCode} for endpoint: $endpointId")
                    endpointNames.remove(endpointId)
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from endpoint: $endpointId")
            _connectedEndpoints.value = _connectedEndpoints.value - endpointId
            endpointNames.remove(endpointId)
            updatePeers()
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val parts = info.endpointName.split("|")
            val remoteUserId = if (parts.size > 1) parts[1] else null
            
            if (remoteUserId != null && remoteUserId == currentLocalUserId) {
                Log.d(TAG, "Ignoring self-discovery: ${info.endpointName}")
                return
            }
            
            Log.d(TAG, "Endpoint found: $endpointId (${info.endpointName})")
            
            // Prevent STATUS_ENDPOINT_IO_ERROR collisions in P2P_CLUSTER
            // When both devices advertise and discover simultaneously, they will both find 
            // each other and try to request a connection at the same time, causing an IO error.
            // We resolve this by only having the device with the lexicographically smaller name request.
            if (currentLocalEndpointName.compareTo(info.endpointName) < 0) {
                Log.d(TAG, "My name '$currentLocalEndpointName' is smaller than '${info.endpointName}'. Requesting connection.")
                connectionsClient.requestConnection(
                    currentLocalEndpointName,
                    endpointId,
                    connectionLifecycleCallback
                ).addOnFailureListener { e ->
                    Log.e(TAG, "Failed to request connection to $endpointId", e)
                }
            } else if (currentLocalEndpointName == info.endpointName) {
                Log.w(TAG, "Warning: Both devices have the exact same tag '$currentLocalEndpointName'. Connection might fail or stall.")
                // Attempt to connect anyway to avoid complete stall
                connectionsClient.requestConnection(currentLocalEndpointName, endpointId, connectionLifecycleCallback)
            } else {
                Log.d(TAG, "My name '$currentLocalEndpointName' is greater than '${info.endpointName}'. Waiting for them to request connection.")
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
        }
    }

    fun startAdvertising(localEndpointName: String) {
        scope.launch {
            currentLocalUserId = signatureService.getUserId() ?: "unknown"
            val actualEndpointName = "$localEndpointName|$currentLocalUserId"
            val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
            connectionsClient.startAdvertising(
                actualEndpointName,
                serviceId,
                connectionLifecycleCallback,
                options
            ).addOnSuccessListener {
                Log.d(TAG, "Started advertising as $actualEndpointName")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start advertising", e)
            }
        }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        Log.d(TAG, "Stopped advertising")
    }

    fun startDiscovery(localEndpointName: String) {
        scope.launch {
            currentLocalUserId = signatureService.getUserId() ?: "unknown"
            currentLocalEndpointName = "$localEndpointName|$currentLocalUserId"
            val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
            connectionsClient.startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                options
            ).addOnSuccessListener {
                Log.d(TAG, "Started discovery")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start discovery", e)
            }
        }
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        Log.d(TAG, "Stopped discovery")
    }
    
    fun disconnect(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        _connectedEndpoints.value = _connectedEndpoints.value - endpointId
        endpointNames.remove(endpointId)
        updatePeers()
        Log.d(TAG, "Disconnected manually from endpoint: $endpointId")
    }
    
    fun stopAllEndpoints() {
        connectionsClient.stopAllEndpoints()
        _connectedEndpoints.value = emptySet()
        endpointNames.clear()
        updatePeers()
        Log.d(TAG, "Stopped all endpoints")
    }
}
